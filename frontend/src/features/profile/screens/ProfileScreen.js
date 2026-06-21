import React from 'react';
import {
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Mascot } from '../../../shared/components/Icon';
import { useProfileViewModel } from '../viewModels/useProfileViewModel';
import { navigateToRoute } from '../../../shared/utils';
import { colors, radius, spacing, shadows } from '../../../shared/theme';

const ProfileScreen = ({ navigation }) => {
  const vm = useProfileViewModel();
  const insets = useSafeAreaInsets();

  // ── Guest view ──
  if (vm.isGuest) {
    return (
      <ScrollView
        style={styles.container}
        contentContainerStyle={[
          styles.content,
          { paddingTop: insets.top + 16, paddingBottom: insets.bottom + 100 },
        ]}
        showsVerticalScrollIndicator={false}
      >
        <Text style={styles.pageTitle}>我的</Text>

        <View style={styles.userCard}>
          <View style={styles.avatar}><Mascot size={50} /></View>
          <View style={styles.userInfo}>
            <Text style={styles.userName}>{vm.displayName}</Text>
            <Text style={styles.userSub}>游客 · {vm.usernameDisplay}</Text>
          </View>
        </View>

        <View style={styles.quotaCard}>
          <View style={styles.cardRow}>
            <Icon name="sparkles-outline" size={16} color={colors.brand500} />
            <Text style={styles.cardTitle}>登录解锁更多</Text>
          </View>
          <Text style={styles.quotaDesc}>
            小工具游客即可使用。登录后可同步数据、参与匿名排行榜、生成 AI 周报。
          </Text>
          <TouchableOpacity
            style={styles.primaryBtn}
            onPress={() => navigateToRoute(navigation, 'Login')}
            activeOpacity={0.85}
          >
            <Text style={styles.primaryBtnText}>登录 / 注册</Text>
          </TouchableOpacity>
        </View>
      </ScrollView>
    );
  }

  // ── Authenticated view ──
  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={[
        styles.content,
        { paddingTop: insets.top + 12, paddingBottom: insets.bottom + 100 },
      ]}
      showsVerticalScrollIndicator={false}
    >
      <View style={styles.headerRow}>
        <Text style={styles.pageTitle}>我的</Text>
        <TouchableOpacity
          style={styles.gearBtn}
          onPress={() => navigation.navigate('Settings')}
          accessibilityLabel="设置"
          activeOpacity={0.75}
        >
          <Icon name="settings-outline" size={20} color={colors.ink500} />
        </TouchableOpacity>
      </View>

      <View style={styles.userCard}>
        <View style={styles.avatar}><Mascot size={50} /></View>
        <View style={styles.userInfo}>
          <Text style={styles.userName}>{vm.displayName}</Text>
          <Text style={styles.userSub}>{vm.usernameDisplay}</Text>
        </View>
        <TouchableOpacity
          style={styles.editChip}
          onPress={() => navigation.navigate('EditProfile', { user: vm.user })}
          activeOpacity={0.8}
        >
          <Icon name="pencil-outline" size={12} color={colors.ink500} />
          <Text style={styles.editChipText}>编辑</Text>
        </TouchableOpacity>
      </View>

      <MenuGroup
        title="排行榜"
        items={[
          {
            icon: 'ribbon-outline',
            label: '我的画像',
            detail: '同城 / 同行 / 同工种',
            onPress: () => navigation.navigate('WorkProfile'),
          },
        ]}
      />

      <MenuGroup
        title="工具"
        items={[
          {
            icon: 'person-outline',
            label: '我的信息',
            detail: '行业 / 职业 / 最讨厌的人',
            onPress: () => navigation.navigate('Onboarding', { edit: true }),
          },
          {
            icon: 'notifications-outline',
            label: '提醒设置',
            detail: '喝水 / 久坐 / 下班',
            onPress: () => navigation.navigate('Reminders'),
          },
        ]}
      />

      <MenuGroup
        title="账号"
        items={[
          {
            icon: 'create-outline',
            label: '编辑个人资料',
            onPress: () => navigation.navigate('EditProfile', { user: vm.user }),
          },
          {
            icon: 'settings-outline',
            label: '设置',
            onPress: () => navigation.navigate('Settings'),
          },
        ]}
      />

      <Text style={styles.versionText}>狗啊喂 goaway · 把时间还给生活</Text>
    </ScrollView>
  );
};

