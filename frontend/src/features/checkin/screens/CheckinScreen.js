import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  Alert,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useAuth } from '../../../app/providers/AuthContext';
import { checkinAPI } from '../api';
import { readCheckinSettings, writeCheckinSettings } from '../storage';
import {
  computeWorkDashboard,
  daysUntilPayday,
  daysUntilWeekend,
  formatMoney,
} from '../utils';
import { colors, radius, spacing, shadows } from '../../../shared/theme';

const CheckinScreen = ({ navigation }) => {
  const insets = useSafeAreaInsets();
  const { isAuthenticated } = useAuth();

  const [settings, setSettings] = useState(null);
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(null);
  const [dashboard, setDashboard] = useState(null);
  const [summary, setSummary] = useState(null);
  const [checkingIn, setCheckingIn] = useState(false);
  const tickRef = useRef(null);

  useEffect(() => {
    readCheckinSettings().then(setSettings);
  }, []);

  // 每秒刷新薪资进度与倒计时
  useEffect(() => {
    if (!settings) return undefined;
    const update = () => setDashboard(computeWorkDashboard(settings));
    update();
    tickRef.current = setInterval(update, 1000);
    return () => clearInterval(tickRef.current);
  }, [settings]);

  const loadSummary = useCallback(() => {
    if (!isAuthenticated) {
      setSummary(null);
      return;
    }
    checkinAPI.summary().then((r) => setSummary(r.data)).catch(() => {});
  }, [isAuthenticated]);

  useEffect(() => {
    loadSummary();
  }, [loadSummary]);

  const handleCheckin = async () => {
    if (!isAuthenticated) {
      Alert.alert('登录后打卡', '登录即可记录连续打卡天数并参与排行榜。', [
        { text: '取消', style: 'cancel' },
        { text: '去登录', onPress: () => navigation.navigate('Login') },
      ]);
      return;
    }
    try {
      setCheckingIn(true);
      const r = await checkinAPI.checkin();
      setSummary(r.data);
    } catch {
      Alert.alert('打卡失败', '请稍后再试');
    } finally {
      setCheckingIn(false);
    }
  };

  const startEdit = () => {
    setDraft({ ...settings });
    setEditing(true);
  };

  const saveEdit = async () => {
    const next = {
      ...draft,
      monthlySalary: Number(draft.monthlySalary) || 0,
      workDaysPerMonth: Number(draft.workDaysPerMonth) || 22,
      paydayDay: Number(draft.paydayDay) || 15,
    };
    setSettings(next);
    setEditing(false);
    await writeCheckinSettings(next);
  };

  if (!settings || !dashboard) {
    return <View style={styles.container} />;
  }

  const payday = daysUntilPayday(settings);
  const weekend = daysUntilWeekend();

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + 12, paddingBottom: insets.bottom + 40 }]}
      showsVerticalScrollIndicator={false}
    >
      <View style={styles.headerRow}>
        <Text style={styles.pageTitle}>打卡</Text>
        <TouchableOpacity style={styles.gearBtn} onPress={startEdit} activeOpacity={0.75}>
          <Icon name="options-outline" size={20} color={colors.ink500} />
        </TouchableOpacity>
      </View>

      {/* 薪资进度卡 */}
      <View style={styles.heroCard}>
        <Text style={styles.heroLabel}>{dashboard.phaseLabel} · {dashboard.countdownLabel}</Text>
        <Text style={styles.heroEarned}>¥{formatMoney(dashboard.todayEarned)}</Text>
        <Text style={styles.heroSub}>今日已赚 · 满勤 ¥{formatMoney(dashboard.dailySalary)}</Text>
        <View style={styles.progressTrack}>
          <View style={[styles.progressFill, { width: `${Math.round(dashboard.progressPct * 100)}%` }]} />
        </View>
        <Text style={styles.heroRate}>每秒 ¥{formatMoney(dashboard.perSecond)}</Text>
      </View>

      {/* 倒计时小卡 */}
      <View style={styles.statsRow}>
        <View style={styles.statCard}>
          <Text style={styles.statValue}>{payday}</Text>
          <Text style={styles.statLabel}>天后发薪</Text>
        </View>
        <View style={styles.statCard}>
          <Text style={styles.statValue}>{weekend === 0 ? '🎉' : weekend}</Text>
          <Text style={styles.statLabel}>{weekend === 0 ? '已是周末' : '天后周末'}</Text>
        </View>
        <View style={styles.statCard}>
          <Text style={styles.statValue}>{summary ? summary.currentStreak : '—'}</Text>
          <Text style={styles.statLabel}>连续打卡</Text>
        </View>
      </View>

      {/* 打卡按钮 */}
      <TouchableOpacity
        style={[styles.checkinBtn, summary?.checkedInToday && styles.checkinBtnDone]}
        onPress={handleCheckin}
        disabled={checkingIn || summary?.checkedInToday}
        activeOpacity={0.85}
      >
        <Icon
          name={summary?.checkedInToday ? 'checkmark-circle' : 'finger-print'}
          size={20}
          color="#fff"
        />
        <Text style={styles.checkinBtnText}>
          {summary?.checkedInToday ? '今日已打卡' : '打卡'}
        </Text>
      </TouchableOpacity>
      {summary ? (
        <Text style={styles.summaryHint}>
          累计打卡 {summary.totalDays} 天 · 本周 {summary.thisWeekDays} 天
        </Text>
      ) : (
        <Text style={styles.summaryHint}>登录后可记录连续打卡并上榜</Text>
      )}

      {/* 设置编辑 */}
      {editing && (
        <View style={styles.editCard}>
          <Text style={styles.editTitle}>我的工时与薪资</Text>
          <EditRow label="上班时间" value={draft.workStart} onChange={(v) => setDraft({ ...draft, workStart: v })} placeholder="09:00" />
          <EditRow label="下班时间" value={draft.workEnd} onChange={(v) => setDraft({ ...draft, workEnd: v })} placeholder="18:00" />
          <EditRow label="月薪 (元)" value={String(draft.monthlySalary)} onChange={(v) => setDraft({ ...draft, monthlySalary: v })} keyboardType="numeric" />
          <EditRow label="每月工作天数" value={String(draft.workDaysPerMonth)} onChange={(v) => setDraft({ ...draft, workDaysPerMonth: v })} keyboardType="numeric" />
          <EditRow label="发薪日 (几号)" value={String(draft.paydayDay)} onChange={(v) => setDraft({ ...draft, paydayDay: v })} keyboardType="numeric" />
          <View style={styles.editActions}>
            <TouchableOpacity style={styles.editCancel} onPress={() => setEditing(false)}>
              <Text style={styles.editCancelText}>取消</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.editSave} onPress={saveEdit}>
              <Text style={styles.editSaveText}>保存</Text>
            </TouchableOpacity>
          </View>
        </View>
      )}
    </ScrollView>
  );
};

