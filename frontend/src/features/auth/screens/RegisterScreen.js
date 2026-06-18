import React, { useState } from 'react';
import {
  Image,
  View,
  StyleSheet,
  TextInput,
  TouchableOpacity,
  Text,
  KeyboardAvoidingView,
  Platform,
  Alert,
  ScrollView,
  Keyboard,
  TouchableWithoutFeedback,
} from 'react-native';
import { useHeaderHeight } from '@react-navigation/elements';
import { useAuth } from '../../../app/providers/AuthContext';
import { authAPI } from '../../../services/api';
import { API_BASE_URL } from '../../../app/config/api';
import { getRequestErrorMessage } from '../../../utils/apiError';
import { useAnalytics, EVENTS } from '../../../shared/analytics';
import { resetToRoute } from '../../../shared/utils';
import { colors, radius, shadows } from '../../../shared/theme';

const RegisterScreen = ({ navigation, route }) => {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const headerHeight = useHeaderHeight();
  const analytics = useAnalytics();
  const { login } = useAuth();

  const persistAuth = async (payload) => {
    const {
      token,
      id,
      username: nextUsername,
      displayName,
      email: nextEmail,
      phone: userPhone,
      avatarUrl,
    } = payload;
    await login(
      {
        id,
        username: nextUsername,
        displayName,
        email: nextEmail,
        phone: userPhone,
        avatarUrl,
      },
      token
    );
    resetToRoute(navigation, 'HomeTabs');
  };

  const handleRegister = async () => {
    if (!username.trim() || !email.trim() || !password.trim() || !confirmPassword.trim()) {
      Alert.alert('错误', '请填写所有字段');
      return;
    }

    if (password !== confirmPassword) {
      Alert.alert('错误', '密码不一致');
      return;
    }

    if (password.length < 6) {
      Alert.alert('错误', '密码至少6位');
      return;
    }

    setLoading(true);
    analytics.track(EVENTS.AUTH_REGISTER_SUBMIT, { method: 'password' });
    try {
      const response = await authAPI.register(username.trim(), email.trim(), password, null);

      analytics.track(EVENTS.AUTH_REGISTER_SUCCESS, {});
      await persistAuth(response.data);
    } catch (error) {
      console.error('Registration error:', error);
      analytics.track(EVENTS.AUTH_REGISTER_FAIL, {
        error_code: error?.response?.status || 'network',
      });
      Alert.alert(
        '注册失败',
        getRequestErrorMessage(error, `发生错误（${error.response?.status || 'network'}）`, {
          apiBaseUrl: API_BASE_URL,
          includeRequestUrl: __DEV__,
          networkFallbackMessage: __DEV__
            ? '无法连接到后端服务'
            : '无法连接到后端服务，请确认后端和数据库已启动',
        })
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      keyboardVerticalOffset={Platform.OS === 'ios' ? headerHeight : 0}
    >
      <TouchableWithoutFeedback onPress={Keyboard.dismiss} accessible={false}>
        <ScrollView
          showsVerticalScrollIndicator={false}
          keyboardShouldPersistTaps="handled"
          keyboardDismissMode="interactive"
          contentInsetAdjustmentBehavior="always"
          contentContainerStyle={styles.scrollContent}
        >
        <View style={styles.formContainer}>
          <View style={styles.heroCard}>
            <View style={styles.brandRow}>
              <View style={styles.logoWrap}>
                <Image
                  source={require('../../../../assets/branding/app-icon-1024.png')}
                  style={styles.logo}
                />
              </View>
              <View style={styles.brandCopy}>
                <Text style={styles.brandLabel}>今天吃点啥？</Text>
                <Text style={styles.brandHint}>保存菜谱灵感、收藏和个人偏好</Text>
              </View>
            </View>
          </View>

          <View style={styles.inputContainer}>
             <Text style={styles.label}>用户名</Text>
            <TextInput
              style={styles.input}
               placeholder="请输入用户名"
              value={username}
              onChangeText={setUsername}
              autoCapitalize="none"
            />
          </View>

          <View style={styles.inputContainer}>
             <Text style={styles.label}>邮箱</Text>
            <TextInput
              style={styles.input}
               placeholder="请输入邮箱"
              value={email}
              onChangeText={setEmail}
              autoCapitalize="none"
              keyboardType="email-address"
            />
          </View>

          <View style={styles.inputContainer}>
             <Text style={styles.label}>密码</Text>
            <TextInput
              style={styles.input}
               placeholder="请设置密码"
              value={password}
              onChangeText={setPassword}
              secureTextEntry
            />
          </View>

          <View style={styles.inputContainer}>
             <Text style={styles.label}>确认密码</Text>
            <TextInput
              style={styles.input}
               placeholder="请确认密码"
              value={confirmPassword}
              onChangeText={setConfirmPassword}
              secureTextEntry
            />
          </View>

          <TouchableOpacity
            style={[styles.button, loading && styles.buttonDisabled]}
            onPress={handleRegister}
            disabled={loading}
          >
            <Text style={styles.buttonText}>
               {loading ? '创建账户中...' : '注册'}
            </Text>
          </TouchableOpacity>

          <View style={styles.footer}>
             <Text style={styles.footerText}>已有账户？ </Text>
             <TouchableOpacity onPress={() => navigation.navigate('Login', route?.params)}>
               <Text style={styles.footerLink}>登录</Text>
             </TouchableOpacity>
          </View>
        </View>
        </ScrollView>
      </TouchableWithoutFeedback>
    </KeyboardAvoidingView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.bg,
  },
  scrollContent: {
    flexGrow: 1,
    justifyContent: 'center',
    paddingVertical: 24,
  },
  formContainer: {
    paddingHorizontal: 20,
    paddingVertical: 28,
  },
  heroCard: {
    backgroundColor: colors.brand50,
    borderWidth: 0.5,
    borderColor: colors.ink100,
    borderRadius: radius.xl,
    padding: 18,
    marginBottom: 18,
    gap: 10,
    ...shadows.md,
  },
  brandRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  logoWrap: {
    width: 64,
    height: 64,
    borderRadius: radius.md,
    backgroundColor: colors.bgElev,
    alignItems: 'center',
    justifyContent: 'center',
    ...shadows.sm,
  },
  logo: {
    width: 50,
    height: 50,
    borderRadius: radius.sm,
  },
  brandCopy: {
    flex: 1,
    gap: 4,
  },
  brandLabel: {
    color: colors.ink900,
    fontSize: 22,
    fontWeight: '900',
    letterSpacing: 0.2,
  },
  brandHint: {
    color: colors.ink500,
    fontSize: 13,
    fontWeight: '600',
  },
  inputContainer: {
    marginBottom: 18,
  },
  label: {
    fontSize: 14,
    color: colors.ink500,
    marginBottom: 8,
    fontWeight: '700',
  },
  input: {
    backgroundColor: colors.bgElev,
    borderRadius: radius.sm,
    paddingHorizontal: 15,
    paddingVertical: 12,
    fontSize: 16,
    color: colors.ink900,
    borderWidth: 0.5,
    borderColor: colors.ink200,
  },
  button: {
    backgroundColor: colors.brand500,
    borderRadius: radius.sm,
    paddingVertical: 15,
    alignItems: 'center',
    marginTop: 20,
    ...shadows.pop,
  },
  buttonDisabled: {
    backgroundColor: colors.brand300,
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  footer: {
    flexDirection: 'row',
    justifyContent: 'center',
    marginTop: 30,
  },
  footerText: {
    color: colors.ink500,
    fontSize: 14,
  },
  footerLink: {
    color: colors.brand500,
    fontSize: 14,
    fontWeight: '700',
  },
});

export default RegisterScreen;
