const fs = require('fs');
const path = require('path');

const environment = process.argv[2] || 'local';
const apiBaseUrl = process.argv[3];
const proxyTarget = process.argv[4] || '';
const demoTestLoginEnabled = process.env.APP_DEMO_TEST_LOGIN_ENABLED !== 'false';
const sentryDsn = process.env.APP_SENTRY_DSN || '';
const analyticsProvider = process.env.APP_ANALYTICS_PROVIDER || (environment === 'local' ? 'log' : 'umeng');
const platform = process.env.APP_PLATFORM || 'ios';
const umengAppKey = (platform === 'android'
  ? process.env.APP_ANALYTICS_UMENG_APP_KEY_ANDROID
  : process.env.APP_ANALYTICS_UMENG_APP_KEY_IOS)
  || process.env.APP_ANALYTICS_UMENG_APP_KEY
  || '69d655e26f259537c7935326';
const umengChannel = process.env.APP_ANALYTICS_UMENG_CHANNEL
  || (platform === 'android' ? 'GooglePlay' : 'AppStore');

if (!apiBaseUrl) {
  console.error('Usage: node scripts/write-runtime-config.js <environment> <apiBaseUrl> [proxyTarget]');
  process.exit(1);
}

const outputPath = path.join(__dirname, '..', 'src', 'app', 'config', 'runtime.generated.js');
const runtimeConfig = {
  environment,
  apiBaseUrl,
  proxyTarget,
  demoTestLoginEnabled,
  sentryDsn,
  analyticsProvider,
  umengAppKey,
  umengChannel,
};

// The generated file is plain JS so Metro, Jest, and Xcode bundle steps can all
// read the same runtime config without adding another build-time dependency.
const content = `const runtimeConfig = ${JSON.stringify(runtimeConfig, null, 2)};

export default runtimeConfig;
`;

fs.writeFileSync(outputPath, content, 'utf8');
console.log(`[runtime-config] wrote ${outputPath}`);
