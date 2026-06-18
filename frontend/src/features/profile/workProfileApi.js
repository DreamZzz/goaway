import apiClient from '../../shared/api/client';

export const workProfileAPI = {
  get: () => apiClient.get('/profile/work'),
  update: (data) => apiClient.put('/profile/work', data),
};
