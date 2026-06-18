import apiClient from '../../shared/api/client';

export const fishingAPI = {
  // 上报新增摸鱼秒数，返回今日/本周/累计汇总
  report: (seconds) => apiClient.post('/fishing/report', { seconds }),
  summary: () => apiClient.get('/fishing/summary'),
};
