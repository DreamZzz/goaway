import React, { useCallback, useEffect, useState } from 'react';
import {
  ActivityIndicator,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useFocusEffect } from '@react-navigation/native';
import { useAuth } from '../../../app/providers/AuthContext';
import { leaderboardAPI } from '../api';
import GIcon from '../../../shared/components/Icon';
import { workProfileAPI } from '../../profile/workProfileApi';
import { formatDuration } from '../../checkin/utils';
import { colors, radius, spacing, shadows } from '../../../shared/theme';

const SUFFIX = { checkin: '天', water_total: '杯', smoke_total: '根', poop_total: '次' };
const PERIODS = [
  { key: 'day', label: '今日' },
  { key: 'week', label: '本周' },
];
const DIMENSIONS = [
  { key: 'all', label: '全站' },
  { key: 'city', label: '同城', field: 'city' },
  { key: 'industry', label: '同行', field: 'industry' },
  { key: 'jobType', label: '同工种', field: 'jobType' },
];

// 主题配色 + 图标（按榜单 key），未列出走兜底
const THEMES = {
  fishing: { icon: 'fish', color: colors.mint, soft: colors.mintSoft },
  fish_total: { icon: 'fish', color: colors.mint, soft: colors.mintSoft },
  fish_single: { icon: 'fish', color: colors.mint, soft: colors.mintSoft },
  water_total: { icon: 'water', color: colors.sky, soft: colors.skySoft },
  smoke_total: { icon: 'smoke', color: colors.lav, soft: colors.lavSoft },
  poop_total: { icon: 'toilet', color: colors.peach, soft: colors.peachSoft },
  poop_single: { icon: 'toilet', color: colors.peach, soft: colors.peachSoft },
  checkin: { icon: 'medal', color: colors.gold300, soft: colors.gold50 },
};
const themeOf = (key) => THEMES[key] || { icon: 'trophy', color: colors.brand500, soft: colors.sakuraSoft };

const MEDAL = { 1: '🥇', 2: '🥈', 3: '🥉' };

const ThemeRail = ({ boards, value, onChange }) => (
  <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.rail}>
    {boards.map((b) => {
      const th = themeOf(b.key);
      const active = b.key === value;
      return (
        <TouchableOpacity
          key={b.key}
          style={[styles.themeCard, active && { backgroundColor: th.soft, borderColor: th.color }]}
          onPress={() => onChange(b.key)}
          activeOpacity={0.85}
        >
          <View style={[styles.themeIcon, active && styles.themeIconActive]}>
            <GIcon name={th.icon} size={26} style={active ? null : styles.dim} />
          </View>
          <Text style={[styles.themeLabel, active && { color: th.color, fontWeight: '800' }]} numberOfLines={1}>
            {b.label}
          </Text>
        </TouchableOpacity>
      );
    })}
  </ScrollView>
);

