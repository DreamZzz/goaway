import apiClient from '../../shared/api/client';

export const badgesAPI = {
  // 荣誉墙：全部徽章（已解锁 + 未解锁带进度）
  list: () => apiClient.get('/badges'),
};

// 阈值/进度按单位格式化展示
export const formatBadgeValue = (value, unit) => {
  if (unit === 'seconds') {
    const h = Math.floor(value / 3600);
    const m = Math.floor((value % 3600) / 60);
    if (h > 0) return m > 0 ? `${h} 小时 ${m} 分` : `${h} 小时`;
    if (m > 0) return `${m} 分`;
    return `${value} 秒`;
  }
  return `${value} 次`;
};
