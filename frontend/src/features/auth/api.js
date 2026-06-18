import apiClient from '../../shared/api/client';

const AUTH_TRANSITION_REQUEST_CONFIG = Object.freeze({
  skipAuthToken: true,
  skipUnauthorizedLogout: true,
  skipGuestInstallationId: true,
});

export const authAPI = {
  guestLogin: (installationId) =>
    apiClient.post('/auth/guest', {}, {
      ...AUTH_TRANSITION_REQUEST_CONFIG,
      headers: {
        'X-Guest-Installation-Id': installationId,
      },
    }),

  login: (identifier, password, captchaId, captchaCode) =>
    apiClient.post('/auth/login', {
      username: identifier,
      password,
      captchaId,
      captchaCode,
    }, AUTH_TRANSITION_REQUEST_CONFIG),

  loginWithSms: (phone, code) =>
    apiClient.post('/auth/login/sms', { phone, code }, AUTH_TRANSITION_REQUEST_CONFIG),

  demoLogin: () =>
    apiClient.post('/auth/demo-login', {}, AUTH_TRANSITION_REQUEST_CONFIG),

  sendLoginSmsCode: (phone) =>
    apiClient.post('/auth/sms/send', { phone }, AUTH_TRANSITION_REQUEST_CONFIG),

  requestCaptcha: () =>
    apiClient.get('/auth/captcha', AUTH_TRANSITION_REQUEST_CONFIG),

  forgotPassword: (email) =>
    apiClient.post('/auth/password/forgot', { email }, AUTH_TRANSITION_REQUEST_CONFIG),

  resetPassword: (email, code, newPassword) =>
    apiClient.post('/auth/password/reset', { email, code, newPassword }, AUTH_TRANSITION_REQUEST_CONFIG),

  register: (username, email, password, phone) =>
    apiClient.post('/auth/register', { username, email, password, phone }, AUTH_TRANSITION_REQUEST_CONFIG),

  appleLogin: (identityToken, email, fullName) =>
    apiClient.post('/auth/apple', { identityToken, email, fullName }, AUTH_TRANSITION_REQUEST_CONFIG),
};
