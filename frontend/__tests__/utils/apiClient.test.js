import AsyncStorage from '@react-native-async-storage/async-storage';
import apiClient from '../../src/shared/api/client';
import {
  AUTH_SESSION_KIND_KEY,
  AUTH_TOKEN_KEY,
  GUEST_INSTALLATION_ID_KEY,
  SESSION_KINDS,
} from '../../src/shared/session/guestSession';

describe('apiClient auth context', () => {
  const requestInterceptor = apiClient.interceptors.request.handlers[0].fulfilled;
  const responseInterceptor = apiClient.interceptors.response.handlers[0].rejected;

  beforeEach(async () => {
    await AsyncStorage.clear();
    jest.clearAllMocks();
  });

  it('attaches the guest installation header for guest-authenticated requests', async () => {
    await AsyncStorage.setItem(AUTH_TOKEN_KEY, 'guest-token');
    await AsyncStorage.setItem(AUTH_SESSION_KIND_KEY, SESSION_KINDS.GUEST);
    await AsyncStorage.setItem(GUEST_INSTALLATION_ID_KEY, 'installation-123');

    const config = await requestInterceptor({ headers: {} });

    expect(config.headers.Authorization).toBe('Bearer guest-token');
    expect(config.headers['X-Guest-Installation-Id']).toBe('installation-123');
    expect(config.headers['X-Request-ID']).toBeTruthy();
  });

  it('skips auth headers when the request opts out of inherited session state', async () => {
    await AsyncStorage.setItem(AUTH_TOKEN_KEY, 'guest-token');
    await AsyncStorage.setItem(AUTH_SESSION_KIND_KEY, SESSION_KINDS.GUEST);
    await AsyncStorage.setItem(GUEST_INSTALLATION_ID_KEY, 'installation-123');

    const config = await requestInterceptor({
      headers: {},
      skipAuthToken: true,
      skipGuestInstallationId: true,
    });

    expect(config.headers.Authorization).toBeUndefined();
    expect(config.headers['X-Guest-Installation-Id']).toBeUndefined();
    expect(config.headers['X-Request-ID']).toBeTruthy();
  });

  it('does not clear stored auth on 401 when skipUnauthorizedLogout is enabled', async () => {
    await AsyncStorage.setItem(AUTH_TOKEN_KEY, 'guest-token');
    await AsyncStorage.setItem(AUTH_SESSION_KIND_KEY, SESSION_KINDS.GUEST);

    const error = {
      response: { status: 401 },
      config: { skipUnauthorizedLogout: true },
    };

    await expect(responseInterceptor(error)).rejects.toBe(error);

    expect(await AsyncStorage.getItem(AUTH_TOKEN_KEY)).toBe('guest-token');
    expect(await AsyncStorage.getItem(AUTH_SESSION_KIND_KEY)).toBe(SESSION_KINDS.GUEST);
  });
});
