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
import { useAuth } from '../../../app/providers/AuthContext';
import { roleplayAPI, streamRoleplayReply } from '../api';
import { readOnboardingProfile, buildHatedPersona } from '../../onboarding/storage';
import { Mascot } from '../../../shared/components/Icon';
import { colors, radius, spacing, shadows } from '../../../shared/theme';

const RoleplayScreen = ({ navigation }) => {
  const insets = useSafeAreaInsets();
  const { isAuthenticated } = useAuth();

  const [personas, setPersonas] = useState([]);
  const [persona, setPersona] = useState(null);
  const [messages, setMessages] = useState([]); // {role, content}
  const [input, setInput] = useState('');
  const [streaming, setStreaming] = useState(false);
  const scrollRef = useRef(null);
  const cancelRef = useRef(null);

  useEffect(() => {
    Promise.all([
      roleplayAPI.personas().then((r) => r.data || []).catch(() => []),
      readOnboardingProfile().then(buildHatedPersona).catch(() => null),
    ]).then(([preset, hated]) => {
      const list = [...preset];
      if (hated) {
        // 把「我最讨厌的人」置顶为自定义角色
        list.unshift({
          code: 'custom',
          name: hated.nickname,
          emoji: '😤',
          description: hated.description,
          customPersona: hated.description,
        });
      }
      setPersonas(list);
    });
  }, []);

  useEffect(() => () => { if (cancelRef.current) cancelRef.current(); }, []);

  const scrollToEnd = useCallback(() => {
    requestAnimationFrame(() => scrollRef.current?.scrollToEnd({ animated: true }));
  }, []);

  const send = () => {
    const text = input.trim();
    if (!text || streaming) return;
    if (!isAuthenticated) {
      Alert.alert('登录后开喷', '和 AI 对线需要登录后使用。', [
        { text: '取消', style: 'cancel' },
        { text: '去登录', onPress: () => navigation.navigate('Login') },
      ]);
      return;
    }
    const userMsg = { role: 'user', content: text };
    const history = [...messages, userMsg];
    setMessages([...history, { role: 'assistant', content: '' }]);
    setInput('');
    setStreaming(true);
    scrollToEnd();

    cancelRef.current = streamRoleplayReply(persona.code, history, {
      customPersona: persona.customPersona,
      onDelta: (d) => {
        setMessages((prev) => {
          const next = [...prev];
          const last = next[next.length - 1];
          if (last && last.role === 'assistant') {
            next[next.length - 1] = { ...last, content: last.content + d };
          }
          return next;
        });
        scrollToEnd();
      },
      onDone: () => setStreaming(false),
      onError: (msg) => {
        setStreaming(false);
        setMessages((prev) => {
          const next = [...prev];
          const last = next[next.length - 1];
          if (last && last.role === 'assistant' && !last.content) {
            next[next.length - 1] = { ...last, content: `（${msg || '对方没有回应'}）` };
          }
          return next;
        });
      },
    });
  };

  // ── 角色选择 ──
  if (!persona) {
    return (
      <ScrollView
        style={styles.container}
        contentContainerStyle={[styles.pickerContent, { paddingTop: insets.top + 12, paddingBottom: insets.bottom + 40 }]}
        showsVerticalScrollIndicator={false}
      >
        <Text style={styles.pageTitle}>找 AI 对线</Text>
        <Text style={styles.subtitle}>选个对象，把今天的气撒出来 👇</Text>
        {personas.map((p) => (
          <TouchableOpacity
            key={p.code}
            style={styles.personaCard}
            onPress={() => { setMessages([]); setPersona(p); }}
            activeOpacity={0.85}
          >
            <Text style={styles.personaEmoji}>{p.emoji}</Text>
            <View style={styles.personaInfo}>
              <Text style={styles.personaName}>{p.name}</Text>
              <Text style={styles.personaDesc}>{p.description}</Text>
            </View>
            <Icon name="chevron-forward" size={18} color={colors.ink300} />
          </TouchableOpacity>
        ))}
      </ScrollView>
    );
  }

  // ── 聊天 ──
  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      keyboardVerticalOffset={0}
    >
      <View style={[styles.chatHeader, { paddingTop: insets.top + 8 }]}>
        <TouchableOpacity onPress={() => { if (cancelRef.current) cancelRef.current(); setPersona(null); }} hitSlop={10}>
          <Icon name="chevron-back" size={24} color={colors.ink900} />
        </TouchableOpacity>
        <Text style={styles.chatTitle}>{persona.emoji} {persona.name}</Text>
        <View style={styles.headerSpacer} />
      </View>

      <ScrollView
        ref={scrollRef}
        style={styles.chatList}
        contentContainerStyle={styles.chatListContent}
        onContentSizeChange={scrollToEnd}
      >
        {messages.length === 0 && (
          <View style={styles.emptyState}>
            <Mascot size={88} />
            <Text style={styles.chatHint}>对它说点什么，开始今天的对线…</Text>
          </View>
        )}
        {messages.map((m, i) => (
          <View key={i} style={[styles.bubbleRow, m.role === 'user' ? styles.rowRight : styles.rowLeft]}>
            <View style={[styles.bubble, m.role === 'user' ? styles.bubbleUser : styles.bubbleAi]}>
              <Text style={m.role === 'user' ? styles.bubbleUserText : styles.bubbleAiText}>
                {m.content || '…'}
              </Text>
            </View>
          </View>
        ))}
      </ScrollView>

      <View style={[styles.inputBar, { paddingBottom: insets.bottom + 8 }]}>
        <TextInput
          style={styles.inputField}
          value={input}
          onChangeText={setInput}
          placeholder="开喷…"
          placeholderTextColor={colors.ink300}
          multiline
          onSubmitEditing={send}
        />
        <TouchableOpacity
          style={[styles.sendBtn, (streaming || !input.trim()) && styles.sendBtnDisabled]}
          onPress={send}
          disabled={streaming || !input.trim()}
          activeOpacity={0.85}
        >
          <Icon name="arrow-up" size={20} color="#fff" />
        </TouchableOpacity>
      </View>
    </KeyboardAvoidingView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  pickerContent: { paddingHorizontal: spacing.md, gap: spacing.sm },
  pageTitle: { fontSize: 28, fontWeight: '800', color: colors.ink900, letterSpacing: -0.3 },
  subtitle: { fontSize: 13, color: colors.ink500, marginBottom: 8 },
  personaCard: {
    flexDirection: 'row', alignItems: 'center', gap: 14, padding: 16,
    backgroundColor: colors.bgElev, borderRadius: radius.lg,
    borderWidth: 0.5, borderColor: colors.ink100, ...shadows.sm,
  },
  personaEmoji: { fontSize: 30 },
  personaInfo: { flex: 1, gap: 3 },
  personaName: { fontSize: 16, fontWeight: '700', color: colors.ink900 },
  personaDesc: { fontSize: 12.5, color: colors.ink500 },

  chatHeader: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
    paddingHorizontal: spacing.md, paddingBottom: 10,
    borderBottomWidth: 0.5, borderBottomColor: colors.ink100, backgroundColor: colors.bgElev,
  },
  chatTitle: { fontSize: 16, fontWeight: '700', color: colors.ink900 },
  headerSpacer: { width: 24 },
  chatList: { flex: 1 },
  chatListContent: { padding: spacing.md, gap: 10 },
  emptyState: { alignItems: 'center', marginTop: 50, gap: 10 },
  chatHint: { textAlign: 'center', color: colors.ink400, fontSize: 13 },
  bubbleRow: { flexDirection: 'row' },
  rowLeft: { justifyContent: 'flex-start' },
  rowRight: { justifyContent: 'flex-end' },
  bubble: { maxWidth: '78%', paddingHorizontal: 14, paddingVertical: 10, borderRadius: 18 },
  bubbleAi: { backgroundColor: colors.bgElev, borderTopLeftRadius: 4, borderWidth: 0.5, borderColor: colors.ink100 },
  bubbleUser: { backgroundColor: colors.brand500, borderTopRightRadius: 4 },
  bubbleAiText: { fontSize: 15, color: colors.ink900, lineHeight: 21 },
  bubbleUserText: { fontSize: 15, color: '#fff', lineHeight: 21 },

  inputBar: {
    flexDirection: 'row', alignItems: 'flex-end', gap: 8,
    paddingHorizontal: spacing.md, paddingTop: 8,
    borderTopWidth: 0.5, borderTopColor: colors.ink100, backgroundColor: colors.bgElev,
  },
  inputField: {
    flex: 1, maxHeight: 120, minHeight: 40, borderRadius: radius.md, backgroundColor: colors.bg,
    borderWidth: 1, borderColor: colors.ink200, paddingHorizontal: 14, paddingTop: 10, paddingBottom: 10,
    fontSize: 15, color: colors.ink900,
  },
  sendBtn: {
    width: 40, height: 40, borderRadius: 20, backgroundColor: colors.brand500,
    alignItems: 'center', justifyContent: 'center',
  },
  sendBtnDisabled: { backgroundColor: colors.ink300 },
});

export default RoleplayScreen;
