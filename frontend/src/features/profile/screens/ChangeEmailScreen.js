import React, { useEffect, useRef, useState } from 'react';
import {
  Alert,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { useHeaderHeight } from '@react-navigation/elements';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useAuth } from '../../../app/providers/AuthContext';
import { userAPI } from '../api';
import { getRequestErrorMessage } from '../../../utils/apiError';
import { colors, radius, shadows, spacing } from '../../../shared/theme';

const COOLDOWN_SECONDS = 60;

const ChangeEmailScreen = ({ navigation }) => {
  const headerHeight = useHeaderHeight();
  const insets = useSafeAreaInsets();
  const { user, updateUser } = useAuth();

  const [newEmail, setNewEmail] = useState('');
  const [code, setCode] = useState('');
  const [codeSent, setCodeSent] = useState(false);
  const [countdown, setCountdown] = useState(0);
  const [sendingCode, setSendingCode] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const timerRef = useRef(null);

  useEffect(() => {
    return () => { if (timerRef.current) clearInterval(timerRef.current); };
  }, []);

  const startCountdown = () => {
    setCountdown(COOLDOWN_SECONDS);
    timerRef.current = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          clearInterval(timerRef.current);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  };

  const handleSendCode = async () => {
    const trimmed = newEmail.trim();
    if (!trimmed || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed)) {
      Alert.alert('请检查', '请输入有效的邮箱地址');
      return;
    }
    setSendingCode(true);
    try {
      await userAPI.sendEmailChangeCode(trimmed);
      setCodeSent(true);
      startCountdown();
    } catch (error) {
      Alert.alert('发送失败', getRequestErrorMessage(error, '发送失败，请稍后重试'));
    } finally {
      setSendingCode(false);
    }
  };

  const handleConfirm = async () => {
    if (!code.trim()) {
      Alert.alert('请输入验证码');
      return;
    }
    setConfirming(true);
    try {
      const response = await userAPI.confirmEmailChange(newEmail.trim(), code.trim());
      await updateUser(response.data);
      Alert.alert('修改成功', `邮箱已更新为 ${newEmail.trim()}`, [
        { text: '好的', onPress: () => navigation.goBack() },
      ]);
    } catch (error) {
      Alert.alert('修改失败', getRequestErrorMessage(error, '验证码错误或已过期，请重新发送'));
    } finally {
      setConfirming(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      keyboardVerticalOffset={Platform.OS === 'ios' ? headerHeight : 0}
    >
      <ScrollView
        style={styles.container}
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 24 }]}
        keyboardShouldPersistTaps="handled"
        keyboardDismissMode="interactive"
      >
        <View style={styles.currentEmailBox}>
          <Text style={styles.currentEmailLabel}>当前邮箱</Text>
          <Text style={styles.currentEmailValue}>{user?.email || '未绑定'}</Text>
        </View>

        <View style={styles.formCard}>
          <View style={styles.section}>
            <Text style={styles.label}>新邮箱</Text>
            <View style={styles.emailRow}>
              <TextInput
                style={[styles.input, styles.emailInput]}
                placeholder="输入新邮箱地址"
                placeholderTextColor={colors.ink300}
                value={newEmail}
                onChangeText={setNewEmail}
                keyboardType="email-address"
                autoCapitalize="none"
                autoCorrect={false}
                editable={!codeSent}
              />
              <TouchableOpacity
                style={[
                  styles.sendCodeButton,
                  (countdown > 0 || sendingCode) && styles.sendCodeButtonDisabled,
                ]}
                onPress={handleSendCode}
                disabled={countdown > 0 || sendingCode}
                activeOpacity={0.84}
              >
                <Text style={styles.sendCodeButtonText}>
                  {sendingCode ? '发送中' : countdown > 0 ? `${countdown}s` : '发送验证码'}
                </Text>
              </TouchableOpacity>
            </View>
          </View>

          {codeSent && (
            <>
              <View style={styles.divider} />
              <View style={styles.section}>
                <Text style={styles.label}>验证码</Text>
                <TextInput
                  style={styles.input}
                  placeholder="输入收到的 6 位验证码"
                  placeholderTextColor={colors.ink300}
                  value={code}
                  onChangeText={setCode}
                  keyboardType="number-pad"
                  maxLength={6}
                />
                <Text style={styles.hint}>验证码已发送至新邮箱，有效期 10 分钟</Text>
              </View>
            </>
          )}
        </View>

        {codeSent && (
          <TouchableOpacity
            style={[styles.submitButton, confirming && styles.submitButtonDisabled]}
            disabled={confirming}
            onPress={handleConfirm}
            activeOpacity={0.86}
          >
            <Text style={styles.submitButtonText}>{confirming ? '确认中...' : '确认修改'}</Text>
          </TouchableOpacity>
        )}
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
  currentEmailBox: {
    backgroundColor: colors.bgElev,
    borderRadius: radius.lg,
    borderWidth: 0.5,
    borderColor: colors.ink100,
    padding: spacing.md,
    gap: 4,
    ...shadows.sm,
  },
  currentEmailLabel: {
    fontSize: 12,
    fontWeight: '600',
    color: colors.ink400,
  },
  currentEmailValue: {
    fontSize: 16,
    color: colors.ink900,
    fontWeight: '700',
  },
  formCard: {
    backgroundColor: colors.bgElev,
    borderRadius: radius.lg,
    borderWidth: 0.5,
    borderColor: colors.ink100,
    overflow: 'hidden',
    ...shadows.sm,
  },
  section: {
    padding: spacing.md,
  },
  label: {
    fontSize: 12,
    fontWeight: '700',
    color: colors.ink400,
    marginBottom: 8,
  },
  emailRow: {
    flexDirection: 'row',
    gap: 10,
    alignItems: 'center',
  },
  input: {
    backgroundColor: colors.bgSoft,
    borderRadius: radius.sm,
    borderWidth: 0.5,
    borderColor: colors.ink100,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontSize: 16,
    color: colors.ink900,
  },
  emailInput: {
    flex: 1,
  },
  sendCodeButton: {
    backgroundColor: colors.brand500,
    borderRadius: radius.sm,
    paddingHorizontal: 14,
    paddingVertical: 12,
    alignItems: 'center',
    justifyContent: 'center',
    minWidth: 90,
  },
  sendCodeButtonDisabled: {
    backgroundColor: colors.ink300,
  },
  sendCodeButtonText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
  },
  hint: {
    fontSize: 12,
    color: colors.ink400,
    marginTop: 6,
  },
  submitButton: {
    backgroundColor: colors.brand500,
    borderRadius: radius.md,
    paddingVertical: 15,
    alignItems: 'center',
    marginTop: 8,
    ...shadows.pop,
  },
  submitButtonDisabled: {
    backgroundColor: colors.ink300,
    shadowOpacity: 0,
  },
  submitButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '800',
  },
  divider: {
    height: 0.5,
    backgroundColor: colors.ink100,
    marginLeft: spacing.md,
  },
});

export default ChangeEmailScreen;
