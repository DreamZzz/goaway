import React, { createContext, useState, useContext, useEffect } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { registerUnauthorizedHandler } from '../../shared/api/client';
import runtimeConfig from '../config/runtime';
import { useAnalytics, EVENTS } from '../../shared/analytics';
import { authAPI } from '../../features/auth/api';
import {
  AUTH_SCOPE_KEY,
  AUTH_SESSION_KIND_KEY,
  AUTH_TOKEN_KEY,
  AUTH_USER_KEY,
  GUEST_INSTALLATION_ID_KEY,
  SESSION_KINDS,
  generateInstallationId,
} from '../../shared/session/guestSession';

const AUTH_SCOPE_VALUE = [
  runtimeConfig.environment || 'local',
  runtimeConfig.proxyTarget || runtimeConfig.apiBaseUrl || '',
].join('|');

const removeStoredAuthKeys = async () => {
  const keys = [AUTH_TOKEN_KEY, AUTH_USER_KEY, AUTH_SCOPE_KEY, AUTH_SESSION_KIND_KEY];

  if (typeof AsyncStorage.multiRemove === 'function') {
    await AsyncStorage.multiRemove(keys);
    return;
  }

  await Promise.all(
    keys.map((key) =>
      typeof AsyncStorage.removeItem === 'function'
        ? AsyncStorage.removeItem(key)
        : Promise.resolve()
    )
  );
};

const AuthContext = createContext({});

