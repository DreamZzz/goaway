import React, { useEffect, useState, useCallback } from 'react';
import {
  Alert,
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
import apiClient from '../../../shared/api/client';
import { pushAPI } from '../api';
import { setupRemotePush } from '../index';
import { colors, radius, spacing, shadows } from '../../../shared/theme';

const FREQ_OPTIONS = [
  { key: 'LOW', label: '低频' },
  { key: 'NORMAL', label: '正常' },
  { key: 'HIGH', label: '狂轰' },
];

const PushSettingsScreen = () => {
  const insets = useSafeAreaInsets();
  const [prefs, setPrefs] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    pushAPI.getPrefs()
      .then((r) => setPrefs(r.data))
      .catch(() => setPrefs({ enabled: true, frequency: 'NORMAL', quietStart: 22, quietEnd: 8 }));
  }, []);

  const persist = useCallback(async (patch) => {
    setSaving(true);
    const next = { ...prefs, ...patch };
    setPrefs(next);
    try {
      const r = await pushAPI.updatePrefs(patch);
      setPrefs(r.data);
    } catch (e) {
      Alert.alert('保存失败', '请检查网络后重试');
    } finally {
      setSaving(false);
    }
  }, [prefs]);

  const onToggleEnabled = async (value) => {
    if (value) {
      await setupRemotePush(); // 开启时确保已申请权限并注册 token
    }
    persist({ enabled: value });
  };

  const setQuiet = (field, text) => {
    const n = parseInt(text, 10);
    if (Number.isNaN(n)) return;
    persist({ [field]: Math.max(0, Math.min(23, n)) });
  };

  const sendTestTaunt = async () => {
    try {
      const r = await apiClient.post('/taunt/test');
      const sent = r.data?.sent;
      Alert.alert(
        sent ? '已推送，注意查收通知' : '生成成功但未送达',
        (r.data?.content || '（空）') +
          (sent ? '' : '\n\n未送达：可能未注册设备或未允许通知，请确认已开启推送权限'),
      );
    } catch (e) {
      Alert.alert('失败', '请先登录并开启推送');
    }
  };

  if (!prefs) return <View style={styles.container} />;

  const enabled = prefs.enabled;

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + 12, paddingBottom: insets.bottom + 40 }]}
      showsVerticalScrollIndicator={false}
    >
      <Text style={styles.pageTitle}>毒舌推送</Text>
      <Text style={styles.subtitle}>让「最讨厌的人」定时来阴阳你，刺激你回来对线</Text>

      <View style={styles.card}>
        <View style={styles.rowHeader}>
          <Icon name="flame-outline" size={18} color={colors.brand500} />
          <Text style={styles.cardTitle}>开启毒舌推送</Text>
          <Switch value={enabled} onValueChange={onToggleEnabled} trackColor={{ true: colors.brand500 }} />
        </View>
      </View>

      {enabled && (
        <>
          <View style={styles.card}>
            <Text style={styles.cardTitle}>骚扰强度</Text>
            <View style={styles.chipRow}>
              {FREQ_OPTIONS.map((opt) => (
                <TouchableOpacity
                  key={opt.key}
                  style={[styles.chip, prefs.frequency === opt.key && styles.chipActive]}
                  onPress={() => persist({ frequency: opt.key })}
                  activeOpacity={0.8}
                >
                  <Text style={[styles.chipText, prefs.frequency === opt.key && styles.chipTextActive]}>
                    {opt.label}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>
            <Text style={styles.hintLine}>低频≈每日 1 条，正常≈每日 3 条，狂轰≈每日 6 条</Text>
          </View>

          <View style={styles.card}>
            <Text style={styles.cardTitle}>免打扰时段</Text>
            <View style={styles.fieldRow}>
              <TextInput
                style={styles.timeInput}
                value={String(prefs.quietStart ?? 22)}
                onChangeText={(t) => setQuiet('quietStart', t)}
                keyboardType="number-pad"
                maxLength={2}
              />
              <Text style={styles.fieldLabel}>点 至</Text>
              <TextInput
                style={styles.timeInput}
                value={String(prefs.quietEnd ?? 8)}
                onChangeText={(t) => setQuiet('quietEnd', t)}
                keyboardType="number-pad"
                maxLength={2}
              />
              <Text style={styles.fieldLabel}>点 不打扰</Text>
            </View>
          </View>

          <TouchableOpacity style={styles.testBtn} onPress={sendTestTaunt} activeOpacity={0.85}>
            <Icon name="send-outline" size={16} color="#fff" />
            <Text style={styles.testBtnText}>给自己来一条试试</Text>
          </TouchableOpacity>
        </>
      )}

      {saving && <Text style={styles.hintLine}>保存中…</Text>}
    </ScrollView>
  );
};

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
  chipRow: { flexDirection: 'row', alignItems: 'center', gap: 8, flexWrap: 'wrap' },
  fieldRow: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  fieldLabel: { fontSize: 14, color: colors.ink500 },
  timeInput: {
    width: 56, height: 38, borderRadius: radius.sm, borderWidth: 1, borderColor: colors.ink200,
    textAlign: 'center', color: colors.ink900, backgroundColor: colors.bg, fontSize: 15,
  },
  chip: {
    paddingHorizontal: 16, paddingVertical: 8, borderRadius: radius.pill,
    backgroundColor: colors.bgSoft, borderWidth: 0.5, borderColor: colors.ink100,
  },
  chipActive: { backgroundColor: colors.brand500, borderColor: colors.brand500 },
  chipText: { fontSize: 13.5, color: colors.ink700, fontWeight: '600' },
  chipTextActive: { color: '#fff' },
  hintLine: { fontSize: 12, color: colors.ink400 },
  testBtn: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8,
    backgroundColor: colors.brand500, borderRadius: radius.lg, paddingVertical: 13, marginTop: 4,
  },
  testBtnText: { color: '#fff', fontSize: 15, fontWeight: '700' },
});

export default PushSettingsScreen;
