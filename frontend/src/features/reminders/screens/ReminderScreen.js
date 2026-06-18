import React, { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Linking,
  ScrollView,
  StyleSheet,
  Switch,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { readReminderSettings, writeReminderSettings } from '../storage';
import { applyReminders } from '../scheduler';
import { localNotification } from '../native/localNotification';
import { colors, radius, spacing, shadows } from '../../../shared/theme';

const ReminderScreen = () => {
  const insets = useSafeAreaInsets();
  const [settings, setSettings] = useState(null);
  const [permission, setPermission] = useState('notDetermined');

  useEffect(() => {
    readReminderSettings().then(setSettings);
    if (localNotification.isAvailable()) {
      localNotification.getPermissionStatus().then((r) => setPermission(r?.status || 'notDetermined')).catch(() => {});
    }
  }, []);

  const persist = useCallback(async (next) => {
    setSettings(next);
    await writeReminderSettings(next);
    await applyReminders(next);
  }, []);

  const ensurePermission = useCallback(async () => {
    if (!localNotification.isAvailable()) {
      Alert.alert('暂不支持', '当前环境无法发送本地通知');
      return false;
    }
    const cur = await localNotification.getPermissionStatus().catch(() => ({ status: 'notDetermined' }));
    if (cur?.status === 'authorized' || cur?.status === 'provisional') {
      setPermission(cur.status);
      return true;
    }
    if (cur?.status === 'denied') {
      Alert.alert('通知权限已关闭', '请在系统设置中允许通知，才能收到提醒。', [
        { text: '取消', style: 'cancel' },
        { text: '去设置', onPress: () => Linking.openSettings() },
      ]);
      return false;
    }
    const res = await localNotification.requestPermission().catch(() => ({ granted: false }));
    setPermission(res?.granted ? 'authorized' : 'denied');
    return !!res?.granted;
  }, []);

  const toggle = useCallback((key) => async (value) => {
    if (value && !(await ensurePermission())) return;
    const next = { ...settings, [key]: { ...settings[key], enabled: value } };
    persist(next);
  }, [settings, ensurePermission, persist]);

  const setIntervalMinutes = (key, minutes) => {
    persist({ ...settings, [key]: { ...settings[key], intervalMin: minutes } });
  };

  const setField = (key, value) => {
    persist({ ...settings, [key]: value });
  };

  const setOffWorkTime = (time) => {
    persist({ ...settings, offWork: { ...settings.offWork, time } });
  };

  if (!settings) return <View style={styles.container} />;

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + 12, paddingBottom: insets.bottom + 40 }]}
      showsVerticalScrollIndicator={false}
    >
      <Text style={styles.pageTitle}>提醒</Text>
      <Text style={styles.subtitle}>本地通知，准点叫你喝水、起身、下班</Text>

      {/* 喝水 */}
      <View style={styles.card}>
        <View style={styles.rowHeader}>
          <Icon name="water-outline" size={18} color={colors.brand500} />
          <Text style={styles.cardTitle}>喝水提醒</Text>
          <Switch value={settings.water.enabled} onValueChange={toggle('water')} trackColor={{ true: colors.brand500 }} />
        </View>
        {settings.water.enabled && (
          <IntervalPicker value={settings.water.intervalMin} onChange={(m) => setIntervalMinutes('water', m)} />
        )}
      </View>

      {/* 久坐 */}
      <View style={styles.card}>
        <View style={styles.rowHeader}>
          <Icon name="walk-outline" size={18} color={colors.brand500} />
          <Text style={styles.cardTitle}>久坐提醒</Text>
          <Switch value={settings.sedentary.enabled} onValueChange={toggle('sedentary')} trackColor={{ true: colors.brand500 }} />
        </View>
        {settings.sedentary.enabled && (
          <IntervalPicker value={settings.sedentary.intervalMin} onChange={(m) => setIntervalMinutes('sedentary', m)} />
        )}
      </View>

      {/* 下班 */}
      <View style={styles.card}>
        <View style={styles.rowHeader}>
          <Icon name="exit-outline" size={18} color={colors.brand500} />
          <Text style={styles.cardTitle}>下班提醒</Text>
          <Switch value={settings.offWork.enabled} onValueChange={toggle('offWork')} trackColor={{ true: colors.brand500 }} />
        </View>
        {settings.offWork.enabled && (
          <View style={styles.fieldRow}>
            <Text style={styles.fieldLabel}>每天</Text>
            <TextInput
              style={styles.timeInput}
              value={settings.offWork.time}
              onChangeText={setOffWorkTime}
              placeholder="18:00"
              placeholderTextColor={colors.ink300}
            />
            <Text style={styles.fieldLabel}>提醒下班</Text>
          </View>
        )}
      </View>

      {/* 工作时段（喝水/久坐生效范围） */}
      <View style={styles.card}>
        <Text style={styles.cardTitle}>喝水 / 久坐 生效时段</Text>
        <View style={styles.fieldRow}>
          <TextInput style={styles.timeInput} value={settings.windowStart} onChangeText={(v) => setField('windowStart', v)} placeholder="09:00" placeholderTextColor={colors.ink300} />
          <Text style={styles.fieldLabel}>至</Text>
          <TextInput style={styles.timeInput} value={settings.windowEnd} onChangeText={(v) => setField('windowEnd', v)} placeholder="18:00" placeholderTextColor={colors.ink300} />
        </View>
      </View>

      {permission === 'denied' && (
        <Text style={styles.hint}>通知权限已关闭，前往系统设置开启后提醒才会送达</Text>
      )}
    </ScrollView>
  );
};

