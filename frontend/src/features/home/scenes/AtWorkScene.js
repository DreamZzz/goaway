import React, { useCallback, useEffect, useRef, useState } from 'react';
import { AppState, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';
import GIcon, { Mascot } from '../../../shared/components/Icon';
import { useAuth } from '../../../app/providers/AuthContext';
import { fishingAPI } from '../../fishing/api';
import { readSideTools, bumpSideTool, addPoopSession } from '../../fishing/sideTools';
import { activityAPI } from '../../activity/api';
import { computeWorkDashboard, formatMoney, formatDuration } from '../../checkin/utils';
import { colors, radius, spacing, shadows } from '../../../shared/theme';

const pad2 = (n) => String(n).padStart(2, '0');
// 滚动秒表：分:秒.厘秒（0.01s）
const formatStopwatch = (sec) => {
  const m = Math.floor(sec / 60);
  const s = Math.floor(sec % 60);
  const cc = Math.floor((sec * 100) % 100);
  return `${m}:${pad2(s)}.${pad2(cc)}`;
};

const AtWorkScene = ({ navigation, settings }) => {
  const { isAuthenticated } = useAuth();
  const perSecond = computeWorkDashboard(settings).perSecond;

  const [elapsed, setElapsed] = useState(0);
  const [summary, setSummary] = useState(null);
  const [side, setSide] = useState({ water: 0, smoke: 0, poopCount: 0, poopSeconds: 0 });

  const startRef = useRef(Date.now());
  const reportedRef = useRef(0);
  const tickRef = useRef(null);

  // 带薪拉屎
  const [pooping, setPooping] = useState(false);
  const [poopElapsed, setPoopElapsed] = useState(0);
  const poopStartRef = useRef(0);
  const poopTickRef = useRef(null);

  const loadSummary = useCallback(() => {
    if (!isAuthenticated) { setSummary(null); return; }
    fishingAPI.summary().then((r) => setSummary(r.data)).catch(() => {});
  }, [isAuthenticated]);

  // 登录态读服务器今日汇总；游客读本地
  const loadSide = useCallback(() => {
    if (isAuthenticated) {
      activityAPI.summary().then((r) => setSide(r.data)).catch(() => {});
    } else {
      readSideTools().then(setSide);
    }
  }, [isAuthenticated]);

  useEffect(() => { loadSummary(); loadSide(); }, [loadSummary, loadSide]);

  // 上报已累计的整秒（与上次上报的差量）
  const reportElapsed = useCallback(() => {
    if (!isAuthenticated) return;
    const total = Math.floor((Date.now() - startRef.current) / 1000);
    const delta = total - reportedRef.current;
    if (delta <= 0) return;
    reportedRef.current = total;
    fishingAPI.report(delta).then((r) => setSummary(r.data)).catch(() => {
      reportedRef.current -= delta; // 失败退回
    });
  }, [isAuthenticated]);

  // 进入即自动计时；滚动显示
  useEffect(() => {
    startRef.current = Date.now();
    reportedRef.current = 0;
    tickRef.current = setInterval(() => {
      setElapsed((Date.now() - startRef.current) / 1000);
    }, 50);
    const sub = AppState.addEventListener('change', (st) => {
      if (st !== 'active') reportElapsed();
    });
    return () => {
      clearInterval(tickRef.current);
      sub.remove();
      reportElapsed(); // 离开页面结算（日累计）
      // 单次摸鱼事件级落库（≥30s 才记，供「单次最长摸鱼」榜与徽章）
      const total = Math.floor((Date.now() - startRef.current) / 1000);
      if (isAuthenticated && total >= 30) {
        activityAPI.record('FISH', total).catch(() => {});
      }
    };
  }, [reportElapsed, isAuthenticated]);

  const togglePoop = () => {
    if (pooping) {
      clearInterval(poopTickRef.current);
      const secs = Math.floor((Date.now() - poopStartRef.current) / 1000);
      setPooping(false);
      setSide((s) => ({ ...s, poopCount: (s.poopCount || 0) + 1, poopSeconds: (s.poopSeconds || 0) + secs }));
      if (isAuthenticated) {
        activityAPI.record('POOP', secs).catch(() => {});
      } else {
        addPoopSession(secs);
      }
    } else {
      poopStartRef.current = Date.now();
      setPoopElapsed(0);
      setPooping(true);
      poopTickRef.current = setInterval(() => {
        setPoopElapsed((Date.now() - poopStartRef.current) / 1000);
      }, 50);
    }
  };
  useEffect(() => () => clearInterval(poopTickRef.current), []);

  const bump = (key) => () => {
    setSide((s) => ({ ...s, [key]: (s[key] || 0) + 1 })); // 乐观更新
    if (isAuthenticated) {
      activityAPI.record(key === 'water' ? 'WATER' : 'SMOKE').catch(() => {});
    } else {
      bumpSideTool(key);
    }
  };
  const todaySeconds = (summary?.todaySeconds || 0) + (isAuthenticated ? 0 : Math.floor(elapsed));

  return (
    <View style={styles.wrap}>
      {/* 主摸鱼秒表 */}
      <View style={styles.timerCard}>
        <View style={styles.mascotWrap}><Mascot size={56} /></View>
        <View style={styles.timerPill}>
          <View style={styles.timerDot} />
          <Text style={styles.timerLabel}>本次摸鱼 · 自动计时中</Text>
        </View>
        <Text style={styles.timerValue}>{formatStopwatch(elapsed)}</Text>
        <Text style={styles.timerSub}>离开页面自动结算 · 今日 {formatDuration(todaySeconds)}</Text>
      </View>

      {/* 带薪拉屎 */}
      <TouchableOpacity style={[styles.poopCard, pooping && styles.poopCardActive]} onPress={togglePoop} activeOpacity={0.85}>
        <View style={styles.poopEmo}><GIcon name="toilet" size={28} /></View>
        <View style={styles.poopInfo}>
          <Text style={styles.poopTitle}>{pooping ? '带薪拉屎中…' : '带薪拉屎'}</Text>
          {pooping ? (
            <Text style={styles.poopMoney}>已赚 ¥{formatMoney(perSecond * poopElapsed)} · {formatStopwatch(poopElapsed)}</Text>
          ) : (
            <Text style={styles.poopDesc}>今日 {side.poopCount} 次 · 累计带薪 {formatDuration(side.poopSeconds)}</Text>
          )}
        </View>
        <View style={[styles.poopBtn, pooping && styles.poopBtnStop]}>
          <Icon name={pooping ? 'stop' : 'play'} size={16} color="#fff" />
        </View>
      </TouchableOpacity>

      {/* 喝水 / 抽烟 计数 */}
      <View style={styles.counterRow}>
        <TouchableOpacity style={styles.counterCard} onPress={bump('water')} activeOpacity={0.8}>
          <GIcon name="water" size={26} />
          <Text style={styles.counterValue}>{side.water}</Text>
          <Text style={styles.counterLabel}>喝水 +1 杯</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.counterCard} onPress={bump('smoke')} activeOpacity={0.8}>
          <GIcon name="smoke" size={26} />
          <Text style={styles.counterValue}>{side.smoke}</Text>
          <Text style={styles.counterLabel}>抽烟 +1 根</Text>
        </TouchableOpacity>
      </View>

      <TouchableOpacity style={styles.rankLink} onPress={() => navigation.navigate('HomeTabs', { screen: 'LeaderboardTab' })} activeOpacity={0.8}>
        <GIcon name="trophy" size={20} />
        <Text style={styles.rankLinkText}>看看今日摸鱼排行榜</Text>
        <Icon name="chevron-forward" size={16} color={colors.ink300} />
      </TouchableOpacity>
      {!isAuthenticated && <Text style={styles.hint}>游客时长不计入排行榜，登录后开始累计上榜</Text>}
    </View>
  );
};

