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

// 次数类榜单的展示后缀（按 key），未列出的 count 榜默认「次」
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

const Segmented = ({ items, value, onChange }) => (
  <View style={styles.segmented}>
    {items.map((it) => {
      const active = it.key === value;
      return (
        <TouchableOpacity
          key={it.key}
          style={[styles.segment, active && styles.segmentActive]}
          onPress={() => onChange(it.key)}
          activeOpacity={0.8}
        >
          <Text style={[styles.segmentText, active && styles.segmentTextActive]}>{it.label}</Text>
        </TouchableOpacity>
      );
    })}
  </View>
);

const BoardChips = ({ boards, value, onChange }) => (
  <ScrollView
    horizontal
    showsHorizontalScrollIndicator={false}
    contentContainerStyle={styles.chipRow}
  >
    {boards.map((b) => {
      const active = b.key === value;
      return (
        <TouchableOpacity
          key={b.key}
          style={[styles.chip, active && styles.chipActive]}
          onPress={() => onChange(b.key)}
          activeOpacity={0.8}
        >
          <Text style={[styles.chipText, active && styles.chipTextActive]}>{b.label}</Text>
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

  // 拉取榜单列表（内置 + 配置），动态渲染
  useEffect(() => {
    leaderboardAPI.boards()
      .then((r) => {
        const list = r.data || [];
        setBoards(list);
        if (list.length && !list.some((b) => b.key === board)) {
          setBoard(list[0].key);
        }
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
    // 选了切片维度但画像缺该字段：仍请求 all，避免空白
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
  const formatScore = (score) =>
    currentBoard?.unit === 'seconds' ? formatDuration(score) : `${score} ${SUFFIX[board] || '次'}`;

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + 12, paddingBottom: insets.bottom + 40 }]}
      showsVerticalScrollIndicator={false}
    >
      <Text style={styles.pageTitle}>排行榜</Text>

      <BoardChips boards={boards} value={board} onChange={setBoard} />
      <Segmented items={PERIODS} value={period} onChange={setPeriod} />
      <Segmented items={DIMENSIONS} value={dimension} onChange={setDimension} />

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
          <Text style={styles.myRankLabel}>我的排名</Text>
          <Text style={styles.myRankValue}>#{data.myRank}</Text>
          <Text style={styles.myRankScore}>{formatScore(data.myScore || 0)}</Text>
        </View>
      )}

      {loading ? (
        <ActivityIndicator style={styles.loader} color={colors.brand500} />
      ) : (
        <View style={styles.listCard}>
          {(data?.entries || []).length === 0 ? (
            <View style={styles.emptyState}>
              <GIcon name="trophy" size={48} />
              <Text style={styles.empty}>还没有人上榜，快来争第一</Text>
            </View>
          ) : (
            data.entries.map((e, i) => (
              <View key={`${e.rank}-${e.nickname}-${i}`} style={[styles.row, e.me && styles.rowMe]}>
                <Text style={[styles.rank, e.rank <= 3 && styles.rankTop]}>{e.rank}</Text>
                <Text style={styles.nickname} numberOfLines={1}>
                  {e.nickname}{e.me ? ' (我)' : ''}
                </Text>
                <Text style={styles.score}>{formatScore(e.score)}</Text>
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
  pageTitle: { fontSize: 28, fontWeight: '800', color: colors.ink900, letterSpacing: -0.3, marginBottom: 4 },
  segmented: {
    flexDirection: 'row', backgroundColor: colors.bgSoft, borderRadius: radius.md, padding: 3, gap: 3,
  },
  segment: { flex: 1, height: 34, borderRadius: radius.sm, alignItems: 'center', justifyContent: 'center' },
  segmentActive: { backgroundColor: colors.bgElev, ...shadows.sm },
  segmentText: { fontSize: 13, color: colors.ink500, fontWeight: '600' },
  segmentTextActive: { color: colors.ink900 },
  chipRow: { gap: 8, paddingVertical: 2, paddingRight: 8 },
  chip: {
    paddingHorizontal: 14, height: 34, borderRadius: radius.pill, alignItems: 'center', justifyContent: 'center',
    backgroundColor: colors.bgSoft, borderWidth: 0.5, borderColor: colors.ink100,
  },
  chipActive: { backgroundColor: colors.brand500, borderColor: colors.brand500 },
  chipText: { fontSize: 13, color: colors.ink500, fontWeight: '600' },
  chipTextActive: { color: '#fff' },
  profileHint: {
    backgroundColor: colors.brand50, borderRadius: radius.md, padding: 12,
  },
  profileHintText: { fontSize: 13, color: colors.brand500, fontWeight: '600' },
  myRankCard: {
    flexDirection: 'row', alignItems: 'center', gap: 12, backgroundColor: colors.heroDark,
    borderRadius: radius.lg, padding: 16,
  },
  myRankLabel: { color: colors.gold300, fontSize: 13, fontWeight: '600', flex: 1 },
  myRankValue: { color: colors.gold50, fontSize: 22, fontWeight: '800' },
  myRankScore: { color: 'rgba(251,243,221,0.8)', fontSize: 13 },
  listCard: {
    backgroundColor: colors.bgElev, borderRadius: radius.lg, overflow: 'hidden',
    borderWidth: 0.5, borderColor: colors.ink100, ...shadows.sm, marginTop: 4,
  },
  row: {
    flexDirection: 'row', alignItems: 'center', gap: 12, paddingHorizontal: 16, paddingVertical: 13,
    borderBottomWidth: 0.5, borderBottomColor: colors.ink100,
  },
  rowMe: { backgroundColor: colors.brand50 },
  rank: { width: 28, fontSize: 15, fontWeight: '700', color: colors.ink400, textAlign: 'center' },
  rankTop: { color: colors.brand500 },
  nickname: { flex: 1, fontSize: 15, fontWeight: '500', color: colors.ink900 },
  score: { fontSize: 14, fontWeight: '700', color: colors.ink700 },
  emptyState: { alignItems: 'center', gap: 10, padding: 28 },
  empty: { textAlign: 'center', color: colors.ink400, fontSize: 14 },
  loader: { marginTop: 24 },
});

export default LeaderboardScreen;
