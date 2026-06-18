/* eslint-env jest */

jest.mock('react-native-vector-icons/Ionicons', () => {
  const React = require('react');

  const MockIcon = ({ name, ...props }) =>
    React.createElement('Icon', { ...props, name });

  MockIcon.loadFont = jest.fn(() => Promise.resolve());

  return MockIcon;
});

jest.mock('@react-native-async-storage/async-storage', () => {
  const store = {};

  return {
    setItem: jest.fn(async (key, value) => {
      store[key] = value;
    }),
    getItem: jest.fn(async (key) => (key in store ? store[key] : null)),
    removeItem: jest.fn(async (key) => {
      delete store[key];
    }),
    multiRemove: jest.fn(async (keys) => {
      (keys || []).forEach((key) => {
        delete store[key];
      });
    }),
    clear: jest.fn(async () => {
      Object.keys(store).forEach((key) => delete store[key]);
    }),
  };
});

jest.mock('react-native-iap', () => ({
  initConnection: jest.fn(async () => true),
  endConnection: jest.fn(async () => {}),
  getProducts: jest.fn(async () => []),
  getSubscriptions: jest.fn(async () => []),
  requestPurchase: jest.fn(async () => ({})),
  requestSubscription: jest.fn(async () => ({})),
  finishTransaction: jest.fn(async () => ({})),
  purchaseErrorListener: jest.fn(() => ({ remove: jest.fn() })),
  purchaseUpdatedListener: jest.fn(() => ({ remove: jest.fn() })),
  getReceiptIOS: jest.fn(async () => 'mock-receipt'),
  checkIntroEligibilityIOS: jest.fn(async () => ({})),
}));

jest.mock('@invertase/react-native-apple-authentication', () => {
  const React = require('react');
  const MockAppleButton = (props) => React.createElement('AppleButton', props);
  MockAppleButton.Style = { BLACK: 'BLACK', WHITE: 'WHITE' };
  MockAppleButton.Type = { SIGN_IN: 'SIGN_IN' };
  return {
    __esModule: true,
    default: {
      isSupported: false,
      performRequest: jest.fn(async () => ({ identityToken: 'token', email: null, fullName: null })),
      Operation: { LOGIN: 'LOGIN' },
      Scope: { FULL_NAME: 'FULL_NAME', EMAIL: 'EMAIL' },
      Error: { CANCELED: '1001' },
    },
    AppleButton: MockAppleButton,
    AppleAuthError: { CANCELED: '1001' },
  };
});

jest.mock('react-native-qrcode-svg', () => {
  const React = require('react');
  return {
    __esModule: true,
    default: (props) => React.createElement('QRCode', props),
  };
});

jest.mock('react-native-view-shot', () => ({
  captureRef: jest.fn(async () => 'file:///tmp/mock-capture.png'),
}));
