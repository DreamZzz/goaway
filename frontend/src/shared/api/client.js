import axios from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { API_BASE_URL } from '../../app/config/api';
import {
  AUTH_SCOPE_KEY,
  AUTH_SESSION_KIND_KEY,
  AUTH_TOKEN_KEY,
  AUTH_USER_KEY,
  GUEST_INSTALLATION_ID_KEY,
  SESSION_KINDS,
} from '../session/guestSession';

let unauthorizedHandler = null;
const AUTH_STORAGE_KEYS = [
  AUTH_TOKEN_KEY,
  AUTH_USER_KEY,
  AUTH_SCOPE_KEY,
  AUTH_SESSION_KIND_KEY,
];

const clearStoredAuth = async () => {
  if (typeof AsyncStorage.multiRemove === 'function') {
    await AsyncStorage.multiRemove(AUTH_STORAGE_KEYS);
    return;
  }

  await Promise.all(
    AUTH_STORAGE_KEYS.map((key) =>
      typeof AsyncStorage.removeItem === 'function'
        ? AsyncStorage.removeItem(key)
        : Promise.resolve()
    )
  );
};

export const generateRequestId = () =>
  'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
    const randomNibble = Math.floor(Math.random() * 16);
    const value = c === 'x'
      ? randomNibble
      : 8 + Math.floor(Math.random() * 4);
    return value.toString(16);
  });

export const getStoredAuthContext = async () => {
  const [token, sessionKind, guestInstallationId] = await Promise.all([
    AsyncStorage.getItem(AUTH_TOKEN_KEY),
    AsyncStorage.getItem(AUTH_SESSION_KIND_KEY),
    AsyncStorage.getItem(GUEST_INSTALLATION_ID_KEY),
  ]);

  return {
    token,
    sessionKind,
    guestInstallationId,
  };
};

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use(
  async (config) => {
    config.headers = config.headers || {};
    config.headers['X-Request-ID'] = generateRequestId();
    try {
      const { token, sessionKind, guestInstallationId } = await getStoredAuthContext();
      if (token && !config.skipAuthToken) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      if (
        token &&
        sessionKind === SESSION_KINDS.GUEST &&
        guestInstallationId &&
        !config.skipGuestInstallationId
      ) {
        config.headers['X-Guest-Installation-Id'] = guestInstallationId;
      }
    } catch (error) {
      console.error('Error getting token:', error);
    }
    return config;
  },
  (error) => Promise.reject(error)
);

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401 && !error.config?.skipUnauthorizedLogout) {
      await clearStoredAuth();
      if (typeof unauthorizedHandler === 'function') {
        await unauthorizedHandler(error);
      }
    }
    return Promise.reject(error);
  }
);

export const registerUnauthorizedHandler = (handler) => {
  unauthorizedHandler = handler;
};

/** Called by XHR-based (SSE) requests that bypass the axios interceptor. */
export const handleUnauthorized = async () => {
  await clearStoredAuth();
  if (typeof unauthorizedHandler === 'function') {
    await unauthorizedHandler();
  }
};

export default apiClient;
