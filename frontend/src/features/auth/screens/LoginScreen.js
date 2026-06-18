import React, { useState } from 'react';
import {
  Alert,
  Image,
  Keyboard,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  TouchableWithoutFeedback,
  View,
} from 'react-native';
import appleAuth, { AppleButton, AppleAuthError } from '@invertase/react-native-apple-authentication';
import { useHeaderHeight } from '@react-navigation/elements';
import { useAuth } from '../../../app/providers/AuthContext';
import { authAPI } from '../../../services/api';
import { API_BASE_URL } from '../../../app/config/api';
import { getRequestErrorMessage, getResponseErrorMessage } from '../../../utils/apiError';
import { useAnalytics, EVENTS } from '../../../shared/analytics';
import { resetToRoute } from '../../../shared/utils';
import { colors, radius, shadows } from '../../../shared/theme';

const LoginScreen = ({ navigation, route }) => {
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [captcha, setCaptcha] = useState(null);
  const [captchaCode, setCaptchaCode] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const headerHeight = useHeaderHeight();
  const analytics = useAnalytics();

  const fetchCaptcha = async () => {
    try {
      const response = await authAPI.requestCaptcha();
      setCaptcha(response.data);
      setCaptchaCode('');
    } catch (error) {
      console.error('Failed to fetch captcha:', error);
      Alert.alert('错误', '获取图形验证码失败，请稍后再试');
    }
  };

  const persistAuth = async (payload) => {
    const { token, id, username, displayName, email, phone: userPhone, avatarUrl } = payload;
    await login(
      {
        id,
        username,
        displayName,
        email,
        phone: userPhone,
        avatarUrl,
      },
      token
    );
    resetToRoute(navigation, 'HomeTabs');
  };

  const handlePasswordLogin = async () => {
    if (!identifier.trim() || !password.trim()) {
      Alert.alert('错误', '请填写账号和密码');
      return;
    }

    if (captcha && !captchaCode.trim()) {
      Alert.alert('错误', '请输入图形验证码');
      return;
    }

    setLoading(true);
    analytics.track(EVENTS.AUTH_LOGIN_SUBMIT, { method: 'password' });
    try {
      const response = await authAPI.login(
        identifier.trim(),
        password,
        captcha?.captchaId,
        captchaCode.trim()
      );
      setCaptcha(null);
      setCaptchaCode('');
      analytics.track(EVENTS.AUTH_LOGIN_SUCCESS, { method: 'password' });
      await persistAuth(response.data);
    } catch (error) {
      console.error('Login error:', error);
      analytics.track(EVENTS.AUTH_LOGIN_FAIL, {
        method: 'password',
        error_code: error?.response?.status || 'network',
      });
      const responseData = error.response?.data;

      if (!error.response) {
        Alert.alert(
          '登录失败',
          getRequestErrorMessage(error, '登录失败', {
            apiBaseUrl: API_BASE_URL,
            includeRequestUrl: true,
            includeErrorCode: true,
            networkFallbackMessage: '无法连接到后端服务',
          })
        );
        return;
      }

      if (responseData?.captchaRequired) {
        await fetchCaptcha();
      }

      if (error.response.status === 401) {
        Alert.alert('登录失败', getResponseErrorMessage(error, '用户名、邮箱或密码错误'));
        return;
      }

      Alert.alert('登录失败', getResponseErrorMessage(error, '服务器异常，请稍后重试'));
    } finally {
      setLoading(false);
    }
  };

  const handleLogin = () => handlePasswordLogin();

  const handleAppleLogin = async () => {
    setLoading(true);
    analytics.track(EVENTS.AUTH_LOGIN_SUBMIT, { method: 'apple' });
    try {
      const response = await appleAuth.performRequest({
        requestedOperation: appleAuth.Operation.LOGIN,
        requestedScopes: [appleAuth.Scope.FULL_NAME, appleAuth.Scope.EMAIL],
      });
      const { identityToken, email, fullName } = response;
      if (!identityToken) {
        throw new Error('Apple 未返回身份凭证');
      }
      const displayName =
        [fullName?.givenName, fullName?.familyName].filter(Boolean).join(' ') || null;
      const apiResponse = await authAPI.appleLogin(identityToken, email, displayName);
      analytics.track(EVENTS.AUTH_LOGIN_SUCCESS, { method: 'apple' });
      await persistAuth(apiResponse.data);
    } catch (error) {
      if (error.code === AppleAuthError.CANCELED) {
        return;
      }
      console.error('Apple login error:', error);
      analytics.track(EVENTS.AUTH_LOGIN_FAIL, {
        method: 'apple',
        error_code: error?.response?.status || 'unknown',
      });
      Alert.alert('Apple 登录失败', '请稍后再试');
    } finally {
      setLoading(false);
    }
  };

  const handleDemoLogin = async () => {
    setLoading(true);
    try {
      const response = await authAPI.demoLogin();
      await persistAuth(response.data);
    } catch (error) {
      console.error('Demo login error:', error);
      Alert.alert('测试登录失败', getRequestErrorMessage(error, '请检查后端服务后重试'));
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
          style={styles.container}
          contentContainerStyle={styles.scrollContent}
          keyboardShouldPersistTaps="handled"
          keyboardDismissMode="interactive"
          contentInsetAdjustmentBehavior="always"
          showsVerticalScrollIndicator={false}
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
                  <Text style={styles.brandHint}>继续你的菜单灵感、收藏和完整菜谱</Text>
                </View>
              </View>
            </View>

            <View style={styles.inputContainer}>
              <Text style={styles.label}>用户名或邮箱</Text>
              <TextInput
                style={styles.input}
                placeholder="请输入用户名或邮箱"
                value={identifier}
                onChangeText={setIdentifier}
                autoCapitalize="none"
              />
            </View>

            <View style={styles.inputContainer}>
              <Text style={styles.label}>密码</Text>
              <TextInput
                style={styles.input}
                placeholder="请输入密码"
                value={password}
                onChangeText={setPassword}
                secureTextEntry
              />
            </View>

            {captcha ? (
              <View style={styles.inputContainer}>
                <Text style={styles.label}>图形验证码</Text>
                <View style={styles.captchaRow}>
                  <TextInput
                    style={[styles.input, styles.captchaInput]}
                    placeholder="输入验证码"
                    value={captchaCode}
                    onChangeText={setCaptchaCode}
                    autoCapitalize="characters"
                  />
                  <TouchableOpacity style={styles.captchaBox} onPress={fetchCaptcha}>
                    <Image source={{ uri: captcha.imageBase64 }} style={styles.captchaImage} />
                  </TouchableOpacity>
                </View>
              </View>
            ) : null}

            <TouchableOpacity
              style={styles.secondaryLinkContainer}
              onPress={() => navigation.navigate('ForgotPassword')}
            >
              <Text style={styles.secondaryLink}>忘记密码？</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[styles.button, loading && styles.buttonDisabled]}
              onPress={handleLogin}
              disabled={loading}
            >
              <Text style={styles.buttonText}>{loading ? '登录中...' : '登录'}</Text>
            </TouchableOpacity>

            {appleAuth.isSupported && (
              <>
                <View style={styles.divider}>
                  <View style={styles.dividerLine} />
                  <Text style={styles.dividerText}>或</Text>
                  <View style={styles.dividerLine} />
                </View>
                <AppleButton
                  buttonStyle={AppleButton.Style.BLACK}
                  buttonType={AppleButton.Type.SIGN_IN}
                  cornerRadius={radius.sm}
                  style={styles.appleButton}
                  onPress={handleAppleLogin}
                />
              </>
            )}

            {__DEV__ ? (
              <View style={styles.demoCard}>
                <View style={styles.demoBadge}>
                  <Text style={styles.demoBadgeText}>开发调试</Text>
                </View>
                <Text style={styles.demoTitle}>使用测试账号快速进入</Text>
                <Text style={styles.demoDescription}>
                  本地联调时可直接调用后端 demo login，避免每次手动输入账号密码。
                </Text>
                <TouchableOpacity
                  style={[styles.demoButton, loading && styles.buttonDisabled]}
                  onPress={handleDemoLogin}
                  disabled={loading}
                >
                  <Text style={styles.demoButtonText}>测试登录</Text>
                </TouchableOpacity>
              </View>
            ) : null}

            <View style={styles.footer}>
              <Text style={styles.footerText}>还没有账户？ </Text>
              <TouchableOpacity onPress={() => navigation.navigate('Register', route?.params)}>
                <Text style={styles.footerLink}>注册</Text>
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
  captchaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  captchaInput: {
    flex: 1,
  },
  captchaBox: {
    width: 118,
    height: 48,
    borderRadius: radius.sm,
    borderWidth: 0.5,
    borderColor: colors.ink200,
    overflow: 'hidden',
    backgroundColor: colors.bgElev,
  },
  captchaImage: {
    width: '100%',
    height: '100%',
  },
  secondaryLinkContainer: {
    alignSelf: 'flex-end',
    marginTop: -8,
  },
  secondaryLink: {
    color: colors.brand500,
    fontSize: 13,
    fontWeight: '700',
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
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
  },
  divider: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 20,
    marginBottom: 16,
    gap: 10,
  },
  dividerLine: {
    flex: 1,
    height: 0.5,
    backgroundColor: colors.ink200,
  },
  dividerText: {
    color: colors.ink400,
    fontSize: 13,
    fontWeight: '600',
  },
  appleButton: {
    height: 50,
    width: '100%',
  },
  demoCard: {
    marginTop: 16,
    padding: 16,
    borderRadius: radius.md,
    backgroundColor: colors.bgSoft,
    borderWidth: 0.5,
    borderColor: colors.ink200,
  },
  demoBadge: {
    alignSelf: 'flex-start',
    backgroundColor: colors.brand100,
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: radius.pill,
    marginBottom: 10,
  },
  demoBadgeText: {
    color: colors.brand500,
    fontSize: 12,
    fontWeight: '700',
  },
  demoTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: colors.ink900,
    marginBottom: 6,
  },
  demoDescription: {
    fontSize: 13,
    lineHeight: 19,
    color: colors.ink500,
    marginBottom: 12,
  },
  demoButton: {
    borderRadius: radius.sm,
    backgroundColor: colors.brand500,
    paddingVertical: 13,
    alignItems: 'center',
  },
  demoButtonText: {
    color: '#fff',
    fontSize: 15,
    fontWeight: '700',
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

export default LoginScreen;
