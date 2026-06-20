import React, { useCallback, useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useFocusEffect } from '@react-navigation/native';
import { useAuth } from '../../../app/providers/AuthContext';
import { readCheckinSettings, DEFAULT_CHECKIN_SETTINGS } from '../../checkin/storage';
import { syncWorkProfileFromLocal } from '../../onboarding/sync';
import { soupAPI } from '../../mood/api';
import GIcon, { Mascot } from '../../../shared/components/Icon';
import { computeScene, SCENES } from '../scene';
import BeforeWorkScene from '../scenes/BeforeWorkScene';
import AtWorkScene from '../scenes/AtWorkScene';
import AfterWorkScene from '../scenes/AfterWorkScene';
import { colors, radius, spacing, shadows } from '../../../shared/theme';

/**
 * 首页：进入时按系统时间 + 打卡工时自动定位到「上班前/上班中/下班后」场景，
 * 顶部分段可手动切换。不再以卡片列表罗列功能。
 */
const HomeScreen = ({ navigation }) => {
  const insets = useSafeAreaInsets();
  const { isAuthenticated } = useAuth();
  const [settings, setSettings] = useState(null);
  const [scene, setScene] = useState(null);
  const [manual, setManual] = useState(false); // 用户是否手动切过
  const [soup, setSoup] = useState('');

  const bootstrap = useCallback(async () => {
    const s = await readCheckinSettings();
    setSettings(s);
    if (!manual) setScene(computeScene(s));
    if (isAuthenticated) syncWorkProfileFromLocal();
  }, [manual, isAuthenticated]);

  // 每次回到首页重新按时间定位（除非用户本次手动切换过）
  useFocusEffect(useCallback(() => { bootstrap(); }, [bootstrap]));

  useEffect(() => { soupAPI.daily().then((r) => setSoup(r.data?.text || '')).catch(() => {}); }, []);

  const onSettingsChanged = (next) => {
    setSettings(next);
    if (!manual) setScene(computeScene(next));
  };

  const refreshSoup = () => soupAPI.random().then((r) => setSoup(r.data?.text || '')).catch(() => {});

  const activeSettings = settings || DEFAULT_CHECKIN_SETTINGS;
  const activeScene = scene || 'during';

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + 12, paddingBottom: insets.bottom + 100 }]}
      showsVerticalScrollIndicator={false}
    >
      <View style={styles.brandRow}>
        <View>
          <Text style={styles.brand}>goaway</Text>
          <Text style={styles.brandCn}>狗啊喂 · 把时间还给生活</Text>
        </View>
        <Mascot size={46} />
      </View>

      {soup ? (
        <TouchableOpacity style={styles.soupCard} onPress={refreshSoup} activeOpacity={0.85}>
          <View style={styles.soupHeader}>
            <View style={styles.soupLabelRow}>
              <GIcon name="soup" size={15} />
              <Text style={styles.soupLabel}>每日毒鸡汤</Text>
            </View>
            <View style={styles.soupRefresh}>
              <Icon name="refresh-outline" size={13} color={colors.gold300} />
              <Text style={styles.soupRefreshText}>换一句</Text>
            </View>
          </View>
          <Text style={styles.soupText}>{soup}</Text>
        </TouchableOpacity>
      ) : null}

      {/* 场景切换器 */}
      <View style={styles.segmented}>
        {SCENES.map((s) => {
          const active = s.key === activeScene;
          return (
            <TouchableOpacity
              key={s.key}
              style={[styles.segment, active && styles.segmentActive]}
              onPress={() => { setManual(true); setScene(s.key); }}
              activeOpacity={0.8}
            >
              <Text style={[styles.segmentText, active && styles.segmentTextActive]}>{s.label}</Text>
            </TouchableOpacity>
          );
        })}
      </View>

      {/* 当前场景内容 */}
      {activeScene === 'before' && (
        <BeforeWorkScene navigation={navigation} settings={activeSettings} onSettingsChanged={onSettingsChanged} />
      )}
      {activeScene === 'during' && (
        <AtWorkScene navigation={navigation} settings={activeSettings} />
      )}
      {activeScene === 'after' && (
        <AfterWorkScene navigation={navigation} />
      )}
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  content: { paddingHorizontal: spacing.md, gap: spacing.md },
  brandRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  brand: { fontSize: 30, fontWeight: '800', color: colors.brand500, letterSpacing: -0.5 },
  brandCn: { fontSize: 12, color: colors.ink500, marginTop: 2 },
  soupLabelRow: { flexDirection: 'row', alignItems: 'center', gap: 5 },
  soupCard: { backgroundColor: '#2A1F0E', borderRadius: radius.lg, padding: 14, gap: 6, ...shadows.sm },
  soupHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  soupLabel: { fontSize: 12, fontWeight: '700', color: colors.gold300, letterSpacing: 0.5 },
  soupRefresh: { flexDirection: 'row', alignItems: 'center', gap: 3 },
  soupRefreshText: { fontSize: 12, color: colors.gold300 },
  soupText: { fontSize: 15, fontWeight: '600', color: colors.gold50, lineHeight: 23 },
  segmented: { flexDirection: 'row', backgroundColor: colors.bgSoft, borderRadius: radius.md, padding: 3, gap: 3 },
  segment: { flex: 1, height: 38, borderRadius: radius.sm, alignItems: 'center', justifyContent: 'center' },
  segmentActive: { backgroundColor: colors.bgElev, ...shadows.sm },
  segmentText: { fontSize: 14, color: colors.ink500, fontWeight: '600' },
  segmentTextActive: { color: colors.ink900 },
});

export default HomeScreen;