const INTERVALS = [30, 45, 60, 90];
const IntervalPicker = ({ value, onChange }) => (
  <View style={styles.intervalRow}>
    <Text style={styles.fieldLabel}>每隔</Text>
    {INTERVALS.map((m) => (
      <TouchableOpacity
        key={m}
        style={[styles.chip, value === m && styles.chipActive]}
        onPress={() => onChange(m)}
        activeOpacity={0.8}
      >
        <Text style={[styles.chipText, value === m && styles.chipTextActive]}>{m}分</Text>
      </TouchableOpacity>
    ))}
  </View>
);

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  content: { paddingHorizontal: spacing.md, gap: spacing.sm },
  pageTitle: { fontSize: 28, fontWeight: '800', color: colors.ink900, letterSpacing: -0.3 },
  subtitle: { fontSize: 13, color: colors.ink500, marginBottom: 8 },
  card: {
    backgroundColor: colors.bgElev, borderRadius: radius.lg, padding: 16, gap: 12,
    borderWidth: 0.5, borderColor: colors.ink100, ...shadows.sm,
  },
  rowHeader: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  cardTitle: { flex: 1, fontSize: 15.5, fontWeight: '700', color: colors.ink900 },
  intervalRow: { flexDirection: 'row', alignItems: 'center', gap: 8, flexWrap: 'wrap' },
  fieldRow: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  fieldLabel: { fontSize: 14, color: colors.ink500 },
  timeInput: {
    width: 78, height: 38, borderRadius: radius.sm, borderWidth: 1, borderColor: colors.ink200,
    textAlign: 'center', color: colors.ink900, backgroundColor: colors.bg, fontSize: 15,
  },
  chip: {
    paddingHorizontal: 14, paddingVertical: 7, borderRadius: radius.pill,
    backgroundColor: colors.bgSoft, borderWidth: 0.5, borderColor: colors.ink100,
  },
  chipActive: { backgroundColor: colors.brand500, borderColor: colors.brand500 },
  chipText: { fontSize: 13, color: colors.ink700, fontWeight: '600' },
  chipTextActive: { color: '#fff' },
  hint: { fontSize: 12, color: colors.danger, textAlign: 'center', marginTop: 8 },
});

export default ReminderScreen;
