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
import { readThread, appendMessage, markRead, HATED_CODE } from '../imStore';
import { useUnread } from '../../../app/providers/UnreadContext';
import { Mascot } from '../../../shared/components/Icon';
import { colors, radius, spacing } from '../../../shared/theme';

const FALLBACK_HATED = {
  code: HATED_CODE,
  name: '最讨厌的人',
  emoji: '😤',
  customPersona: '我最讨厌的那个人：爱画饼、爱pua、阴阳怪气、张口闭口格局',
};

const ChatThreadScreen = ({ navigation, route }) => {
  const insets = useSafeAreaInsets();
  const { isAuthenticated } = useAuth();
  const { refreshUnread } = useUnread();
  const code = route.params?.code;

  const [persona, setPersona] = useState(null);
  const [messages, setMessages] = useState([]); // 持久化历史 + 当前流式态
  const [input, setInput] = useState('');
  const [streaming, setStreaming] = useState(false);
  const scrollRef = useRef(null);
  const cancelRef = useRef(null);
  const streamedRef = useRef('');

  // 解析联系人（预设按 code 取；custom 由「最讨厌的人」画像构建，未设置则用兜底）
  useEffect(() => {
    (async () => {
      if (code === HATED_CODE) {
        const profile = await readOnboardingProfile().catch(() => null);
        const hated = buildHatedPersona(profile);
        setPersona(hated
          ? { code: HATED_CODE, name: hated.nickname, emoji: '😤', customPersona: hated.description }
          : FALLBACK_HATED);
      } else {
        const preset = await roleplayAPI.personas().then((r) => r.data || []).catch(() => []);
        setPersona(preset.find((p) => p.code === code) || { code, name: '对线', emoji: '🤖' });
      }
    })();
  }, [code]);

  // 加载本地历史 + 标记已读
  useEffect(() => {
    if (!code) return;
    (async () => {
      const history = await readThread(code);
      setMessages(history);
      await markRead(code);
      refreshUnread();
    })();
  }, [code, refreshUnread]);

  useEffect(() => () => { if (cancelRef.current) cancelRef.current(); }, []);

  const scrollToEnd = useCallback(() => {
    requestAnimationFrame(() => scrollRef.current?.scrollToEnd({ animated: true }));
  }, []);

  const send = async () => {
    const text = input.trim();
    if (!text || streaming || !persona) return;
    if (!isAuthenticated) {
      Alert.alert('登录后开喷', '和 AI 对线需要登录后使用。', [
        { text: '取消', style: 'cancel' },
        { text: '去登录', onPress: () => navigation.navigate('Login') },
      ]);
      return;
    }

    const userMsg = await appendMessage(code, { role: 'user', content: text });
    const history = [...messages, userMsg].map((m) => ({ role: m.role, content: m.content }));
    setMessages((prev) => [...prev, userMsg, { id: 'streaming', role: 'assistant', content: '' }]);
    setInput('');
    setStreaming(true);
    streamedRef.current = '';
    scrollToEnd();

    cancelRef.current = streamRoleplayReply(persona.code, history, {
      customPersona: persona.customPersona,
      onDelta: (d) => {
        streamedRef.current += d;
        setMessages((prev) => {
          const next = [...prev];
          const last = next[next.length - 1];
          if (last && last.id === 'streaming') {
            next[next.length - 1] = { ...last, content: streamedRef.current };
          }
          return next;
        });
        scrollToEnd();
      },
      onDone: async () => {
        setStreaming(false);
        const content = streamedRef.current || '（对方没有回应）';
        const saved = await appendMessage(code, { role: 'assistant', content });
        setMessages((prev) => {
          const next = prev.filter((m) => m.id !== 'streaming');
          return [...next, saved];
        });
      },
      onError: (msg) => {
        setStreaming(false);
        setMessages((prev) => prev.map((m) =>
          m.id === 'streaming' ? { ...m, content: `（${msg || '对方没有回应'}）` } : m));
      },
    });
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <View style={[styles.chatHeader, { paddingTop: insets.top + 8 }]}>
        <TouchableOpacity onPress={() => { if (cancelRef.current) cancelRef.current(); navigation.goBack(); }} hitSlop={10}>
          <Icon name="chevron-back" size={24} color={colors.ink900} />
        </TouchableOpacity>
        <Text style={styles.chatTitle}>{persona ? `${persona.emoji} ${persona.name}` : ''}</Text>
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
        {messages.map((m) => (
          <View key={m.id} style={[styles.bubbleRow, m.role === 'user' ? styles.rowRight : styles.rowLeft]}>
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

export default ChatThreadScreen;
