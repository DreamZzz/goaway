import { useCallback, useMemo } from 'react';
import { Alert } from 'react-native';
import { useAuth } from '../../../app/providers/AuthContext';
import { userAPI } from '../api';

/**
 * ViewModel for ProfileScreen.
 * Owns: user display values, logout / delete-account actions.
 */
export const useProfileViewModel = () => {
  const { user, logout, isGuest, guestInstallationId } = useAuth();

  const displayName = useMemo(
    () => (isGuest ? '游客模式' : user?.displayName || user?.username || 'goaway 用户'),
    [isGuest, user]
  );

  const usernameDisplay = useMemo(
    () => {
      if (isGuest) {
        return guestInstallationId ? `访客 ${guestInstallationId.slice(-6)}` : '未登录';
      }
      return user?.username ? '@' + user.username : '--';
    },
    [guestInstallationId, isGuest, user]
  );

  const deleteAccount = useCallback(() => {
    Alert.alert(
      '注销账号',
      '注销后，您的账号信息和个人数据将被永久删除，且无法恢复。确定要继续吗？',
      [
        { text: '取消', style: 'cancel' },
        {
          text: '继续注销',
          style: 'destructive',
          onPress: () => {
            Alert.alert(
              '最终确认',
              '此操作不可撤销。您的账号将被立即注销。',
              [
                { text: '再想想', style: 'cancel' },
                {
                  text: '确认注销',
                  style: 'destructive',
                  onPress: async () => {
                    try {
                      await userAPI.deleteAccount();
                      await logout();
                    } catch (err) {
                      console.error('Delete account failed:', err);
                      Alert.alert('注销失败', '请稍后再试，或联系客服。');
                    }
                  },
                },
              ]
            );
          },
        },
      ]
    );
  }, [logout]);

  return {
    user,
    isGuest,
    displayName,
    usernameDisplay,
    logout,
    deleteAccount,
  };
};
