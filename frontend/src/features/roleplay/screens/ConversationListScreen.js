import React, { useCallback, useState } from 'react';
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { roleplayAPI } from '../api';
import { readOnboardingProfile, buildHatedPersona } from '../../onboarding/storage';
import { readIndex, HATED_CODE } from '../imStore';
import { syncTauntInbox } from '../inboxSync';
import { useUnread } from '../../../app/providers/UnreadContext';
import { colors, radius, spacing, shadows } from '../../../shared/theme';

const formatTime = (ts) => {
  if (!ts) return '';
  const d = new Date(ts);
  const now = new Date();
  const sameDay = d.toDateString() === now.toDateString();
  const pad = (n) => String(n).padStart(2, '0');
  return sameDay ? `${pad(d.getHours())}:${pad(d.getMinutes())}` : `${d.getMonth() + 1}-${d.getDate()}`;
};

const ConversationListScreen = ({ navigation }) => {
  const insets = useSafeAreaInsets();
  const { refreshUnread } = useUnread();
  const [contacts, setContacts] = useState([]);
  const [index, setIndex] = useState({});

  const load = useCallback(async () => {
    // 先同步收件箱，再读联系人与本地索引，保证未读/预览最新
    await syncTauntInbox();
    const [preset, profile, idx] = await Promise.all([
      roleplayAPI.personas().then((r) => r.data || []).catch(() => []),
      readOnboardingProfile().catch(() => null),
      readIndex(),
    ]);
    const hated = buildHatedPersona(profile);
    const hatedContact = {
      code: HATED_CODE,
      name: hated?.nickname || '最讨厌的人',
      emoji: '😤',
      description: hated?.description || '点进去设置你最讨厌的人',
    };
    setContacts([hatedContact, ...preset]);
    setIndex(idx);
    refreshUnread();
  }, [refreshUnread]);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={[styles.content, { paddingTop: insets.top + 12, paddingBottom: insets.bottom + 40 }]}
      showsVerticalScrollIndicator={false}
    >
      <Text style={styles.pageTitle}>对线</Text>
      <Text style={styles.subtitle}>挑个对象开喷，TA 也会主动来撩你 👇</Text>

      {contacts.map((c) => {
        const meta = index[c.code] || {};
        const unread = meta.unread || 0;
        return (
          <TouchableOpacity
            key={c.code}
            style={styles.row}
            onPress={() => navigation.navigate('ChatThread', { code: c.code })}
            activeOpacity={0.85}
          >
            <View style={styles.avatar}>
              <Text style={styles.avatarEmoji}>{c.emoji}</Text>
              {unread > 0 && (
                <View style={styles.badge}>
                  <Text style={styles.badgeText}>{unread > 99 ? '99+' : unread}</Text>
                </View>
              )}
            </View>
            <View style={styles.rowBody}>
              <View style={styles.rowTop}>
                <Text style={styles.name} numberOfLines={1}>{c.name}</Text>
                <Text style={styles.time}>{formatTime(meta.lastTs)}</Text>
              </View>
              <Text style={[styles.preview, unread > 0 && styles.previewUnread]} numberOfLines={1}>
                {meta.lastText || c.description}
              </Text>
            </View>
          </TouchableOpacity>
        );
      })}
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  content: { paddingHorizontal: spacing.md, gap: spacing.sm },
  pageTitle: { fontSize: 28, fontWeight: '800', color: colors.ink900, letterSpacing: -0.3 },
  subtitle: { fontSize: 13, color: colors.ink500, marginBottom: 8 },
  row: {
    flexDirection: 'row', alignItems: 'center', gap: 14, padding: 14,
    backgroundColor: colors.bgElev, borderRadius: radius.lg,
    borderWidth: 0.5, borderColor: colors.ink100, ...shadows.sm,
  },
  avatar: {
    width: 48, height: 48, borderRadius: 24, backgroundColor: colors.bgSoft,
    alignItems: 'center', justifyContent: 'center',
  },
  avatarEmoji: { fontSize: 26 },
  badge: {
    position: 'absolute', top: -4, right: -4, minWidth: 18, height: 18, paddingHorizontal: 4,
    borderRadius: 9, backgroundColor: colors.danger, alignItems: 'center', justifyContent: 'center',
    borderWidth: 1.5, borderColor: colors.bgElev,
  },
  badgeText: { color: '#fff', fontSize: 10, fontWeight: '700' },
  rowBody: { flex: 1, gap: 4 },
  rowTop: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  name: { flex: 1, fontSize: 16, fontWeight: '700', color: colors.ink900 },
  time: { fontSize: 11, color: colors.ink400, marginLeft: 8 },
  preview: { fontSize: 13, color: colors.ink500 },
  previewUnread: { color: colors.ink700, fontWeight: '600' },
});

export default ConversationListScreen;
