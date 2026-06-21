import apiClient from '../../shared/api/client';

export const activityAPI = {
  // 异步记录一次离散动作（WATER | SMOKE | POOP），fire-and-forget
  record: (type, durationSeconds) =>
    apiClient.post('/activity/events', { type, durationSeconds }),
  // 今日汇总（登录态），结构同本地计数：{ water, smoke, poopCount, poopSeconds }
  summary: () => apiClient.get('/activity/summary'),
};
