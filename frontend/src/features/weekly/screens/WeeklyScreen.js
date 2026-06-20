import React, { useCallback, useEffect, useRef, useState } from 'react';
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
import Icon from 'react-native-vector-icons/Ionicons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useFocusEffect } from '@react-navigation/native';
import { useAuth } from '../../../app/providers/AuthContext';
import { weeklyAPI, streamWeeklyReport } from '../api';
import GIcon from '../../../shared/components/Icon';
import { colors, radius, spacing, shadows } from '../../../shared/theme';

const WeeklyScreen = ({ navigation }) => {
  const insets = useSafeAreaInsets();
  const { isAuthenticated } = useAuth();

  const [fragments, setFragments] = useState('');
  const [content, setContent] = useState('');
  const [generating, setGenerating] = useState(false);
  const [history, setHistory] = useState([]);
  const cancelRef = useRef(null);

  const loadHistory = useCallback(() => {
    if (!isAuthenticated) {
      setHistory([]);
      return;
    }
    weeklyAPI.list().then((r) => setHistory(r.data || [])).catch(() => {});
  }, [isAuthenticated]);

  useFocusEffect(useCallback(() => { loadHistory(); }, [loadHistory]));

  useEffect(() => () => { if (cancelRef.current) cancelRef.current(); }, []);

  const generate = () => {
    if (!isAuthenticated) {
      Alert.alert('登录后生成', 'AI 周报需要登录后使用。', [
        { text: '取消', style: 'cancel' },
        { text: '去登录', onPress: () => navigation.navigate('Login') },
      ]);
      return;
    }
    if (!fragments.trim()) {
      Alert.alert('提示', '先写几条本周的工作碎片吧');
      return;
    }
    setContent('');
    setGenerating(true);
    cancelRef.current = streamWeeklyReport(fragments, {
      onDelta: (d) => setContent((prev) => prev + d),
      onDone: () => { setGenerating(false); loadHistory(); },
      onError: (msg) => { setGenerating(false); Alert.alert('生成失败', msg || '请稍后再试'); },
    });
  };

  return (
    <KeyboardAvoidingView style={styles.container} behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
      <ScrollView
        contentContainerStyle={[styles.content, { paddingTop: insets.top + 12, paddingBottom: insets.bottom + 40 }]}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
      >
        <Text style={styles.pageTitle}>AI 周报</Text>
        <Text style={styles.subtitle}>把本周的零散成果丢进来，一键生成正式周报</Text>

        <TextInput
          style={styles.input}
          value={fragments}
          onChangeText={setFragments}
          placeholder={'例如：\n完成了登录模块\n修复了 3 个线上 bug\n对接了支付接口'}
          placeholderTextColor={colors.ink300}
          multiline
          textAlignVertical="top"
        />

        <TouchableOpacity
          style={[styles.genBtn, generating && styles.genBtnDisabled]}
          onPress={generate}
          disabled={generating}
          activeOpacity={0.85}
        >
          <Icon name={generating ? 'sparkles' : 'sparkles-outline'} size={18} color="#fff" />
          <Text style={styles.genBtnText}>{generating ? '生成中…' : '生成周报'}</Text>
        </TouchableOpacity>

        {content ? (
          <View style={styles.resultCard}>
            <Text style={styles.resultText} selectable>{content}</Text>
          </View>
        ) : null}

        {history.length > 0 && (
          <View style={styles.historySection}>
            <Text style={styles.historyTitle}>历史周报</Text>
            {history.map((r) => (
              <View key={r.id} style={styles.historyRow}>
                <GIcon name="doc" size={18} />
                <Text style={styles.historyWeek}>{r.weekKey || '周报'}</Text>
                <Text style={styles.historyPreview} numberOfLines={1}>
                  {(r.content || '').replace(/[#\n]/g, ' ').trim()}
                </Text>
              </View>
            ))}
          </View>
        )}

        {!isAuthenticated && (
          <Text style={styles.hint}>登录后即可生成并保存 AI 周报</Text>
        )}
      </ScrollView>
    </KeyboardAvoidingView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  content: { paddingHorizontal: spacing.md, gap: spacing.sm },
  pageTitle: { fontSize: 28, fontWeight: '800', color: colors.ink900, letterSpacing: -0.3 },
  subtitle: { fontSize: 13, color: colors.ink500, marginBottom: 4 },
  input: {
    minHeight: 130, backgroundColor: colors.bgElev, borderRadius: radius.lg, padding: 16,
    fontSize: 15, color: colors.ink900, borderWidth: 0.5, borderColor: colors.ink100, ...shadows.sm,
  },
  genBtn: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8, height: 50,
    borderRadius: radius.md, backgroundColor: colors.brand500, marginTop: 4, ...shadows.pop,
  },
  genBtnDisabled: { backgroundColor: colors.ink400 },
  genBtnText: { color: '#fff', fontSize: 16, fontWeight: '700' },
  resultCard: {
    backgroundColor: colors.bgElev, borderRadius: radius.lg, padding: 16, marginTop: 8,
    borderWidth: 0.5, borderColor: colors.ink100, ...shadows.sm,
  },
  resultText: { fontSize: 14.5, color: colors.ink900, lineHeight: 23 },
  historySection: { marginTop: 16, gap: 8 },
  historyTitle: {
    fontSize: 11, fontWeight: '700', color: colors.ink400, letterSpacing: 1,
    textTransform: 'uppercase', paddingHorizontal: 4,
  },
  historyRow: {
    flexDirection: 'row', alignItems: 'center', gap: 8, paddingHorizontal: 14, paddingVertical: 12,
    backgroundColor: colors.bgElev, borderRadius: radius.md, borderWidth: 0.5, borderColor: colors.ink100,
  },
  historyWeek: { fontSize: 13, fontWeight: '700', color: colors.ink900 },
  historyPreview: { flex: 1, fontSize: 12, color: colors.ink400 },
  hint: { fontSize: 12, color: colors.ink400, textAlign: 'center', marginTop: 8 },
});

export default WeeklyScreen;
