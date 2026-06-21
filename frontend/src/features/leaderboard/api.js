import apiClient from '../../shared/api/client';

export const leaderboardAPI = {
  // 榜单列表（内置 + 配置），返回 [{key,label,unit,periodDefault}]
  boards: () => apiClient.get('/leaderboard/boards'),
  // board: 榜单 key, dimension: all|city|industry|jobType, period: day|week
  get: ({ board = 'fishing', dimension = 'all', slice, period = 'day' } = {}) =>
    apiClient.get('/leaderboard', { params: { board, dimension, slice, period } }),
};