const styles = StyleSheet.create({
  wrap: { gap: spacing.md },
  timerCard: { backgroundColor: colors.mintSoft, borderRadius: radius.lg, padding: 22, paddingTop: 18, gap: 6, alignItems: 'center', borderWidth: 0.5, borderColor: colors.ink100, overflow: 'hidden', ...shadows.sm },
  mascotWrap: { position: 'absolute', left: 6, top: 6 },
  timerPill: { flexDirection: 'row', alignItems: 'center', gap: 6, backgroundColor: colors.bgElev, paddingHorizontal: 12, paddingVertical: 5, borderRadius: radius.pill, ...shadows.sm },
  timerDot: { width: 7, height: 7, borderRadius: 4, backgroundColor: colors.mint },
  timerLabel: { fontSize: 12, color: colors.mint, fontWeight: '700' },
  timerValue: { fontSize: 52, fontWeight: '800', color: colors.ink900, letterSpacing: -1, fontVariant: ['tabular-nums'], marginTop: 4 },
  timerSub: { fontSize: 12, color: colors.ink400 },
  poopCard: { flexDirection: 'row', alignItems: 'center', gap: 14, padding: 16, backgroundColor: colors.bgElev, borderRadius: radius.lg, borderWidth: 0.5, borderColor: colors.ink100, ...shadows.sm },
  poopCardActive: { borderColor: colors.brand500, borderWidth: 1.5 },
  poopEmo: { width: 46, height: 46, borderRadius: 15, backgroundColor: '#fff', alignItems: 'center', justifyContent: 'center', ...shadows.sm },
  poopInfo: { flex: 1, gap: 3 },
  poopTitle: { fontSize: 16, fontWeight: '700', color: colors.ink900 },
  poopDesc: { fontSize: 12.5, color: colors.ink500 },
  poopMoney: { fontSize: 13, fontWeight: '700', color: colors.brand500, fontVariant: ['tabular-nums'] },
  poopBtn: { width: 36, height: 36, borderRadius: 18, backgroundColor: colors.brand500, alignItems: 'center', justifyContent: 'center' },
  poopBtnStop: { backgroundColor: colors.danger },
  counterRow: { flexDirection: 'row', gap: spacing.sm },
  counterCard: { flex: 1, backgroundColor: colors.bgElev, borderRadius: radius.lg, padding: 16, gap: 4, alignItems: 'center', borderWidth: 0.5, borderColor: colors.ink100, ...shadows.sm },
  counterEmoji: { fontSize: 26 },
  counterValue: { fontSize: 24, fontWeight: '800', color: colors.ink900 },
  counterLabel: { fontSize: 12, color: colors.ink500 },
  rankLink: { flexDirection: 'row', alignItems: 'center', gap: 8, paddingVertical: 14, paddingHorizontal: 16, backgroundColor: colors.bgElev, borderRadius: radius.lg, borderWidth: 0.5, borderColor: colors.ink100 },
  rankLinkText: { flex: 1, fontSize: 14.5, fontWeight: '600', color: colors.ink900 },
  hint: { fontSize: 12, color: colors.ink400, textAlign: 'center' },
});

export default AtWorkScene;