export const useAuth = () => useContext(AuthContext);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [token, setToken] = useState(null);
  const [sessionKind, setSessionKind] = useState(SESSION_KINDS.ANONYMOUS);
  const [guestInstallationId, setGuestInstallationId] = useState(null);
  const analytics = useAnalytics();

  useEffect(() => {
    // Check for stored token on app start
    loadStoredToken();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    registerUnauthorizedHandler(async () => {
      setToken(null);
      setUser(null);
      setSessionKind(SESSION_KINDS.ANONYMOUS);
    });

    return () => {
      registerUnauthorizedHandler(null);
    };
  }, []);

  const loadStoredToken = async () => {
    try {
      const [
        storedToken,
        storedUser,
        storedScope,
        storedSessionKind,
        storedInstallationId,
      ] = await Promise.all([
        AsyncStorage.getItem(AUTH_TOKEN_KEY),
        AsyncStorage.getItem(AUTH_USER_KEY),
        AsyncStorage.getItem(AUTH_SCOPE_KEY),
        AsyncStorage.getItem(AUTH_SESSION_KIND_KEY),
        AsyncStorage.getItem(GUEST_INSTALLATION_ID_KEY),
      ]);

      if (storedInstallationId) {
        setGuestInstallationId(storedInstallationId);
      }

      if (storedToken && storedUser && storedUser.trim() !== '' && storedScope !== AUTH_SCOPE_VALUE) {
        await removeStoredAuthKeys();
        setSessionKind(SESSION_KINDS.ANONYMOUS);
        return;
      }

      if (storedToken && storedUser && storedUser.trim() !== '') {
        setToken(storedToken);
        try {
          const parsed = JSON.parse(storedUser);
          setUser(parsed);
          setSessionKind(
            storedSessionKind === SESSION_KINDS.GUEST
              ? SESSION_KINDS.GUEST
              : SESSION_KINDS.AUTHENTICATED
          );
          if (parsed && parsed.id != null && storedSessionKind !== SESSION_KINDS.GUEST) {
            analytics.identify(String(parsed.id));
          }
        } catch (parseError) {
          console.error('Error parsing stored user:', parseError);
          // Clear corrupted data
          await AsyncStorage.removeItem(AUTH_USER_KEY);
          setSessionKind(SESSION_KINDS.ANONYMOUS);
        }
      } else {
        setSessionKind(SESSION_KINDS.ANONYMOUS);
      }
    } catch (error) {
      console.error('Error loading stored auth:', error);
    } finally {
      setLoading(false);
    }
  };

  const getOrCreateGuestInstallationId = async () => {
    if (guestInstallationId) {
      return guestInstallationId;
    }

    const storedInstallationId = await AsyncStorage.getItem(GUEST_INSTALLATION_ID_KEY);
    if (storedInstallationId) {
      setGuestInstallationId(storedInstallationId);
      return storedInstallationId;
    }

    const nextInstallationId = generateInstallationId();
    await AsyncStorage.setItem(GUEST_INSTALLATION_ID_KEY, nextInstallationId);
    setGuestInstallationId(nextInstallationId);
    return nextInstallationId;
  };

  const persistSession = async (userData, authToken, nextSessionKind) => {
    await AsyncStorage.setItem(AUTH_TOKEN_KEY, authToken);
    await AsyncStorage.setItem(AUTH_USER_KEY, JSON.stringify(userData));
    await AsyncStorage.setItem(AUTH_SCOPE_KEY, AUTH_SCOPE_VALUE);
    await AsyncStorage.setItem(AUTH_SESSION_KIND_KEY, nextSessionKind);
    setToken(authToken);
    setUser(userData);
    setSessionKind(nextSessionKind);
  };

  const login = async (userData, authToken) => {
    try {
      const authenticatedUser = {
        ...(userData || {}),
        isGuest: false,
        guestTrialRemaining: null,
      };
      delete authenticatedUser.installationId;

      await persistSession(authenticatedUser, authToken, SESSION_KINDS.AUTHENTICATED);
      if (authenticatedUser && authenticatedUser.id != null) {
        analytics.identify(String(authenticatedUser.id));
      }
    } catch (error) {
      console.error('Error saving auth:', error);
      throw error;
    }
  };

  const logout = async () => {
    try {
      await removeStoredAuthKeys();
      setToken(null);
      setUser(null);
      setSessionKind(SESSION_KINDS.ANONYMOUS);
      analytics.track(EVENTS.AUTH_LOGOUT, {});
      analytics.reset();
    } catch (error) {
      console.error('Error clearing auth:', error);
    }
  };

  const updateUser = async (userData) => {
    try {
      const nextUser = { ...(user || {}), ...(userData || {}) };
      await AsyncStorage.setItem(AUTH_USER_KEY, JSON.stringify(nextUser));
      setUser(nextUser);
    } catch (error) {
      console.error('Error updating user:', error);
    }
  };

  const ensureGuestSession = async () => {
    if (sessionKind === SESSION_KINDS.AUTHENTICATED && token) {
      return {
        token,
        user,
        installationId: guestInstallationId,
        sessionKind,
      };
    }

    if (sessionKind === SESSION_KINDS.GUEST && token) {
      const installationId = await getOrCreateGuestInstallationId();
      return {
        token,
        user,
        installationId,
        sessionKind: SESSION_KINDS.GUEST,
      };
    }

    const installationId = await getOrCreateGuestInstallationId();
    const response = await authAPI.guestLogin(installationId);
    const payload = response?.data || {};

    if (!payload.token) {
      throw new Error('Guest session token missing');
    }

    const guestUser = {
      id: payload.id ?? null,
      username: payload.username || 'guest',
      displayName: payload.displayName || '游客',
      email: payload.email ?? null,
      phone: payload.phone ?? null,
      avatarUrl: payload.avatarUrl ?? null,
      installationId,
      isGuest: true,
      guestTrialRemaining:
        typeof payload.guestTrialRemaining === 'number' ? payload.guestTrialRemaining : null,
    };

    await persistSession(guestUser, payload.token, SESSION_KINDS.GUEST);
    return {
      token: payload.token,
      user: guestUser,
      installationId,
      sessionKind: SESSION_KINDS.GUEST,
    };
  };

  const value = {
    user,
    token,
    loading,
    sessionKind,
    guestInstallationId,
    login,
    logout,
    updateUser,
    ensureGuestSession,
    getOrCreateGuestInstallationId,
    updateGuestTrialRemaining: async (remaining) => {
      if (sessionKind !== SESSION_KINDS.GUEST) return;
      const nextUser = {
        ...(user || {}),
        guestTrialRemaining: typeof remaining === 'number' ? remaining : null,
      };
      await AsyncStorage.setItem(AUTH_USER_KEY, JSON.stringify(nextUser));
      setUser(nextUser);
    },
    isAuthenticated: sessionKind === SESSION_KINDS.AUTHENTICATED && !!token,
    isGuest: sessionKind !== SESSION_KINDS.AUTHENTICATED,
    hasSessionToken: !!token,
    isGuestSessionActive: sessionKind === SESSION_KINDS.GUEST && !!token,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};
