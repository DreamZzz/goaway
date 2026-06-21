import React, { useEffect, useState } from 'react';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';
import { NavigationContainer, DefaultTheme, createNavigationContainerRef } from '@react-navigation/native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createStackNavigator } from '@react-navigation/stack';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import Icon from 'react-native-vector-icons/Ionicons';
import GIcon from '../../shared/components/Icon';
import { useAuth } from '../providers/AuthContext';
import HomeScreen from '../../features/home/screens/HomeScreen';
import WeeklyScreen from '../../features/weekly/screens/WeeklyScreen';
import RoleplayScreen from '../../features/roleplay/screens/RoleplayScreen';
import LeaderboardScreen from '../../features/leaderboard/screens/LeaderboardScreen';
import LoginScreen from '../../features/auth/screens/LoginScreen';
import RegisterScreen from '../../features/auth/screens/RegisterScreen';
import ForgotPasswordScreen from '../../features/auth/screens/ForgotPasswordScreen';
import ProfileScreen from '../../features/profile/screens/ProfileScreen';
import SettingsScreen from '../../features/profile/screens/SettingsScreen';
import EditProfileScreen from '../../features/profile/screens/EditProfileScreen';
import ChangeEmailScreen from '../../features/profile/screens/ChangeEmailScreen';
import ChangePasswordScreen from '../../features/profile/screens/ChangePasswordScreen';
import WorkProfileScreen from '../../features/profile/screens/WorkProfileScreen';
import BadgesScreen from '../../features/badges/screens/BadgesScreen';
import ReminderScreen from '../../features/reminders/screens/ReminderScreen';
import OnboardingScreen from '../../features/onboarding/screens/OnboardingScreen';
import { isOnboarded } from '../../features/onboarding/storage';
import { hydrateWorkProfile } from '../../features/onboarding/sync';
import { colors } from '../../shared/theme';

const RootStack = createStackNavigator();
const Tab = createBottomTabNavigator();

export const navigationRef = createNavigationContainerRef();

const navigationTheme = {
  ...DefaultTheme,
  colors: {
    ...DefaultTheme.colors,
    background: colors.bg,
    card: colors.bgElev,
    border: colors.ink100,
    primary: colors.brand500,
    text: colors.ink900,
  },
};

const TAB_GLYPH = { HomeTab: 'home', LeaderboardTab: 'trophy', BadgesTab: 'medal', ProfileTab: 'mascot' };

const TAB_LABELS = {
  HomeTab: '首页',
  LeaderboardTab: '排行榜',
  BadgesTab: '勋章',
  ProfileTab: '我的',
};

const createTabBarIcon = (routeName) => ({ focused }) => {
  const glyph = TAB_GLYPH[routeName];
  if (glyph === 'home') {
    return <GIcon name="home" size={26} color={focused ? colors.brand500 : colors.ink300} />;
  }
  return <GIcon name={glyph} size={glyph === 'mascot' ? 30 : 26} style={focused ? styles.tabIconOn : styles.tabIconDim} />;
};

const TAB_BAR_ICON_RENDERERS = {
  HomeTab: createTabBarIcon('HomeTab'),
  LeaderboardTab: createTabBarIcon('LeaderboardTab'),
  BadgesTab: createTabBarIcon('BadgesTab'),
  ProfileTab: createTabBarIcon('ProfileTab'),
};

const HeaderBackIcon = ({ tintColor }) => (
  <Icon
    name="chevron-back"
    size={22}
    color={tintColor || colors.ink900}
    style={styles.headerBackIcon}
  />
);

function TabsNavigator() {
  const insets = useSafeAreaInsets();

  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarActiveTintColor: colors.brand500,
        tabBarInactiveTintColor: colors.ink400,
        tabBarStyle: {
          backgroundColor: 'rgba(251,247,242,0.92)',
          borderTopColor: colors.ink200,
          borderTopWidth: 0.5,
          paddingBottom: Math.max(8, insets.bottom),
          height: 56 + Math.max(8, insets.bottom),
        },
        tabBarLabelStyle: {
          fontSize: 10,
          fontWeight: '600',
          marginBottom: 2,
        },
        tabBarIcon: TAB_BAR_ICON_RENDERERS[route.name],
        tabBarLabel: TAB_LABELS[route.name] || route.name,
      })}
    >
      <Tab.Screen name="HomeTab" component={HomeScreen} />
      <Tab.Screen name="LeaderboardTab" component={LeaderboardScreen} />
      <Tab.Screen name="BadgesTab" component={BadgesScreen} />
      <Tab.Screen name="ProfileTab" component={ProfileScreen} />
    </Tab.Navigator>
  );
}

