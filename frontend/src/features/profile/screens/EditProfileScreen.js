import React, { useMemo, useRef, useState } from 'react';
import {
  Alert,
  Keyboard,
  KeyboardAvoidingView,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  TouchableWithoutFeedback,
  View,
  Platform,
} from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';
import { useHeaderHeight } from '@react-navigation/elements';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useAuth } from '../../../app/providers/AuthContext';
import { userAPI } from '../../../services/api';
import { getRequestErrorMessage } from '../../../utils/apiError';
import { colors, radius, shadows, spacing } from '../../../shared/theme';

const GENDER_OPTIONS = ['保密', '男', '女', '其他'];

const formatDateInput = (value) => value.replace(/[^\d-]/g, '').slice(0, 10);

const EditProfileScreen = ({ navigation, route }) => {
  const { user: authUser, updateUser } = useAuth();
  const initialUser = route?.params?.user || authUser || {};
  const [displayName, setDisplayName] = useState(initialUser.displayName || initialUser.username || '');
  const [bio, setBio] = useState(initialUser.bio || '');
  const [gender, setGender] = useState(initialUser.gender || '保密');
  const [birthday, setBirthday] = useState(initialUser.birthday || '');
  const [region, setRegion] = useState(initialUser.region || '');
  const [saving, setSaving] = useState(false);
  const bioInputRef = useRef(null);
  const headerHeight = useHeaderHeight();
  const insets = useSafeAreaInsets();

  const userId = initialUser.id;

  const payload = useMemo(
    () => ({
      displayName: displayName.trim(),
      bio: bio.trim(),
      gender,
      birthday: birthday.trim() || null,
      region: region.trim(),
    }),
    [bio, birthday, displayName, gender, region]
  );

  const handleSave = async () => {
    if (!userId) {
      Alert.alert('保存失败', '无法识别用户信息');
      return;
    }

    if (!payload.displayName) {
      Alert.alert('保存失败', '名字不能为空');
      return;
    }

    if (payload.birthday && !/^\d{4}-\d{2}-\d{2}$/.test(payload.birthday)) {
      Alert.alert('保存失败', '生日格式应为 YYYY-MM-DD');
      return;
    }

    setSaving(true);
    try {
      const response = await userAPI.updateProfile(userId, payload);
      await updateUser(response.data);
      navigation.goBack();
    } catch (error) {
      console.error('Failed to update profile:', error);
      Alert.alert('保存失败', getRequestErrorMessage(error, '保存失败，请稍后重试'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      keyboardVerticalOffset={Platform.OS === 'ios' ? headerHeight : 0}
    >
      <TouchableWithoutFeedback onPress={Keyboard.dismiss} accessible={false}>
        <ScrollView
          style={styles.container}
          contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 24 }]}
          keyboardShouldPersistTaps="handled"
          keyboardDismissMode="interactive"
          contentInsetAdjustmentBehavior="always"
        >
          <View style={styles.profileCard}>
            <View style={styles.avatar}>
              <Text style={styles.avatarText}>
                {(displayName || initialUser.username || '?').slice(0, 1).toUpperCase()}
              </Text>
            </View>
            <View style={styles.profileCopy}>
              <Text style={styles.profileName}>{displayName || '未命名用户'}</Text>
              <Text style={styles.profileMeta}>@{initialUser.username || 'whattoeat'}</Text>
            </View>
          </View>

          <View style={styles.formCard}>
            <FormField label="名字">
              <TextInput
                style={styles.input}
                placeholder="输入展示给别人的名字"
                placeholderTextColor={colors.ink300}
                value={displayName}
                onChangeText={setDisplayName}
              />
            </FormField>

            <Divider />

            <FormField label="一句话自我介绍">
              <TextInput
                ref={bioInputRef}
                style={[styles.input, styles.multilineInput]}
                placeholder="介绍一下自己"
                placeholderTextColor={colors.ink300}
                value={bio}
                onChangeText={setBio}
                multiline
                autoCorrect={false}
                scrollEnabled
              />
            </FormField>
          </View>

          <View style={styles.formCard}>
            <FormField label="性别">
              <View style={styles.optionRow}>
                {GENDER_OPTIONS.map((option) => (
                  <TouchableOpacity
                    key={option}
                    style={[styles.optionButton, gender === option && styles.optionButtonActive]}
                    onPress={() => setGender(option)}
                    activeOpacity={0.8}
                  >
                    <Text style={[styles.optionText, gender === option && styles.optionTextActive]}>
                      {option}
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>
            </FormField>

            <Divider />

            <FormField label="生日">
              <TextInput
                style={styles.input}
                placeholder="YYYY-MM-DD"
                placeholderTextColor={colors.ink300}
                value={birthday}
                onChangeText={(text) => setBirthday(formatDateInput(text))}
                keyboardType="numbers-and-punctuation"
              />
            </FormField>

            <Divider />

            <FormField label="地区">
              <TextInput
                style={styles.input}
                placeholder="例如：北京 / 上海 / 杭州"
                placeholderTextColor={colors.ink300}
                value={region}
                onChangeText={setRegion}
              />
            </FormField>
          </View>

          <TouchableOpacity
            style={[styles.saveButton, saving && styles.saveButtonDisabled]}
            disabled={saving}
            onPress={handleSave}
            activeOpacity={0.86}
          >
            <Icon name="checkmark-circle-outline" size={18} color="#fff" />
            <Text style={styles.saveButtonText}>{saving ? '保存中...' : '保存资料'}</Text>
          </TouchableOpacity>
        </ScrollView>
      </TouchableWithoutFeedback>
    </KeyboardAvoidingView>
  );
};

const FormField = ({ label, children }) => (
  <View style={styles.field}>
    <Text style={styles.label}>{label}</Text>
    {children}
  </View>
);

const Divider = () => <View style={styles.divider} />;

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.bg,
  },
  content: {
    paddingHorizontal: spacing.md,
    paddingTop: spacing.md,
    gap: spacing.md,
  },
  profileCard: {
    backgroundColor: colors.bgElev,
    borderRadius: radius.lg,
    borderWidth: 0.5,
    borderColor: colors.ink100,
    padding: spacing.md,
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    ...shadows.sm,
  },
  avatar: {
    width: 58,
    height: 58,
    borderRadius: 29,
    backgroundColor: colors.brand500,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarText: {
    color: '#fff',
    fontSize: 22,
    fontWeight: '800',
  },
  profileCopy: {
    flex: 1,
    gap: 3,
  },
  profileName: {
    fontSize: 18,
    fontWeight: '800',
    color: colors.ink900,
  },
  profileMeta: {
    fontSize: 13,
    color: colors.ink500,
  },
  formCard: {
    backgroundColor: colors.bgElev,
    borderRadius: radius.md,
    borderWidth: 0.5,
    borderColor: colors.ink100,
    overflow: 'hidden',
    ...shadows.sm,
  },
  field: {
    paddingHorizontal: spacing.md,
    paddingVertical: 14,
  },
  label: {
    fontSize: 12,
    fontWeight: '700',
    color: colors.ink400,
    marginBottom: 7,
  },
  input: {
    backgroundColor: colors.bgSoft,
    borderRadius: radius.sm,
    borderWidth: 0.5,
    borderColor: colors.ink100,
    paddingHorizontal: 13,
    paddingVertical: 11,
    fontSize: 16,
    color: colors.ink900,
  },
  multilineInput: {
    minHeight: 110,
    textAlignVertical: 'top',
  },
  optionRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
  },
  optionButton: {
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderRadius: radius.pill,
    backgroundColor: colors.bgSoft,
    borderWidth: 0.5,
    borderColor: colors.ink100,
  },
  optionButtonActive: {
    backgroundColor: colors.brand500,
    borderColor: colors.brand500,
  },
  optionText: {
    color: colors.ink700,
    fontSize: 14,
    fontWeight: '700',
  },
  optionTextActive: {
    color: '#fff',
  },
  divider: {
    height: 0.5,
    backgroundColor: colors.ink100,
    marginLeft: spacing.md,
  },
  saveButton: {
    backgroundColor: colors.brand500,
    borderRadius: radius.md,
    paddingVertical: 15,
    alignItems: 'center',
    justifyContent: 'center',
    flexDirection: 'row',
    gap: 8,
    ...shadows.pop,
  },
  saveButtonDisabled: {
    backgroundColor: colors.ink300,
    shadowOpacity: 0,
  },
  saveButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '800',
  },
});

export default EditProfileScreen;
