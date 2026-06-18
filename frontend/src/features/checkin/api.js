import apiClient from '../../shared/api/client';

export const checkinAPI = {
  // 今日打卡（幂等），返回连续天数等汇总
  checkin: () => apiClient.post('/checkin'),
  // 打卡汇总
  summary: () => apiClient.get('/checkin/summary'),
};
