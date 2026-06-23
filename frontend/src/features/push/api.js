import apiClient from '../../shared/api/client';

/**
 * 推送设备注册与毒舌推送偏好。后端 /api/push/*，均需登录。
 */
export const pushAPI = {
  registerDevice: (deviceToken, platform = 'ios') =>
    apiClient.post('/push/devices', { deviceToken, platform }),

  getPrefs: () => apiClient.get('/push/prefs'),

  // patch: { enabled, frequency, quietStart, quietEnd }
  updatePrefs: (patch) => apiClient.put('/push/prefs', patch),

  // 轻量活跃心跳，喂给「不活跃召回」判定
  markActive: () => apiClient.post('/push/active'),
};
