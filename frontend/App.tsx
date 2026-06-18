/**
 * goaway — 给打工人的实用小工具
 * React Native with Spring Boot Backend
 */

import React, { useEffect } from 'react';
import { AppState, StatusBar } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import Icon from 'react-native-vector-icons/Ionicons';
import { AuthProvider } from './src/app/providers/AuthContext';
import AppNavigator from './src/app/navigation/AppNavigator';
import { AnalyticsProvider } from './src/shared/analytics';
import { readReminderSettings } from './src/features/reminders/storage';
import { applyReminders } from './src/features/reminders/scheduler';

function App(): React.JSX.Element {
  useEffect(() => {
    Icon.loadFont()
      .then(() => undefined)
      .catch(error => console.error('Error loading Ionicons font:', error));
  }, []);

  // 启动与每次回到前台时，按存储的提醒设置重排本地通知（滚动窗口）
  useEffect(() => {
    const reschedule = () => {
      readReminderSettings()
        .then(applyReminders)
        .catch(() => undefined);
    };
    reschedule();
    const sub = AppState.addEventListener('change', state => {
      if (state === 'active') reschedule();
    });
    return () => sub.remove();
  }, []);

  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <SafeAreaProvider>
        <AnalyticsProvider>
          <AuthProvider>
            <StatusBar barStyle="dark-content" backgroundColor="#FFF8F1" />
            <AppNavigator />
          </AuthProvider>
        </AnalyticsProvider>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}

export default App;
