export const AUTH_TOKEN_KEY = 'auth_token';
export const AUTH_USER_KEY = 'user';
export const AUTH_SCOPE_KEY = 'auth_scope';
export const AUTH_SESSION_KIND_KEY = 'auth_session_kind';
export const GUEST_INSTALLATION_ID_KEY = 'guest_installation_id';

export const SESSION_KINDS = Object.freeze({
  ANONYMOUS: 'anonymous',
  GUEST: 'guest',
  AUTHENTICATED: 'authenticated',
});

export const GUEST_TRIAL_LIMIT = 3;

export const generateInstallationId = () =>
  'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (character) => {
    const randomNibble = Math.floor(Math.random() * 16);
    const value = character === 'x'
      ? randomNibble
      : 8 + Math.floor(Math.random() * 4);
    return value.toString(16);
  });
