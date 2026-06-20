import React, { useEffect, useState } from 'react';
import {
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useAuth } from '../../../app/providers/AuthContext';
import { readCheckinSettings, writeCheckinSettings } from '../../checkin/storage';
import { resetToRoute } from '../../../shared/utils';
import Select from '../components/Select';
import { INDUSTRIES, JOB_TYPES, GENDERS, HATED_RELATIONS } from '../constants';
import {
  readOnboardingProfile,
  writeOnboardingProfile,
  setOnboarded,
} from '../storage';
import { syncWorkProfileFromLocal } from '../sync';
import { colors, radius, spacing, shadows } from '../../../shared/theme';

const OnboardingScreen = ({ navigation, route }) => {
  const insets = useSafeAreaInsets();
  const { isAuthenticated } = useAuth();
  const editMode = route?.params?.edit;

  const [form, setForm] = useState(null);
  const [times, setTimes] = useState({ workStart: '09:00', workEnd: '18:00' });
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    Promise.all([readOnboardingProfile(), readCheckinSettings()]).then(([p, c]) => {
      setForm(p);
      setTimes({ workStart: c.workStart, workEnd: c.workEnd });
    });
  }, []);

  const set = (key) => (val) => setForm((f) => ({ ...f, [key]: val }));

  const finish = async () => {
    setSaving(true);
    await writeOnboardingProfile(form);
    const checkin = await readCheckinSettings();
    await writeCheckinSettings({ ...checkin, workStart: times.workStart, workEnd: times.workEnd });
    await setOnboarded();
    if (isAuthenticated) {
      await syncWorkProfileFromLocal(form);
    }
    setSaving(false);
    if (editMode) {
      navigation.goBack();
    } else {
      resetToRoute(navigation, 'HomeTabs');
    }
  };

  if (!form) return <View style={styles.container} />;

  return (
    <KeyboardAvoidingView style={styles.container} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView
        contentContainerStyle={[styles.content, { paddingTop: insets.top + 20, paddingBottom: insets.bottom + 40 }]}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
      >
        <Text style={styles.title}>{editMode ? '编辑我的信息' : '欢迎来到狗啊喂 👋'}</Text>
        <Text style={styles.subtitle}>填一点基础信息，帮你算薪资、匹配排行榜分组、定制 AI 对线</Text>

        <View style={styles.card}>
          <View style={styles.fieldRow}>
            <Text style={styles.label}>昵称</Text>
            <TextInput style={styles.input} value={form.nickname} onChangeText={set('nickname')} placeholder="榜单上的名字" placeholderTextColor={colors.ink300} maxLength={20} />
          </View>
          <Divider />
          <Select label="行业" value={form.industry} options={INDUSTRIES} onChange={set('industry')} />
          <Divider />
          <Select label="职业" value={form.jobType} options={JOB_TYPES} onChange={set('jobType')} />
          <Divider />
          <Select label="性别" value={form.gender} options={GENDERS} onChange={set('gender')} />
          <Divider />
          <View style={styles.fieldRow}>
            <Text style={styles.label}>城市</Text>
            <TextInput style={styles.input} value={form.city} onChangeText={set('city')} placeholder="如 北京" placeholderTextColor={colors.ink300} maxLength={20} />
          </View>
        </View>

        <Text style={styles.section}>工作时间</Text>
        <View style={styles.card}>
          <View style={styles.fieldRow}>
            <Text style={styles.label}>上班时间</Text>
            <TextInput style={styles.timeInput} value={times.workStart} onChangeText={(v) => setTimes((t) => ({ ...t, workStart: v }))} placeholder="09:00" placeholderTextColor={colors.ink300} />
          </View>
          <Divider />
          <View style={styles.fieldRow}>
            <Text style={styles.label}>下班时间</Text>
            <TextInput style={styles.timeInput} value={times.workEnd} onChangeText={(v) => setTimes((t) => ({ ...t, workEnd: v }))} placeholder="18:00" placeholderTextColor={colors.ink300} />
          </View>
        </View>

        <Text style={styles.section}>最讨厌的人（用于 AI 对线）</Text>
        <View style={styles.card}>
          <Select label="身份" value={form.hatedRelation} options={HATED_RELATIONS} placeholder="选一个" onChange={set('hatedRelation')} />
          <Divider />
          <View style={styles.fieldRow}>
            <Text style={styles.label}>称呼</Text>
            <TextInput style={styles.input} value={form.hatedNickname} onChangeText={set('hatedNickname')} placeholder="代号，可不填" placeholderTextColor={colors.ink300} maxLength={20} />
          </View>
          <Divider />
          <View style={styles.fieldCol}>
            <Text style={styles.label}>TA 的特征 / 口头禅</Text>
            <TextInput
              style={styles.textarea}
              value={form.hatedTraits}
              onChangeText={set('hatedTraits')}
              placeholder="如：阴阳怪气、爱甩锅，口头禅『这个很简单吧』"
              placeholderTextColor={colors.ink300}
              multiline
              maxLength={200}
            />
          </View>
        </View>

        <TouchableOpacity style={styles.btn} onPress={finish} disabled={saving} activeOpacity={0.85}>
          <Text style={styles.btnText}>{saving ? '保存中…' : editMode ? '保存' : '开始使用'}</Text>
        </TouchableOpacity>
        {!editMode && (
          <TouchableOpacity onPress={finish} disabled={saving} activeOpacity={0.7}>
            <Text style={styles.skip}>先跳过，之后在「我的」补填</Text>
          </TouchableOpacity>
        )}
      </ScrollView>
    </KeyboardAvoidingView>
  );
};

const Divider = () => <View style={styles.divider} />;

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  content: { paddingHorizontal: spacing.md, gap: spacing.sm },
  title: { fontSize: 26, fontWeight: '800', color: colors.ink900, letterSpacing: -0.3 },
  subtitle: { fontSize: 13.5, color: colors.ink500, lineHeight: 20, marginBottom: 8 },
  section: { fontSize: 12, fontWeight: '700', color: colors.ink400, letterSpacing: 1, textTransform: 'uppercase', paddingHorizontal: 4, marginTop: 8 },
  card: { backgroundColor: colors.bgElev, borderRadius: radius.lg, paddingHorizontal: 16, borderWidth: 0.5, borderColor: colors.ink100, ...shadows.sm },
  fieldRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingVertical: 12 },
  fieldCol: { paddingVertical: 12, gap: 8 },
  label: { fontSize: 15, color: colors.ink700, fontWeight: '500' },
  input: { flex: 1, marginLeft: 16, fontSize: 15, color: colors.ink900, textAlign: 'right' },
  timeInput: { width: 90, height: 38, borderRadius: radius.sm, borderWidth: 1, borderColor: colors.ink200, textAlign: 'center', color: colors.ink900, backgroundColor: colors.bg, fontSize: 15 },
  textarea: { minHeight: 64, borderRadius: radius.sm, borderWidth: 1, borderColor: colors.ink200, padding: 10, fontSize: 14.5, color: colors.ink900, backgroundColor: colors.bg, textAlignVertical: 'top' },
  divider: { height: 0.5, backgroundColor: colors.ink100 },
  btn: { height: 52, borderRadius: radius.md, backgroundColor: colors.brand500, alignItems: 'center', justifyContent: 'center', marginTop: 16, ...shadows.pop },
  btnText: { color: '#fff', fontSize: 16, fontWeight: '700' },
  skip: { fontSize: 13, color: colors.ink400, textAlign: 'center', paddingVertical: 14 },
});

export default OnboardingScreen;
