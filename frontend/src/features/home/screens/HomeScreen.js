import React, { useCallback, useEffect, useState } from 'react';
import {
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { soupAPI } from '../../mood/api';
import { colors, radius, spacing, shadows } from '../../../shared/theme';

/**
 * goaway 首页：顶部每日毒鸡汤 + 按「上班前 / 上班中 / 下班后 / 情绪解压」组织的工具入口。
 */
const SCENES = [
  {
    key: 'before-work',
    title: '上班前',
    subtitle: '打卡场景',
    icon: 'alarm-outline',
    route: 'Checkin',
    tools: ['上班倒计时', '距发薪 / 周末', '薪资实时进度'],
  },
  {
    key: 'at-work',
    title: '上班中',
    subtitle: '摸鱼场景',
    icon: 'cafe-outline',
    route: 'Fishing',
    tools: ['摸鱼计时', '今日摸鱼时长', '摸鱼排行榜'],
  },
  {
    key: 'after-work',
    title: '下班后',
    subtitle: '周报场景',
    icon: 'moon-outline',
    route: 'Weekly',
    tools: ['AI 周报生成', '碎片一键成稿', '历史周报'],
  },
  {
    key: 'anytime',
    title: '不爽就喷',
    subtitle: '情绪解压',
    icon: 'flame-outline',
    route: 'Roleplay',
    tools: ['找 AI 对线', '老板 / 同事 / 甲方', '怼赢解压'],
  },
];

const SceneCard = ({ scene, onPress }) => {
  const Wrapper = scene.route ? TouchableOpacity : View;
  return (
    <Wrapper style={styles.sceneCard} onPress={onPress} activeOpacity={0.85}>
      <View style={styles.sceneHeader}>
        <View style={styles.sceneIconWrap}>
          <Icon name={scene.icon} size={20} color={colors.brand500} />
        </View>
        <View style={styles.sceneTitleGroup}>
          <Text style={styles.sceneTitle}>{scene.title}</Text>
          <Text style={styles.sceneSubtitle}>{scene.subtitle}</Text>
        </View>
        {scene.route ? (
          <Icon name="chevron-forward" size={18} color={colors.ink300} />
        ) : (
          <View style={styles.soonBadge}>
            <Text style={styles.soonBadgeText}>即将上线</Text>
          </View>
        )}
      </View>
      <View style={styles.toolList}>
        {scene.tools.map((tool) => (
          <View key={tool} style={styles.toolChip}>
            <Text style={styles.toolChipText}>{tool}</Text>
          </View>
        ))}
      </View>
    </Wrapper>
  );
};

const HomeScreen = ({ navigation }) => {
  const insets = useSafeAreaInsets();
  const [soup, setSoup] = useState('');

  const loadDaily = useCallback(() => {
    soupAPI.daily().then((r) => setSoup(r.data?.text || '')).catch(() => {});
  }, []);

  useEffect(() => { loadDaily(); }, [loadDaily]);

  const refreshSoup = () => {
    soupAPI.random().then((r) => setSoup(r.data?.text || '')).catch(() => {});
  };

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={[
        styles.content,
        { paddingTop: insets.top + 16, paddingBottom: insets.bottom + 100 },
      ]}
      showsVerticalScrollIndicator={false}
    >
      <Text style={styles.brand}>goaway</Text>
      <Text style={styles.tagline}>把时间还给生活 · 给打工人的实用小工具</Text>

      {soup ? (
        <TouchableOpacity style={styles.soupCard} onPress={refreshSoup} activeOpacity={0.85}>
          <View style={styles.soupHeader}>
            <Text style={styles.soupLabel}>每日毒鸡汤</Text>
            <View style={styles.soupRefresh}>
              <Icon name="refresh-outline" size={13} color={colors.gold300} />
              <Text style={styles.soupRefreshText}>换一句</Text>
            </View>
          </View>
          <Text style={styles.soupText}>{soup}</Text>
        </TouchableOpacity>
      ) : null}

      {SCENES.map((scene) => (
        <SceneCard
          key={scene.key}
          scene={scene}
          onPress={scene.route ? () => navigation.navigate(scene.route) : undefined}
        />
      ))}
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.bg,
  },
  content: {
    paddingHorizontal: spacing.md,
    gap: spacing.md,
  },
  brand: {
    fontSize: 32,
    fontWeight: '800',
    color: colors.ink900,
    letterSpacing: -0.5,
  },
  tagline: {
    fontSize: 13,
    color: colors.ink500,
    marginTop: -8,
    marginBottom: 4,
  },
  soupCard: {
    backgroundColor: '#2A1F0E',
    borderRadius: radius.lg,
    padding: 16,
    gap: 8,
    ...shadows.sm,
  },
  soupHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  soupLabel: {
    fontSize: 12,
    fontWeight: '700',
    color: colors.gold300,
    letterSpacing: 0.5,
  },
  soupRefresh: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 3,
  },
  soupRefreshText: {
    fontSize: 12,
    color: colors.gold300,
  },
  soupText: {
    fontSize: 16,
    fontWeight: '600',
    color: colors.gold50,
    lineHeight: 24,
  },
  sceneCard: {
    backgroundColor: colors.bgElev,
    borderRadius: radius.lg,
    padding: 16,
    gap: 14,
    borderWidth: 0.5,
    borderColor: colors.ink100,
    ...shadows.sm,
  },
  sceneHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  sceneIconWrap: {
    width: 40,
    height: 40,
    borderRadius: 12,
    backgroundColor: colors.brand50,
    alignItems: 'center',
    justifyContent: 'center',
  },
  sceneTitleGroup: {
    flex: 1,
    gap: 2,
  },
  sceneTitle: {
    fontSize: 17,
    fontWeight: '700',
    color: colors.ink900,
  },
  sceneSubtitle: {
    fontSize: 12,
    color: colors.ink500,
  },
  soonBadge: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: radius.pill,
    backgroundColor: colors.bgSoft,
  },
  soonBadgeText: {
    fontSize: 11,
    fontWeight: '600',
    color: colors.ink400,
  },
  toolList: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  toolChip: {
    paddingHorizontal: 12,
    paddingVertical: 7,
    borderRadius: radius.pill,
    backgroundColor: colors.bgSoft,
    borderWidth: 0.5,
    borderColor: colors.ink100,
  },
  toolChipText: {
    fontSize: 12.5,
    color: colors.ink700 || colors.ink900,
    fontWeight: '500',
  },
  placeholderHint: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    paddingVertical: 16,
  },
  placeholderHintText: {
    fontSize: 12,
    color: colors.ink400,
  },
});

export default HomeScreen;
