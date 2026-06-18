import apiClient from '../../shared/api/client';

export const soupAPI = {
  daily: () => apiClient.get('/soup/daily'),
  random: () => apiClient.get('/soup/random'),
};
