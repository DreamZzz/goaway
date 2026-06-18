import apiClient from '../../shared/api/client';

export const userAPI = {
  getProfile: (id) =>
    apiClient.get(`/users/${id}`),

  updateProfile: (id, userData) =>
    apiClient.put(`/users/${id}`, userData),

  deleteAccount: () =>
    apiClient.delete('/users/me'),

  changePassword: (currentPassword, newPassword) =>
    apiClient.post('/auth/password/change', { currentPassword, newPassword }),

  sendEmailChangeCode: (newEmail) =>
    apiClient.post('/auth/email/change/send-code', { newEmail }),

  confirmEmailChange: (newEmail, code) =>
    apiClient.post('/auth/email/change/confirm', { newEmail, code }),
};
