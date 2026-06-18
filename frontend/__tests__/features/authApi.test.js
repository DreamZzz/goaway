const mockPost = jest.fn();
const mockGet = jest.fn();

jest.mock('../../src/shared/api/client', () => ({
  __esModule: true,
  default: {
    post: (...args) => mockPost(...args),
    get: (...args) => mockGet(...args),
  },
}));

import { authAPI } from '../../src/features/auth/api';

describe('authAPI session transition requests', () => {
  beforeEach(() => {
    mockPost.mockReset();
    mockGet.mockReset();
  });

  it('sends password login without inheriting guest auth state', () => {
    authAPI.login('appletest', 'secret', 'captcha-id', '1234');

    expect(mockPost).toHaveBeenCalledWith(
      '/auth/login',
      {
        username: 'appletest',
        password: 'secret',
        captchaId: 'captcha-id',
        captchaCode: '1234',
      },
      expect.objectContaining({
        skipAuthToken: true,
        skipUnauthorizedLogout: true,
        skipGuestInstallationId: true,
      })
    );
  });

  it('keeps guest login bound to the installation header without inheriting stale auth', () => {
    authAPI.guestLogin('installation-123');

    expect(mockPost).toHaveBeenCalledWith(
      '/auth/guest',
      {},
      expect.objectContaining({
        skipAuthToken: true,
        skipUnauthorizedLogout: true,
        skipGuestInstallationId: true,
        headers: {
          'X-Guest-Installation-Id': 'installation-123',
        },
      })
    );
  });
});