const LoadingGate = () => (
  <View style={styles.loadingGate}>
    <ActivityIndicator size="large" color={colors.brand500} />
    <Text style={styles.loadingGateText}>正在准备 goaway…</Text>
  </View>
);

export default function AppNavigator() {
  const { loading, isAuthenticated } = useAuth();
  const insets = useSafeAreaInsets();
  const [onboardChecked, setOnboardChecked] = useState(false);
  const [needOnboarding, setNeedOnboarding] = useState(false);

  useEffect(() => {
    if (loading) return; // 等鉴权状态就绪
    (async () => {
      let done = await isOnboarded();
      // 已登录但本地无引导标记时，尝试从账号拉回画像（避免重装/换设备后重复填写）
      if (!done && isAuthenticated) {
        done = await hydrateWorkProfile();
      }
      setNeedOnboarding(!done);
      setOnboardChecked(true);
    })();
  }, [loading, isAuthenticated]);

  if (loading || !onboardChecked) {
    return <LoadingGate />;
  }

  return (
    <NavigationContainer ref={navigationRef} theme={navigationTheme}>
      <RootStack.Navigator
        initialRouteName={needOnboarding ? 'Onboarding' : 'HomeTabs'}
        screenOptions={{
          headerStyle: { backgroundColor: colors.bgElev },
          headerTintColor: colors.ink900,
          headerTitleStyle: { fontWeight: '700', fontSize: 17 },
          contentStyle: { backgroundColor: colors.bg },
          headerStatusBarHeight: insets.top,
          headerBackImage: HeaderBackIcon,
        }}
      >
        <RootStack.Screen
          name="Onboarding"
          component={OnboardingScreen}
          options={{ headerShown: false, gestureEnabled: false }}
        />
        <RootStack.Screen
          name="HomeTabs"
          component={TabsNavigator}
          options={{ headerShown: false }}
        />
        <RootStack.Screen
          name="Weekly"
          component={WeeklyScreen}
          options={{ headerShown: false }}
        />
        <RootStack.Screen
          name="Roleplay"
          component={RoleplayScreen}
          options={{ headerShown: false }}
        />
        <RootStack.Screen
          name="WorkProfile"
          component={WorkProfileScreen}
          options={{ title: '我的画像' }}
        />
        <RootStack.Screen
          name="Reminders"
          component={ReminderScreen}
          options={{ headerShown: false }}
        />
        <RootStack.Screen
          name="Login"
          component={LoginScreen}
          options={{ headerShown: false }}
        />
        <RootStack.Screen
          name="Register"
          component={RegisterScreen}
          options={{ title: '注册' }}
        />
        <RootStack.Screen
          name="ForgotPassword"
          component={ForgotPasswordScreen}
          options={{ title: '找回密码' }}
        />
        <RootStack.Screen
          name="Settings"
          component={SettingsScreen}
          options={{ title: '设置' }}
        />
        <RootStack.Screen
          name="EditProfile"
          component={EditProfileScreen}
          options={{ title: '编辑个人资料' }}
        />
        <RootStack.Screen
          name="ChangeEmail"
          component={ChangeEmailScreen}
          options={{ title: '修改邮箱' }}
        />
        <RootStack.Screen
          name="ChangePassword"
          component={ChangePasswordScreen}
          options={{ title: '修改密码' }}
        />
      </RootStack.Navigator>
    </NavigationContainer>
  );
}

const styles = StyleSheet.create({
  loadingGate: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.bg,
  },
  loadingGateText: {
    marginTop: 12,
    color: colors.ink500,
    fontWeight: '600',
  },
  headerBackIcon: {
    marginLeft: 2,
  },
  tabIconOn: { opacity: 1 },
  tabIconDim: { opacity: 0.5 },
});
