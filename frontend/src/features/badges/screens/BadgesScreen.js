import React, { useCallback, useState } from 'react';
import { ActivityIndicator, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import GIcon from '../../../shared/components/Icon';
import { colors, radius, spacing, shadows } from '../../../shared/theme';
import { badgesAPI, formatBadgeValue } from '../api';

const KIND_LABEL = { SINGLE: '单次成就', CUMULATIVE: '累计成就' };
const KIND_ORDER = ['SINGLE', 'CUMULATIVE'];

const BadgeCard = ({ b }) => (
  <View style={[styles.card, b.earned && styles.cardEarned]}>
    <View style={[styles.iconWrap, b.earned ? styles.iconEarned : styles.iconLocked]}>
      <GIcon name={b.icon} size={30} style={b.earned ? null : styles.dim} />
    </View>
    <Text style={[styles.title, !b.earned && styles.titleLocked]} numberOfLines={1}>{b.title}</Text>
    <Text style={styles.desc} numberOfLines={2}>{b.description}</Text>
    {b.earned ? (
      <View style={styles.earnedPill}>
        <Text style={styles.earnedText}>已解锁</Text>
      </View>
    ) : (
      <View style={styles.progressWrap}>
        <View style={styles.track}>
          <View style={[styles.fill, { width: `${Math.round(b.progress * 100)}%` }]} />
        </View>
        <Text style={styles.progressText}>
          {formatBadgeValue(b.current, b.unit)} / {formatBadgeValue(b.threshold, b.unit)}
        </Text>
      </View>
    )}
  </View>
);

const BadgesScreen = () => {
  const insets = useSafeAreaInsets();
  const [badges, setBadges] = useState(null);
  const [error, setError] = useState(false);

  useFocusEffect(
    useCallback(() => {
      let alive = true;
      badgesAPI.list()
        .then((r) => { if (alive) { setBadges(r.data); setError(false); } })
        .catch(() => { if (alive) setError(true); });
      return () => { alive = false; };
    }, [])
  );

  if (badges === null && !error) {
    return <View style={styles.center}><ActivityIndicator color={colors.brand500} /></View>;
  }
  if (error) {
    return <View style={styles.center}><Text style={styles.hint}>登录后即可点亮专属荣誉徽章</Text></View>;
  }

  const earnedCount = badges.filter((b) => b.earned).length;

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + 12, paddingBottom: insets.bottom + 40 }]}
      showsVerticalScrollIndicator={false}
    >
      <Text style={styles.pageTitle}>荣誉徽章</Text>
      <View style={styles.summary}>
        <GIcon name="trophy" size={26} />
        <Text style={styles.summaryText}>已点亮 {earnedCount} / {badges.length} 枚</Text>
      </View>

      {KIND_ORDER.map((kind) => {
        const items = badges.filter((b) => b.kind === kind);
        if (items.length === 0) return null;
        return (
          <View key={kind} style={styles.section}>
            <Text style={styles.sectionTitle}>{KIND_LABEL[kind]}</Text>
            <View style={styles.grid}>
              {items.map((b) => <BadgeCard key={b.key} b={b} />)}
            </View>
          </View>
        );
      })}
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  content: { paddingHorizontal: spacing.md, paddingBottom: spacing.lg, gap: spacing.lg },
  pageTitle: { fontSize: 28, fontWeight: '800', color: colors.ink900, letterSpacing: -0.3, marginBottom: -4 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.bg, padding: 24 },
  hint: { color: colors.ink400, fontSize: 14 },
  summary: {
    flexDirection: 'row', alignItems: 'center', gap: 8, alignSelf: 'center',
    backgroundColor: colors.bgElev, paddingHorizontal: 16, paddingVertical: 10,
    borderRadius: radius.pill, ...shadows.sm,
  },
  summaryText: { fontSize: 15, fontWeight: '700', color: colors.ink900 },
  section: { gap: 10 },
  sectionTitle: { fontSize: 13, fontWeight: '700', color: colors.ink500, letterSpacing: 1, paddingHorizontal: 2 },
  grid: { flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between', gap: 12 },
  card: {
    width: '47%', flexGrow: 1, backgroundColor: colors.bgElev, borderRadius: radius.lg,
    padding: 16, gap: 6, alignItems: 'center', borderWidth: 0.5, borderColor: colors.ink100, ...shadows.sm,
  },
  cardEarned: { borderColor: colors.brand200, backgroundColor: colors.bgElev },
  iconWrap: { width: 58, height: 58, borderRadius: 20, alignItems: 'center', justifyContent: 'center', marginBottom: 2 },
  iconEarned: { backgroundColor: colors.sakuraSoft },
  iconLocked: { backgroundColor: colors.bgSoft },
  dim: { opacity: 0.35 },
  title: { fontSize: 15, fontWeight: '700', color: colors.ink900, textAlign: 'center' },
  titleLocked: { color: colors.ink400 },
  desc: { fontSize: 11.5, color: colors.ink500, textAlign: 'center', lineHeight: 16, minHeight: 32 },
  earnedPill: { backgroundColor: colors.mintSoft, paddingHorizontal: 12, paddingVertical: 4, borderRadius: radius.pill, marginTop: 2 },
  earnedText: { fontSize: 11.5, fontWeight: '700', color: colors.mint },
  progressWrap: { width: '100%', gap: 4, marginTop: 2 },
  track: { height: 6, borderRadius: 3, backgroundColor: colors.ink100, overflow: 'hidden' },
  fill: { height: 6, borderRadius: 3, backgroundColor: colors.brand500 },
  progressText: { fontSize: 10.5, color: colors.ink400, textAlign: 'center' },
});

export default BadgesScreen;
