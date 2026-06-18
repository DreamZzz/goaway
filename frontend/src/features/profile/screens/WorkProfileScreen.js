import React, { useEffect, useState } from 'react';
import {
  Alert,
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
import { workProfileAPI } from '../workProfileApi';
import { colors, radius, spacing, shadows } from '../../../shared/theme';

const FIELDS = [
  { key: 'nickname', label: '昵称', placeholder: '榜单上展示的名字', max: 30 },
  { key: 'city', label: '城市', placeholder: '如 北京', max: 40 },
  { key: 'industry', label: '行业', placeholder: '如 互联网', max: 40 },
  { key: 'jobType', label: '工种', placeholder: '如 后端 / 产品 / 运营', max: 40 },
];

const WorkProfileScreen = ({ navigation }) => {
  const insets = useSafeAreaInsets();
  const [form, setForm] = useState({ nickname: '', city: '', industry: '', jobType: '' });
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    workProfileAPI.get()
      .then((r) => setForm({
        nickname: r.data?.nickname || '',
        city: r.data?.city || '',
        industry: r.data?.industry || '',
        jobType: r.data?.jobType || '',
      }))
      .catch(() => {});
  }, []);

  const save = async () => {
    try {
      setSaving(true);
      await workProfileAPI.update(form);
      Alert.alert('已保存', '画像已更新，可在排行榜按分组查看。', [
        { text: '好', onPress: () => navigation.goBack() },
      ]);
    } catch {
      Alert.alert('保存失败', '请稍后再试');
    } finally {
      setSaving(false);
    }
  };

  return (
    <KeyboardAvoidingView style={styles.container} behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
      <ScrollView
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 32 }]}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
      >
        <Text style={styles.intro}>
          匿名画像用于排行榜分组（同城 / 同行 / 同工种），不会展示你的真实身份。
        </Text>
        <View style={styles.card}>
          {FIELDS.map((f, i) => (
            <View key={f.key} style={[styles.row, i > 0 && styles.rowBorder]}>
              <Text style={styles.label}>{f.label}</Text>
              <TextInput
                style={styles.input}
                value={form[f.key]}
                onChangeText={(v) => setForm({ ...form, [f.key]: v })}
                placeholder={f.placeholder}
                placeholderTextColor={colors.ink300}
                maxLength={f.max}
              />
            </View>
          ))}
        </View>
        <TouchableOpacity style={styles.saveBtn} onPress={save} disabled={saving} activeOpacity={0.85}>
          <Text style={styles.saveBtnText}>{saving ? '保存中…' : '保存画像'}</Text>
        </TouchableOpacity>
      </ScrollView>
    </KeyboardAvoidingView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  content: { paddingHorizontal: spacing.md, paddingTop: spacing.md, gap: spacing.md },
  intro: { fontSize: 13, color: colors.ink500, lineHeight: 20 },
  card: {
    backgroundColor: colors.bgElev, borderRadius: radius.lg, overflow: 'hidden',
    borderWidth: 0.5, borderColor: colors.ink100, ...shadows.sm,
  },
  row: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 16, paddingVertical: 14 },
  rowBorder: { borderTopWidth: 0.5, borderTopColor: colors.ink100 },
  label: { width: 64, fontSize: 15, fontWeight: '600', color: colors.ink900 },
  input: { flex: 1, fontSize: 15, color: colors.ink900, textAlign: 'right' },
  saveBtn: {
    height: 50, borderRadius: radius.md, backgroundColor: colors.brand500,
    alignItems: 'center', justifyContent: 'center', ...shadows.pop,
  },
  saveBtnText: { color: '#fff', fontSize: 16, fontWeight: '700' },
});

export default WorkProfileScreen;
