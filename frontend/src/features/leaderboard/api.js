import apiClient from '../../shared/api/client';

export const leaderboardAPI = {
  // board: fishing|checkin, dimension: all|city|industry|jobType, period: day|week
  get: ({ board = 'fishing', dimension = 'all', slice, period = 'day' } = {}) =>
    apiClient.get('/leaderboard', { params: { board, dimension, slice, period } }),
};
