import React, { useCallback, useState } from 'react';
import { ActivityIndicator, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import GIcon from '../../../shared/components/Icon';
import { colors, radius, spacing, shadows } from '../../../shared/theme';
import { badgesAPI } from '../api';
import { tierStyle, formatMetric, formatDate } from '../tiers';
import { useCelebration } from '../../../app/providers/BadgeCelebrationContext';
import { readCelebrated, markCelebrated, awardKey } from '../storage';

// 从 wall 推导「已解锁档位」为 award 形状（补未读 / 播种用）
const deriveEarnedAwards = (wall) => {
  const out = [];
  (wall.series || []).forEach((s) => {
    (s.tiers || []).forEach((t) => {
      if (t.earned) {
        out.push({
          seriesKey: s.seriesKey, seriesTitle: s.title, icon: s.icon,
          tier: t.tier, tierLabel: t.label, colorKey: t.colorKey,
          threshold: t.threshold, unit: s.unit, earnedAt: t.earnedAt,
          promotion: false,
        });
      }
    });
  });
  (wall.extras || []).forEach((e) => {
    if (e.earned) {
      out.push({
        seriesKey: e.key, seriesTitle: e.title, icon: e.icon, tier: '', tierLabel: '',
        colorKey: 'lav', threshold: 0, unit: e.unit, earnedAt: e.earnedAt, promotion: false,
      });
    }
  });
  return out;
};

const SeriesCard = ({ s }) => {
  const unlocked = s.currentTierOrder >= 0;
  const st = tierStyle(unlocked ? s.currentColorKey : 'gray');
  return (
    <View style={styles.card}>
      <View style={[styles.disc, { backgroundColor: st.soft, borderColor: unlocked ? st.color : colors.ink100 }]}>
        <GIcon name={s.icon} size={30} style={unlocked ? null : styles.dim} />
      </View>
      <View style={styles.info}>
        <View style={styles.titleRow}>
          <Text style={styles.title}>{s.title}</Text>
          {unlocked && <View style={[styles.tierPill, { backgroundColor: st.color }]}><Text style={styles.tierPillText}>{s.currentTierLabel}</Text></View>}
        </View>
        {unlocked
          ? <Text style={styles.sub}>当前 {formatMetric(s.current, s.unit)}{s.unit === 'count' ? '次' : ''} · 解锁于 {formatDate(s.currentEarnedAt)}</Text>
          : <Text style={styles.sub}>还差一点点就解锁啦</Text>}
        {/* 5 档轨 */}
        <View style={styles.track}>
          {(s.tiers || []).map((t) => {
            const tst = tierStyle(t.colorKey);
            return <View key={t.tier} style={[styles.dot, { backgroundColor: t.earned ? tst.color : colors.ink100 }]} />;
          })}
        </View>
        {/* 下一档进度 */}
        {s.nextTierLabel ? (
          <>
            <View style={styles.progressTrack}>
              <View style={[styles.progressFill, { width: `${Math.round(s.progressToNext * 100)}%`, backgroundColor: st.color }]} />
            </View>
            <Text style={styles.next}>
              距「{s.nextTierLabel}」 {formatMetric(s.current, s.unit)} / {formatMetric(s.nextThreshold, s.unit)}{s.unit === 'count' ? '次' : ''}
            </Text>
          </>
        ) : (
          <Text style={[styles.next, { color: colors.brand500, fontWeight: '700' }]}>已封顶 · 夯 🏆</Text>
        )}
      </View>
    </View>
  );
};

const ExtraCard = ({ b }) => {
  const st = tierStyle(b.earned ? 'lav' : 'gray');
  return (
    <View style={[styles.extra, b.earned && { borderColor: colors.brand200 }]}>
      <View style={[styles.extraIcon, { backgroundColor: st.soft }]}>
        <GIcon name={b.icon} size={26} style={b.earned ? null : styles.dim} />
      </View>
      <Text style={styles.extraTitle} numberOfLines={1}>{b.title}</Text>
      <Text style={styles.extraDesc} numberOfLines={2}>{b.description || ''}</Text>
      {b.earned
        ? <Text style={styles.extraDate}>解锁于 {formatDate(b.earnedAt)}</Text>
        : <Text style={styles.extraDate}>{Math.round((b.progress || 0) * 100)}%</Text>}
    </View>
  );
};

const BadgesScreen = () => {
  const insets = useSafeAreaInsets();
  const { celebrate } = useCelebration();
  const [wall, setWall] = useState(null);
  const [error, setError] = useState(false);

  useFocusEffect(
    useCallback(() => {
      let alive = true;
      badgesAPI.list()
        .then(async (r) => {
          if (!alive) return;
          setWall(r.data); setError(false);
          // 补未读：对比已弹集合
          const earned = deriveEarnedAwards(r.data);
          const seen = await readCelebrated();
          const unseen = earned.filter((a) => !seen.has(awardKey(a)));
          if (seen.size === 0) {
            markCelebrated(unseen.map(awardKey)); // 首次静默播种，不补弹历史
          } else if (unseen.length) {
            celebrate(unseen);
          }
        })
        .catch(() => { if (alive) setError(true); });
      return () => { alive = false; };
    }, [celebrate])
  );

  if (wall === null && !error) {
    return <View style={styles.center}><ActivityIndicator color={colors.brand500} /></View>;
  }
  if (error) {
    return <View style={styles.center}><Text style={styles.hint}>登录后即可点亮专属荣誉徽章</Text></View>;
  }

  const earnedTiers = (wall.series || []).reduce((n, s) => n + (s.currentTierOrder >= 0 ? 1 : 0), 0);

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + 12, paddingBottom: insets.bottom + 40 }]}
      showsVerticalScrollIndicator={false}
    >
      <Text style={styles.pageTitle}>荣誉徽章</Text>
      <View style={styles.summary}>
        <GIcon name="medal" size={24} />
        <Text style={styles.summaryText}>{earnedTiers} / {(wall.series || []).length} 个系列已点亮</Text>
      </View>

      {(wall.series || []).map((s) => <SeriesCard key={s.seriesKey} s={s} />)}

      {(wall.extras || []).length > 0 && (
        <>
          <Text style={styles.sectionTitle}>特殊成就</Text>
          <View style={styles.extraGrid}>
            {wall.extras.map((b) => <ExtraCard key={b.key} b={b} />)}
          </View>
        </>
      )}
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  content: { paddingHorizontal: spacing.md, paddingBottom: spacing.lg, gap: spacing.sm },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.bg, padding: 24 },
  hint: { color: colors.ink400, fontSize: 14 },
  pageTitle: { fontSize: 28, fontWeight: '800', color: colors.ink900, letterSpacing: -0.3 },
  summary: {
    flexDirection: 'row', alignItems: 'center', gap: 8, alignSelf: 'center',
    backgroundColor: colors.bgElev, paddingHorizontal: 16, paddingVertical: 9,
    borderRadius: radius.pill, marginBottom: 6, ...shadows.sm,
  },
  summaryText: { fontSize: 14, fontWeight: '700', color: colors.ink900 },
  card: {
    flexDirection: 'row', gap: 14, backgroundColor: colors.bgElev, borderRadius: radius.lg,
    padding: 16, borderWidth: 0.5, borderColor: colors.ink100, ...shadows.sm,
  },
  disc: { width: 58, height: 58, borderRadius: 29, borderWidth: 2, alignItems: 'center', justifyContent: 'center' },
  dim: { opacity: 0.35 },
  info: { flex: 1, gap: 5, justifyContent: 'center' },
  titleRow: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  title: { fontSize: 16, fontWeight: '800', color: colors.ink900 },
  tierPill: { paddingHorizontal: 9, paddingVertical: 2, borderRadius: radius.pill },
  tierPillText: { color: '#fff', fontSize: 11, fontWeight: '800' },
  sub: { fontSize: 12, color: colors.ink500 },
  track: { flexDirection: 'row', gap: 6, marginTop: 2 },
  dot: { width: 14, height: 6, borderRadius: 3 },
  progressTrack: { height: 6, borderRadius: 3, backgroundColor: colors.ink100, overflow: 'hidden', marginTop: 2 },
  progressFill: { height: 6, borderRadius: 3 },
  next: { fontSize: 11, color: colors.ink400 },
  sectionTitle: { fontSize: 13, fontWeight: '700', color: colors.ink500, letterSpacing: 1, marginTop: 12, paddingHorizontal: 2 },
  extraGrid: { flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between', gap: 12 },
  extra: {
    width: '47%', flexGrow: 1, backgroundColor: colors.bgElev, borderRadius: radius.lg, padding: 14,
    gap: 5, alignItems: 'center', borderWidth: 0.5, borderColor: colors.ink100, ...shadows.sm,
  },
  extraIcon: { width: 50, height: 50, borderRadius: 17, alignItems: 'center', justifyContent: 'center' },
  extraTitle: { fontSize: 14, fontWeight: '700', color: colors.ink900, textAlign: 'center' },
  extraDesc: { fontSize: 11, color: colors.ink500, textAlign: 'center', minHeight: 30 },
  extraDate: { fontSize: 11, color: colors.ink400 },
});

export default BadgesScreen;