function MenuGroup({ title, items }) {
  return (
    <View style={styles.menuGroup}>
      <Text style={styles.menuGroupTitle}>{title}</Text>
      <View style={styles.menuCard}>
        {items.map((item, i) => (
          <View key={i}>
            {i > 0 && <View style={styles.menuDivider} />}
            <TouchableOpacity
              style={styles.menuRow}
              onPress={item.onPress}
              disabled={!item.onPress}
              activeOpacity={item.onPress ? 0.7 : 1}
            >
              <View style={styles.menuIconWrap}>
                <Icon name={item.icon} size={16} color={colors.brand500} />
              </View>
              <Text style={styles.menuLabel}>{item.label}</Text>
              {item.detail && <Text style={styles.menuDetail}>{item.detail}</Text>}
              <Icon name="chevron-forward" size={16} color={colors.ink300} />
            </TouchableOpacity>
          </View>
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.bg,
  },
  content: {
    paddingHorizontal: spacing.md,
    gap: spacing.md,
  },
  headerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  pageTitle: {
    fontSize: 28,
    fontWeight: '800',
    color: colors.ink900,
    letterSpacing: -0.3,
  },
  gearBtn: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: colors.bgSoft,
    alignItems: 'center',
    justifyContent: 'center',
  },
  userCard: {
    backgroundColor: colors.bgElev,
    borderRadius: radius.lg,
    padding: 18,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
    borderWidth: 0.5,
    borderColor: colors.ink100,
    ...shadows.sm,
  },
  avatar: {
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: colors.sakuraSoft,
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
  avatarGuest: {
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: colors.ink300,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarText: {
    color: '#fff',
    fontSize: 22,
    fontWeight: '700',
  },
  userInfo: {
    flex: 1,
    gap: 3,
  },
  userName: {
    fontSize: 18,
    fontWeight: '700',
    color: colors.ink900,
  },
  userSub: {
    fontSize: 12,
    color: colors.ink500,
  },
  editChip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    height: 30,
    paddingHorizontal: 12,
    borderRadius: radius.pill,
    borderWidth: 1,
    borderColor: colors.ink200,
    backgroundColor: colors.bgElev,
  },
  editChipText: {
    fontSize: 12,
    fontWeight: '500',
    color: colors.ink500,
  },
  quotaCard: {
    backgroundColor: colors.bgElev,
    borderRadius: radius.lg,
    padding: 16,
    gap: 10,
    borderWidth: 0.5,
    borderColor: colors.ink100,
    ...shadows.sm,
  },
  cardRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  cardTitle: {
    fontSize: 15,
    fontWeight: '700',
    color: colors.ink900,
  },
  quotaDesc: {
    fontSize: 13,
    color: colors.ink500,
    lineHeight: 20,
  },
  primaryBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    height: 48,
    borderRadius: radius.md,
    backgroundColor: colors.brand500,
    marginTop: 4,
    ...shadows.pop,
  },
  primaryBtnText: {
    color: '#fff',
    fontSize: 15,
    fontWeight: '700',
  },
  menuGroup: {
    gap: 6,
  },
  menuGroupTitle: {
    fontSize: 11,
    fontWeight: '700',
    color: colors.ink400,
    letterSpacing: 1,
    textTransform: 'uppercase',
    paddingHorizontal: 4,
  },
  menuCard: {
    backgroundColor: colors.bgElev,
    borderRadius: radius.lg,
    overflow: 'hidden',
    borderWidth: 0.5,
    borderColor: colors.ink100,
    ...shadows.sm,
  },
  menuRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingHorizontal: 16,
    paddingVertical: 14,
  },
  menuIconWrap: {
    width: 28,
    height: 28,
    borderRadius: 8,
    backgroundColor: colors.brand50,
    alignItems: 'center',
    justifyContent: 'center',
  },
  menuLabel: {
    flex: 1,
    fontSize: 14.5,
    fontWeight: '500',
    color: colors.ink900,
  },
  menuDetail: {
    fontSize: 12,
    color: colors.ink400,
  },
  menuDivider: {
    height: 0.5,
    backgroundColor: colors.ink100,
    marginLeft: 56,
  },
  versionText: {
    fontSize: 11,
    color: colors.ink300,
    textAlign: 'center',
    paddingVertical: 8,
  },
});

export default ProfileScreen;