const EditRow = ({ label, value, onChange, placeholder, keyboardType }) => (
  <View style={styles.editRow}>
    <Text style={styles.editLabel}>{label}</Text>
    <TextInput
      style={styles.editInput}
      value={value}
      onChangeText={onChange}
      placeholder={placeholder}
      placeholderTextColor={colors.ink300}
      keyboardType={keyboardType}
    />
  </View>
);

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  content: { paddingHorizontal: spacing.md, gap: spacing.md },
  headerRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  pageTitle: { fontSize: 28, fontWeight: '800', color: colors.ink900, letterSpacing: -0.3 },
  gearBtn: {
    width: 36, height: 36, borderRadius: 18, backgroundColor: colors.bgSoft,
    alignItems: 'center', justifyContent: 'center',
  },
  heroCard: {
    backgroundColor: '#2A1F0E', borderRadius: radius.lg, padding: 20, gap: 6, ...shadows.sm,
  },
  heroLabel: { color: colors.gold300, fontSize: 13, fontWeight: '600' },
  heroEarned: { color: colors.gold50, fontSize: 40, fontWeight: '800', letterSpacing: -1 },
  heroSub: { color: 'rgba(251,243,221,0.7)', fontSize: 12 },
  progressTrack: {
    height: 8, borderRadius: 4, backgroundColor: 'rgba(255,255,255,0.15)', marginTop: 8, overflow: 'hidden',
  },
  progressFill: { height: 8, borderRadius: 4, backgroundColor: colors.gold300 },
  heroRate: { color: 'rgba(251,243,221,0.6)', fontSize: 12, marginTop: 2 },
  statsRow: { flexDirection: 'row', gap: 8 },
  statCard: {
    flex: 1, backgroundColor: colors.bgElev, borderRadius: radius.lg, padding: 14, gap: 4,
    alignItems: 'center', borderWidth: 0.5, borderColor: colors.ink100, ...shadows.sm,
  },
  statValue: { fontSize: 22, fontWeight: '700', color: colors.ink900 },
  statLabel: { fontSize: 11, color: colors.ink500 },
  checkinBtn: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8, height: 52,
    borderRadius: radius.md, backgroundColor: colors.brand500, ...shadows.pop,
  },
  checkinBtnDone: { backgroundColor: colors.ink400 },
  checkinBtnText: { color: '#fff', fontSize: 16, fontWeight: '700' },
  summaryHint: { fontSize: 12, color: colors.ink400, textAlign: 'center', marginTop: -4 },
  editCard: {
    backgroundColor: colors.bgElev, borderRadius: radius.lg, padding: 16, gap: 10,
    borderWidth: 0.5, borderColor: colors.ink100, ...shadows.sm,
  },
  editTitle: { fontSize: 15, fontWeight: '700', color: colors.ink900, marginBottom: 2 },
  editRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  editLabel: { fontSize: 14, color: colors.ink700 },
  editInput: {
    width: 120, height: 40, borderRadius: radius.sm, borderWidth: 1, borderColor: colors.ink200,
    paddingHorizontal: 12, color: colors.ink900, textAlign: 'right', backgroundColor: colors.bg,
  },
  editActions: { flexDirection: 'row', gap: 10, marginTop: 6 },
  editCancel: {
    flex: 1, height: 44, borderRadius: radius.md, borderWidth: 1, borderColor: colors.ink200,
    alignItems: 'center', justifyContent: 'center',
  },
  editCancelText: { color: colors.ink500, fontWeight: '600' },
  editSave: {
    flex: 1, height: 44, borderRadius: radius.md, backgroundColor: colors.brand500,
    alignItems: 'center', justifyContent: 'center',
  },
  editSaveText: { color: '#fff', fontWeight: '700' },
});

export default CheckinScreen;
