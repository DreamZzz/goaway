import apiClient from '../shared/api/client';
import { authAPI } from '../features/auth/api';
import { uploadAPI } from '../features/media/api';
import { userAPI } from '../features/profile/api';

export {
  apiClient as default,
  authAPI,
  uploadAPI,
  userAPI,
};
