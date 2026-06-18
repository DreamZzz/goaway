import React from 'react';
import {
  Alert,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useAuth } from '../../../app/providers/AuthContext';
import { userAPI } from '../api';
import { getRequestErrorMessage } from '../../../utils/apiError';
import { resetToRoute } from '../../../shared/utils';
import { colors, radius, shadows, spacing } from '../../../shared/theme';

const SettingsSection = ({ title, children }) => (
  <View style={styles.section}>
    <Text style={styles.sectionTitle}>{title}</Text>
    <View style={styles.sectionCard}>{children}</View>
  </View>
);

const SettingsRow = ({ icon, label, value, onPress, destructive }) => (
  <TouchableOpacity style={styles.row} onPress={onPress} activeOpacity={0.7}>
    <View style={styles.rowLeft}>
      <Icon name={icon} size={18} color={destructive ? colors.danger : colors.ink500} style={styles.rowIcon} />
      <Text style={[styles.rowLabel, destructive && styles.rowLabelDestructive]}>{label}</Text>
    </View>
    <View style={styles.rowRight}>
      {value ? <Text style={styles.rowValue} numberOfLines={1}>{value}</Text> : null}
      <Icon name="chevron-forward" size={16} color={colors.ink300} />
    </View>
  </TouchableOpacity>
);

const Divider = () => <View style={styles.divider} />;

const SettingsScreen = ({ navigation }) => {
  const insets = useSafeAreaInsets();
  const { user, logout } = useAuth();

  const handleLogout = () => {
    Alert.alert('退出登录', '确认退出当前账号？', [
      { text: '取消', style: 'cancel' },
      {
        text: '退出',
        style: 'destructive',
        onPress: async () => {
          await logout();
          resetToRoute(navigation, 'HomeTabs', { screen: 'ProfileTab' });
        },
      },
    ]);
  };

  const handleDeleteAccount = () => {
    Alert.alert(
      '注销账号',
      '注销后账号数据将被永久删除且无法恢复，确认继续？',
      [
        { text: '取消', style: 'cancel' },
        {
          text: '我已了解，继续注销',
          style: 'destructive',
          onPress: () => {
            Alert.alert('最终确认', '此操作不可撤销，确认注销账号？', [
              { text: '取消', style: 'cancel' },
              {
                text: '确认注销',
                style: 'destructive',
                onPress: async () => {
                  try {
                    await userAPI.deleteAccount();
                    await logout();
                    resetToRoute(navigation, 'HomeTabs', { screen: 'ProfileTab' });
                  } catch (error) {
                    Alert.alert('注销失败', getRequestErrorMessage(error, '注销失败，请稍后重试'));
                  }
                },
              },
            ]);
          },
        },
      ]
    );
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <ScrollView
        style={styles.container}
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 32 }]}
        showsVerticalScrollIndicator={false}
        keyboardShouldPersistTaps="handled"
      >
        <SettingsSection title="个人资料">
          <SettingsRow
            icon="person-outline"
            label="编辑个人资料"
            value={user?.displayName || user?.username}
            onPress={() => navigation.navigate('EditProfile', { user })}
          />
        </SettingsSection>

        <SettingsSection title="账号">
          <SettingsRow
            icon="mail-outline"
            label="邮箱"
            value={user?.email || '未绑定'}
            onPress={() => navigation.navigate('ChangeEmail')}
          />

          <Divider />

          <SettingsRow
            icon="lock-closed-outline"
            label="修改密码"
            onPress={() => navigation.navigate('ChangePassword')}
          />
        </SettingsSection>

        <SettingsSection title="账号操作">
          <SettingsRow
            icon="log-out-outline"
            label="退出登录"
            onPress={handleLogout}
          />
          <Divider />
          <SettingsRow
            icon="trash-outline"
            label="注销账号"
            onPress={handleDeleteAccount}
            destructive
          />
        </SettingsSection>
      </ScrollView>
    </KeyboardAvoidingView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.bg,
  },
  content: {
    paddingHorizontal: spacing.md,
    paddingTop: spacing.md,
    gap: spacing.md,
  },
  section: {
    gap: spacing.xs,
  },
  sectionTitle: {
    fontSize: 12,
    fontWeight: '700',
    color: colors.ink400,
    textTransform: 'uppercase',
    letterSpacing: 0.8,
    paddingHorizontal: 4,
  },
  sectionCard: {
    backgroundColor: colors.bgElev,
    borderRadius: radius.lg,
    borderWidth: 0.5,
    borderColor: colors.ink100,
    overflow: 'hidden',
    ...shadows.sm,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 14,
    minHeight: 52,
  },
  rowLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    flex: 1,
  },
  rowIcon: {
    width: 20,
  },
  rowLabel: {
    fontSize: 15,
    fontWeight: '600',
    color: colors.ink900,
  },
  rowLabelDestructive: {
    color: colors.danger,
  },
  rowRight: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    maxWidth: '55%',
  },
  rowValue: {
    fontSize: 14,
    color: colors.ink400,
    textAlign: 'right',
  },
  divider: {
    height: 0.5,
    backgroundColor: colors.ink100,
    marginLeft: 46,
  },
});

export default SettingsScreen;