const LeaderboardScreen = ({ navigation }) => {
  const insets = useSafeAreaInsets();
  const { isAuthenticated } = useAuth();

  const [boards, setBoards] = useState([]);
  const [board, setBoard] = useState('fishing');
  const [period, setPeriod] = useState('day');
  const [dimension, setDimension] = useState('all');
  const [profile, setProfile] = useState(null);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    leaderboardAPI.boards()
      .then((r) => {
        const list = r.data || [];
        setBoards(list);
        if (list.length && !list.some((b) => b.key === board)) setBoard(list[0].key);
      })
      .catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (isAuthenticated) {
      workProfileAPI.get().then((r) => setProfile(r.data)).catch(() => {});
    } else {
      setProfile(null);
    }
  }, [isAuthenticated]);

  const load = useCallback(() => {
    const dim = DIMENSIONS.find((d) => d.key === dimension);
    const slice = dim?.field ? profile?.[dim.field] : undefined;
    const effectiveDimension = dim?.field && !slice ? 'all' : dimension;
    setLoading(true);
    leaderboardAPI
      .get({ board, dimension: effectiveDimension, slice, period })
      .then((r) => setData(r.data))
      .catch(() => setData(null))
      .finally(() => setLoading(false));
  }, [board, dimension, period, profile]);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  const dim = DIMENSIONS.find((d) => d.key === dimension);
  const missingSlice = dim?.field && !profile?.[dim.field];
  const currentBoard = boards.find((b) => b.key === board);
  const th = themeOf(board);
  const formatScore = (score) =>
    currentBoard?.unit === 'seconds' ? formatDuration(score) : `${score} ${SUFFIX[board] || '次'}`;

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + 12, paddingBottom: insets.bottom + 40 }]}
      showsVerticalScrollIndicator={false}
    >
      <Text style={styles.pageTitle}>排行榜</Text>

      {/* 主题（主角）*/}
      <ThemeRail boards={boards} value={board} onChange={setBoard} />

      {/* 今日 / 本周 + 范围（弱化）*/}
      <View style={styles.filterRow}>
        <View style={styles.periodSeg}>
          {PERIODS.map((p) => {
            const active = p.key === period;
            return (
              <TouchableOpacity key={p.key} style={[styles.period, active && { backgroundColor: th.color }]}
                onPress={() => setPeriod(p.key)} activeOpacity={0.8}>
                <Text style={[styles.periodText, active && styles.periodTextActive]}>{p.label}</Text>
              </TouchableOpacity>
            );
          })}
        </View>
      </View>

      <View style={styles.dimRow}>
        <Text style={styles.dimLabel}>范围</Text>
        {DIMENSIONS.map((d) => {
          const active = d.key === dimension;
          return (
            <TouchableOpacity key={d.key} style={[styles.dimPill, active && styles.dimPillActive]}
              onPress={() => setDimension(d.key)} activeOpacity={0.7}>
              <Text style={[styles.dimText, active && styles.dimTextActive]}>{d.label}</Text>
            </TouchableOpacity>
          );
        })}
      </View>

      {missingSlice && (
        <TouchableOpacity style={styles.profileHint} onPress={() => navigation.navigate('WorkProfile')} activeOpacity={0.8}>
          <Text style={styles.profileHintText}>
            {isAuthenticated ? '设置画像后即可查看分组榜单 →' : '登录并设置画像后可查看分组榜单 →'}
          </Text>
        </TouchableOpacity>
      )}

      {/* 我的排名 */}
      {data?.myRank != null && (
        <View style={styles.myRankCard}>
          <View style={[styles.myRankIcon, { backgroundColor: th.soft }]}><GIcon name={th.icon} size={24} /></View>
          <View style={{ flex: 1 }}>
            <Text style={styles.myRankLabel}>我在「{currentBoard?.label || ''}」</Text>
            <Text style={styles.myRankScore}>{formatScore(data.myScore || 0)}</Text>
          </View>
          <Text style={styles.myRankValue}>#{data.myRank}</Text>
        </View>
      )}

      {loading ? (
        <ActivityIndicator style={styles.loader} color={th.color} />
      ) : (
        <View style={styles.listCard}>
          {(data?.entries || []).length === 0 ? (
            <View style={styles.emptyState}>
              <GIcon name={th.icon} size={48} style={styles.dim} />
              <Text style={styles.empty}>这个榜还空着，快来抢头名</Text>
            </View>
          ) : (
            data.entries.map((e, i) => (
              <View key={`${e.rank}-${e.nickname}-${i}`} style={[styles.row, e.me && { backgroundColor: th.soft }]}>
                <Text style={[styles.rank, e.rank <= 3 && { color: th.color }]}>
                  {MEDAL[e.rank] || e.rank}
                </Text>
                <Text style={styles.nickname} numberOfLines={1}>
                  {e.nickname}{e.me ? ' (我)' : ''}
                </Text>
                <Text style={[styles.score, { color: th.color }]}>{formatScore(e.score)}</Text>
              </View>
            ))
          )}
        </View>
      )}
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  content: { paddingHorizontal: spacing.md, gap: spacing.sm },
  pageTitle: { fontSize: 28, fontWeight: '800', color: colors.ink900, letterSpacing: -0.3, marginBottom: 2 },
  dim: { opacity: 0.5 },
  // 主题卡
  rail: { gap: 10, paddingVertical: 4, paddingRight: 8 },
  themeCard: {
    width: 76, paddingVertical: 12, borderRadius: radius.lg, alignItems: 'center', gap: 8,
    backgroundColor: colors.bgElev, borderWidth: 1.5, borderColor: colors.ink100, ...shadows.sm,
  },
  themeIcon: {
    width: 48, height: 48, borderRadius: 24, backgroundColor: colors.bgSoft,
    alignItems: 'center', justifyContent: 'center',
  },
  themeIconActive: { backgroundColor: '#fff' },
  themeLabel: { fontSize: 11.5, color: colors.ink500, fontWeight: '600', maxWidth: 70, textAlign: 'center' },
  // 今日/本周
  filterRow: { flexDirection: 'row', marginTop: 2 },
  periodSeg: { flexDirection: 'row', backgroundColor: colors.bgSoft, borderRadius: radius.pill, padding: 3, gap: 2 },
  period: { paddingHorizontal: 18, height: 30, borderRadius: radius.pill, alignItems: 'center', justifyContent: 'center' },
  periodText: { fontSize: 13, color: colors.ink500, fontWeight: '700' },
  periodTextActive: { color: '#fff' },
  // 范围（弱化）
  dimRow: { flexDirection: 'row', alignItems: 'center', gap: 6, flexWrap: 'wrap' },
  dimLabel: { fontSize: 11, color: colors.ink300, marginRight: 2 },
  dimPill: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: radius.pill },
  dimPillActive: { backgroundColor: colors.bgSoft },
  dimText: { fontSize: 12, color: colors.ink400, fontWeight: '500' },
  dimTextActive: { color: colors.ink900, fontWeight: '700' },
  profileHint: { backgroundColor: colors.brand50, borderRadius: radius.md, padding: 10 },
  profileHintText: { fontSize: 12.5, color: colors.brand500, fontWeight: '600' },
  // 我的排名
  myRankCard: {
    flexDirection: 'row', alignItems: 'center', gap: 12, backgroundColor: colors.heroDark,
    borderRadius: radius.lg, padding: 14, marginTop: 2,
  },
  myRankIcon: { width: 40, height: 40, borderRadius: 20, alignItems: 'center', justifyContent: 'center' },
  myRankLabel: { color: 'rgba(251,243,221,0.75)', fontSize: 12, fontWeight: '600' },
  myRankScore: { color: colors.gold50, fontSize: 15, fontWeight: '700', marginTop: 1 },
  myRankValue: { color: colors.gold300, fontSize: 26, fontWeight: '900' },
  // 榜单
  listCard: {
    backgroundColor: colors.bgElev, borderRadius: radius.lg, overflow: 'hidden',
    borderWidth: 0.5, borderColor: colors.ink100, ...shadows.sm, marginTop: 2,
  },
  row: {
    flexDirection: 'row', alignItems: 'center', gap: 12, paddingHorizontal: 16, paddingVertical: 13,
    borderBottomWidth: 0.5, borderBottomColor: colors.ink100,
  },
  rank: { width: 30, fontSize: 16, fontWeight: '800', color: colors.ink400, textAlign: 'center' },
  nickname: { flex: 1, fontSize: 15, fontWeight: '500', color: colors.ink900 },
  score: { fontSize: 14, fontWeight: '800' },
  emptyState: { alignItems: 'center', gap: 10, padding: 28 },
  empty: { textAlign: 'center', color: colors.ink400, fontSize: 14 },
  loader: { marginTop: 24 },
});

export default LeaderboardScreen;
