import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  Alert,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useAuth } from '../../../app/providers/AuthContext';
import { fishingAPI } from '../api';
import { formatDuration } from '../../checkin/utils';
import { colors, radius, spacing, shadows } from '../../../shared/theme';

const REPORT_INTERVAL_SECONDS = 60;

const FishingScreen = ({ navigation }) => {
  const insets = useSafeAreaInsets();
  const { isAuthenticated } = useAuth();

  const [running, setRunning] = useState(false);
  const [sessionSeconds, setSessionSeconds] = useState(0);
  const [summary, setSummary] = useState(null);

  const tickRef = useRef(null);
  const unreportedRef = useRef(0); // 尚未上报的秒数

  const loadSummary = useCallback(() => {
    if (!isAuthenticated) {
      setSummary(null);
      return;
    }
    fishingAPI.summary().then((r) => setSummary(r.data)).catch(() => {});
  }, [isAuthenticated]);

  useEffect(() => {
    loadSummary();
  }, [loadSummary]);

  const flushReport = useCallback(async () => {
    const seconds = Math.floor(unreportedRef.current);
    if (seconds <= 0 || !isAuthenticated) {
      return;
    }
    unreportedRef.current -= seconds;
    try {
      const r = await fishingAPI.report(seconds);
      setSummary(r.data);
    } catch {
      // 上报失败则把秒数退回，下次再试
      unreportedRef.current += seconds;
    }
  }, [isAuthenticated]);

  // 计时与定时上报
  useEffect(() => {
    if (!running) {
      return undefined;
    }
    tickRef.current = setInterval(() => {
      setSessionSeconds((s) => s + 1);
      unreportedRef.current += 1;
      if (unreportedRef.current >= REPORT_INTERVAL_SECONDS) {
        flushReport();
      }
    }, 1000);
    return () => clearInterval(tickRef.current);
  }, [running, flushReport]);

  // 离开页面时停表并结算
  useEffect(() => () => { flushReport(); }, [flushReport]);

  const toggle = () => {
    if (running) {
      setRunning(false);
      flushReport();
    } else {
      if (!isAuthenticated) {
        Alert.alert('提示', '游客可正常计时，登录后摸鱼时长才会计入排行榜。', [
          { text: '仅计时', onPress: () => setRunning(true) },
          { text: '去登录', onPress: () => navigation.navigate('Login') },
        ]);
        return;
      }
      setRunning(true);
    }
  };

  const todaySeconds = (summary?.todaySeconds || 0) + (isAuthenticated ? 0 : sessionSeconds);

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + 12, paddingBottom: insets.bottom + 40 }]}
      showsVerticalScrollIndicator={false}
    >
      <Text style={styles.pageTitle}>摸鱼</Text>

      <View style={styles.timerCard}>
        <Text style={styles.timerLabel}>本次摸鱼</Text>
        <Text style={styles.timerValue}>{formatDuration(sessionSeconds)}</Text>
        <TouchableOpacity
          style={[styles.timerBtn, running && styles.timerBtnStop]}
          onPress={toggle}
          activeOpacity={0.85}
        >
          <Icon name={running ? 'pause' : 'play'} size={22} color="#fff" />
          <Text style={styles.timerBtnText}>{running ? '结束摸鱼' : '开始摸鱼'}</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.statsRow}>
        <View style={styles.statCard}>
          <Text style={styles.statValue}>{formatDuration(todaySeconds)}</Text>
          <Text style={styles.statLabel}>今日</Text>
        </View>
        <View style={styles.statCard}>
          <Text style={styles.statValue}>{summary ? formatDuration(summary.thisWeekSeconds) : '—'}</Text>
          <Text style={styles.statLabel}>本周</Text>
        </View>
        <View style={styles.statCard}>
          <Text style={styles.statValue}>{summary ? formatDuration(summary.totalSeconds) : '—'}</Text>
          <Text style={styles.statLabel}>累计</Text>
        </View>
      </View>

      <TouchableOpacity style={styles.rankLink} onPress={() => navigation.navigate('HomeTabs', { screen: 'LeaderboardTab' })} activeOpacity={0.8}>
        <Icon name="trophy-outline" size={16} color={colors.brand500} />
        <Text style={styles.rankLinkText}>看看今日摸鱼排行榜</Text>
        <Icon name="chevron-forward" size={16} color={colors.ink300} />
      </TouchableOpacity>

      {!isAuthenticated && (
        <Text style={styles.hint}>当前为游客，时长不计入排行榜。登录后开始累计上榜。</Text>
      )}
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  content: { paddingHorizontal: spacing.md, gap: spacing.md },
  pageTitle: { fontSize: 28, fontWeight: '800', color: colors.ink900, letterSpacing: -0.3 },
  timerCard: {
    backgroundColor: colors.bgElev, borderRadius: radius.lg, padding: 24, gap: 12, alignItems: 'center',
    borderWidth: 0.5, borderColor: colors.ink100, ...shadows.sm,
  },
  timerLabel: { fontSize: 13, color: colors.ink500, fontWeight: '600' },
  timerValue: { fontSize: 44, fontWeight: '800', color: colors.ink900, letterSpacing: -1 },
  timerBtn: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8, height: 52,
    paddingHorizontal: 32, borderRadius: radius.pill, backgroundColor: colors.brand500, ...shadows.pop,
  },
  timerBtnStop: { backgroundColor: colors.danger },
  timerBtnText: { color: '#fff', fontSize: 16, fontWeight: '700' },
  statsRow: { flexDirection: 'row', gap: 8 },
  statCard: {
    flex: 1, backgroundColor: colors.bgElev, borderRadius: radius.lg, padding: 14, gap: 4,
    alignItems: 'center', borderWidth: 0.5, borderColor: colors.ink100, ...shadows.sm,
  },
  statValue: { fontSize: 16, fontWeight: '700', color: colors.ink900 },
  statLabel: { fontSize: 11, color: colors.ink500 },
  rankLink: {
    flexDirection: 'row', alignItems: 'center', gap: 8, paddingVertical: 14, paddingHorizontal: 16,
    backgroundColor: colors.bgElev, borderRadius: radius.lg, borderWidth: 0.5, borderColor: colors.ink100,
  },
  rankLinkText: { flex: 1, fontSize: 14.5, fontWeight: '600', color: colors.ink900 },
  hint: { fontSize: 12, color: colors.ink400, textAlign: 'center' },
});

export default FishingScreen;
