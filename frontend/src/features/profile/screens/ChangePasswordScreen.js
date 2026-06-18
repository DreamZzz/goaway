import React, { useState } from 'react';
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
import { userAPI } from '../api';
import { getRequestErrorMessage } from '../../../utils/apiError';
import { colors, radius, shadows, spacing } from '../../../shared/theme';

const ChangePasswordScreen = ({ navigation }) => {
  const headerHeight = useHeaderHeight();
  const insets = useSafeAreaInsets();
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [saving, setSaving] = useState(false);

  const handleSubmit = async () => {
    if (!currentPassword || !newPassword || !confirmPassword) {
      Alert.alert('请填写完整', '所有字段均为必填');
      return;
    }
    if (newPassword.length < 6) {
      Alert.alert('密码太短', '新密码至少需要 6 位');
      return;
    }
    if (newPassword !== confirmPassword) {
      Alert.alert('密码不一致', '两次输入的新密码不一致');
      return;
    }
    setSaving(true);
    try {
      await userAPI.changePassword(currentPassword, newPassword);
      Alert.alert('修改成功', '密码已更新，下次登录请使用新密码', [
        { text: '好的', onPress: () => navigation.goBack() },
      ]);
    } catch (error) {
      Alert.alert('修改失败', getRequestErrorMessage(error, '修改失败，请稍后重试'));
    } finally {
      setSaving(false);
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
        <View style={styles.formCard}>
          <View style={styles.section}>
            <Text style={styles.label}>当前密码</Text>
            <TextInput
              style={styles.input}
              placeholder="输入当前密码"
              placeholderTextColor={colors.ink300}
              value={currentPassword}
              onChangeText={setCurrentPassword}
              secureTextEntry
              autoCapitalize="none"
            />
          </View>

          <View style={styles.divider} />

          <View style={styles.section}>
            <Text style={styles.label}>新密码</Text>
            <TextInput
              style={styles.input}
              placeholder="至少 6 位"
              placeholderTextColor={colors.ink300}
              value={newPassword}
              onChangeText={setNewPassword}
              secureTextEntry
              autoCapitalize="none"
            />
          </View>

          <View style={styles.divider} />

          <View style={styles.section}>
            <Text style={styles.label}>确认新密码</Text>
            <TextInput
              style={styles.input}
              placeholder="再次输入新密码"
              placeholderTextColor={colors.ink300}
              value={confirmPassword}
              onChangeText={setConfirmPassword}
              secureTextEntry
              autoCapitalize="none"
            />
          </View>
        </View>

        <TouchableOpacity
          style={styles.forgotLink}
          onPress={() => navigation.navigate('ForgotPassword')}
        >
          <Text style={styles.forgotLinkText}>忘记当前密码？</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[styles.submitButton, saving && styles.submitButtonDisabled]}
          disabled={saving}
          onPress={handleSubmit}
          activeOpacity={0.86}
        >
          <Text style={styles.submitButtonText}>{saving ? '保存中...' : '确认修改'}</Text>
        </TouchableOpacity>
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
  forgotLink: {
    alignSelf: 'flex-start',
    paddingHorizontal: 2,
  },
  forgotLinkText: {
    fontSize: 14,
    color: colors.brand500,
    fontWeight: '600',
  },
  submitButton: {
    backgroundColor: colors.brand500,
    borderRadius: radius.md,
    paddingVertical: 15,
    alignItems: 'center',
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

export default ChangePasswordScreen;
