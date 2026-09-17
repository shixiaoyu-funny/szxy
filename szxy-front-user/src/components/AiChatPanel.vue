<script setup lang="ts">
import { ref, computed, nextTick, onMounted, onUnmounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useUserStore } from '../stores/user';
import ChatMessage from './chat/ChatMessage.vue';
import ChatInput from './chat/ChatInput.vue';
import {
  newSession,
  getSessionsByUid,
  getChatsBySid,
  sendChat,
  sendChatAnonymous,
  renameSession,
  type ChatMessageVO,
  type ChatSessionVO,
} from '../api/ai';
import { getErrorMessage } from '../api/axios';

interface Msg {
  role: 'user' | 'assistant';
  content: string;
  mediaUrls?: string[];
  time?: string;
}

const router = useRouter();
const userStore = useUserStore();

const sessionId = ref<number | null>(null);
const sessions = ref<ChatSessionVO[]>([]);
const messages = ref<Msg[]>([]);
const sending = ref(false);
const initializing = ref(false);
const container = ref<HTMLElement>();
const showScrollBtn = ref(false);

const currentSessionTitle = computed(() => {
  const current = sessions.value.find((s) => s.id === sessionId.value);
  return current?.simpleDesc?.trim() || '新对话';
});

const roleOf = (role: ChatMessageVO['role']): 'user' | 'assistant' => {
  const code = typeof role === 'number' ? role : role?.code;
  return code === 1 ? 'user' : 'assistant';
};

const scrollBottom = async () => {
  await nextTick();
  if (container.value) {
    container.value.scrollTo({ top: container.value.scrollHeight, behavior: 'smooth' });
    showScrollBtn.value = false;
  }
};

const onScroll = () => {
  if (!container.value) return;
  const { scrollTop, scrollHeight, clientHeight } = container.value;
  showScrollBtn.value = scrollHeight - scrollTop - clientHeight > 80;
};

const mapMessages = (list: ChatMessageVO[]): Msg[] =>
  list.map((m) => ({
    role: roleOf(m.role),
    content: m.content,
    mediaUrls: m.mediaUrls || undefined,
    time: m.createTime,
  }));

const loadMessages = async (sid: number) => {
  const res = await getChatsBySid(sid);
  messages.value = mapMessages((res as { data?: ChatMessageVO[] }).data ?? []);
  await scrollBottom();
};

const refreshSessions = async () => {
  const res = await getSessionsByUid();
  sessions.value = (res as { data?: ChatSessionVO[] }).data ?? [];
};

const pickSession = (list: ChatSessionVO[]): ChatSessionVO | undefined => {
  return list.find((s) => s.simpleDesc === '新对话') ?? list[0];
};

const initChat = async () => {
  if (!userStore.isLoggedIn) return;
  initializing.value = true;
  try {
    await refreshSessions();
    let target = pickSession(sessions.value);
    if (!target) {
      const created = await newSession();
      sessionId.value = (created as { data: number }).data;
      messages.value = [];
      await refreshSessions();
      return;
    }
    sessionId.value = target.id;
    await loadMessages(target.id);
  } catch (err) {
    console.error('初始化 AI 对话失败:', err);
    const msg = getErrorMessage(err);
    if (msg) ElMessage.error(msg);
  } finally {
    initializing.value = false;
  }
};

const switchSession = async (sid: number) => {
  if (sessionId.value === sid || sending.value) return;
  sessionId.value = sid;
  messages.value = [];
  try {
    await loadMessages(sid);
  } catch (err) {
    const msg = getErrorMessage(err);
    if (msg) ElMessage.error(msg);
  }
};

const handleNewChat = async () => {
  if (!userStore.isLoggedIn) {
    requireLogin();
    return;
  }
  try {
    const res = await newSession();
    sessionId.value = (res as { data: number }).data;
    messages.value = [];
    await refreshSessions();
    ElMessage.success('已创建新对话');
  } catch (err: unknown) {
    const msg = getErrorMessage(err);
    if (msg) ElMessage.error(msg);
    await refreshSessions();
    const existing = sessions.value.find((s) => s.simpleDesc === '新对话');
    if (existing) {
      sessionId.value = existing.id;
      await loadMessages(existing.id);
    }
  }
};

const requireLogin = () => {
  ElMessage.info('登录注册后才能体验完整功能哦');
  router.push('/login');
};

const buildAnonymousHistory = () => {
  // 去掉刚 push 进列表的当前用户消息，取最近 20 条作为上下文
  const recent = messages.value.slice(0, -1).slice(-20);
  return recent.map((m) => ({
    role: m.role === 'user' ? 1 : 2,
    content: m.content || '',
    mediaUrls: m.mediaUrls || undefined,
  }));
};

const handleSend = async (payload: { text: string; mediaUrls: string[] }) => {
  if (sending.value) return;
  const text = payload.text?.trim() || '';
  const mediaUrls = payload.mediaUrls || [];
  if (!text && !mediaUrls.length) return;

  const now = new Date().toISOString();
  messages.value.push({
    role: 'user',
    content: text || (mediaUrls.length ? '（图片）' : ''),
    mediaUrls: mediaUrls.length ? mediaUrls : undefined,
    time: now,
  });
  await scrollBottom();
  sending.value = true;

  try {
    if (!userStore.isLoggedIn) {
      const res = await sendChatAnonymous({ content: text, mediaUrls, history: buildAnonymousHistory() });
      const reply = (res as { data?: string }).data ?? '';
      messages.value.push({ role: 'assistant', content: reply, time: new Date().toISOString() });
      await scrollBottom();
      return;
    }
    if (!sessionId.value) return;
    const res = await sendChat(sessionId.value, { content: text, mediaUrls });
    const reply = (res as { data?: string }).data ?? '';
    messages.value.push({ role: 'assistant', content: reply, time: new Date().toISOString() });
    await scrollBottom();
    await refreshSessions();
  } catch (err) {
    console.error('发送消息失败:', err);
    const msg = getErrorMessage(err);
    if (msg) {
      messages.value.push({
        role: 'assistant',
        content: msg,
        time: new Date().toISOString(),
      });
      ElMessage.error(msg);
    }
  } finally {
    sending.value = false;
  }
};

const formatSessionTime = (time?: string) => {
  if (!time) return '';
  const d = new Date(time);
  if (Number.isNaN(d.getTime())) return '';
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

const renaming = ref(false);

/** Ctrl/Cmd + R：重命名当前选中会话（拦截浏览器刷新） */
const openRenameDialog = async () => {
  if (!userStore.isLoggedIn || renaming.value || sending.value) return;
  const sid = sessionId.value;
  if (sid == null) {
    ElMessage.warning('请先选中一个会话');
    return;
  }
  const current = sessions.value.find((s) => s.id === sid);
  const oldName = current?.simpleDesc?.trim() || '新对话';
  try {
    const { value } = await ElMessageBox.prompt('请输入新的会话名称', '修改会话名称', {
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      inputValue: oldName,
      inputPlaceholder: '最多 64 个字符',
      inputPattern: /\S+/,
      inputErrorMessage: '会话名称不能为空',
      customClass: 'session-rename-box',
    });
    const name = String(value ?? '').trim();
    if (!name) {
      ElMessage.warning('会话名称不能为空');
      return;
    }
    if (name.length > 64) {
      ElMessage.warning('会话名称最多 64 个字符');
      return;
    }
    if (name === oldName) return;
    renaming.value = true;
    await renameSession(sid, name);
    const target = sessions.value.find((s) => s.id === sid);
    if (target) target.simpleDesc = name;
    ElMessage.success('已修改会话名称');
    await refreshSessions();
  } catch (err) {
    if (err === 'cancel' || err === 'close') return;
    const msg = getErrorMessage(err);
    if (msg) ElMessage.error(msg);
  } finally {
    renaming.value = false;
  }
};

const onKeydownRename = (e: KeyboardEvent) => {
  if (!(e.ctrlKey || e.metaKey)) return;
  if (e.key !== 'r' && e.key !== 'R') return;
  if (!userStore.isLoggedIn || sessionId.value == null) return;
  e.preventDefault();
  void openRenameDialog();
};

const goLogin = () => router.push('/login');

watch(
  () => userStore.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) initChat();
    else {
      sessionId.value = null;
      sessions.value = [];
      messages.value = [];
    }
  }
);

onMounted(() => {
  if (userStore.isLoggedIn) initChat();
  window.addEventListener('keydown', onKeydownRename);
});

onUnmounted(() => {
  window.removeEventListener('keydown', onKeydownRename);
});
</script>

<template>
  <section class="ai-chat-panel">
    <div class="ai-chat-layout">
      <div class="ai-chat-main">
        <header class="ai-chat-header">
          <h2 class="session-title" :title="currentSessionTitle">
            {{ userStore.isLoggedIn ? currentSessionTitle : '禾小智 · 游客模式' }}
          </h2>
        </header>

        <div ref="container" class="ai-chat-messages" @scroll="onScroll">
          <div v-if="initializing" class="ai-chat-placeholder">
            <div class="mini-spinner" />
            <span>正在连接禾小智…</span>
          </div>
          <div v-else-if="!messages.length" class="ai-chat-placeholder">
            <p class="welcome-title">你好，我是禾小智</p>
            <p class="welcome-desc">可以问我景点推荐、乡村游玩攻略、当季特色活动等问题</p>
          </div>
          <ChatMessage
            v-for="(m, i) in messages"
            :key="i"
            :role="m.role"
            :content="m.content"
            :media-urls="m.mediaUrls"
            :time="m.time"
          />
          <div v-if="sending" class="typing-indicator">
            <span /><span /><span />
          </div>
        </div>

        <div class="ai-chat-footer">
          <button v-show="showScrollBtn" type="button" class="scroll-btn" @click="scrollBottom">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="6 9 12 15 18 9" />
            </svg>
          </button>
          <ChatInput :disabled="initializing || sending || (userStore.isLoggedIn && !sessionId)" @send="handleSend" />
        </div>
      </div>

      <aside v-if="userStore.isLoggedIn" class="ai-chat-sidebar">
        <div class="sidebar-header">
          <span class="sidebar-title">历史会话</span>
          <button
            type="button"
            class="new-chat-btn"
            :disabled="initializing || sending"
            title="新对话"
            @click="handleNewChat"
          >
            +
          </button>
        </div>
        <div v-if="!sessions.length" class="sidebar-empty">暂无历史会话</div>
        <ul v-else class="session-list">
          <li
            v-for="s in sessions"
            :key="s.id"
            class="session-item"
            :class="{ active: s.id === sessionId }"
            :title="'点击切换 · Ctrl+R 重命名'"
            @click="switchSession(s.id)"
          >
            <p class="session-desc">{{ s.simpleDesc || `会话 ${s.id}` }}</p>
            <p v-if="s.updateTime || s.createTime" class="session-time">
              {{ formatSessionTime(s.updateTime || s.createTime) }}
            </p>
          </li>
        </ul>
      </aside>

      <aside v-else class="ai-chat-sidebar ai-chat-sidebar-anon">
        <div class="sidebar-header">
          <span class="sidebar-title">历史会话</span>
          <button type="button" class="new-chat-btn" title="新对话" @click="requireLogin">+</button>
        </div>
        <div class="anon-hint">
          <p>登录后即可保存对话、创建新对话并浏览历史记录</p>
          <button type="button" class="login-btn" @click="goLogin">登录 / 注册</button>
        </div>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.ai-chat-panel {
  margin: 0 16px 16px;
  aspect-ratio: 4 / 3;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.ai-chat-layout {
  display: flex;
  height: 100%;
}

.ai-chat-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #eef0f3;
}

.ai-chat-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 14px 20px;
  border-bottom: 1px solid #eef0f3;
  background: #fafbfc;
}

.session-title {
  margin: 0;
  max-width: 100%;
  font-size: 16px;
  font-weight: 600;
  color: #2e7d32;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-chat-sidebar {
  width: 220px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #f8faf9;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 12px;
  border-bottom: 1px solid #eef0f3;
}

.sidebar-title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
}

.new-chat-btn {
  width: 28px;
  height: 28px;
  border: 1px solid #8bc34a;
  border-radius: 8px;
  background: #f0f9e8;
  color: #558b2f;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  transition: all 0.2s;
}

.new-chat-btn:hover:not(:disabled) {
  background: #8bc34a;
  color: #fff;
}

.new-chat-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.sidebar-empty {
  padding: 24px 12px;
  text-align: center;
  font-size: 13px;
  color: #909399;
}

.session-list {
  list-style: none;
  margin: 0;
  padding: 8px;
  overflow-y: auto;
  flex: 1;
}

.session-list::-webkit-scrollbar {
  width: 4px;
}

.session-list::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 4px;
}

.session-item {
  padding: 10px 12px;
  margin-bottom: 6px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
  border: 1px solid transparent;
}

.session-item:hover {
  background: rgba(139, 195, 74, 0.1);
}

.session-item.active {
  background: rgba(139, 195, 74, 0.18);
  border-color: rgba(139, 195, 74, 0.35);
}

.session-desc {
  margin: 0;
  font-size: 13px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-time {
  margin: 4px 0 0;
  font-size: 11px;
  color: #909399;
}

.ai-chat-sidebar-anon {
  align-items: center;
  justify-content: center;
}

.anon-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 24px 20px;
  text-align: center;
  flex: 1;
  color: #909399;
  font-size: 13px;
  line-height: 1.6;
}

.anon-hint p {
  margin: 0;
}

.login-btn {
  height: 36px;
  padding: 0 20px;
  border: none;
  border-radius: 18px;
  background: linear-gradient(135deg, #8bc34a 0%, #66bb6a 100%);
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}

.ai-chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  scroll-behavior: smooth;
}

.ai-chat-messages::-webkit-scrollbar {
  width: 4px;
}

.ai-chat-messages::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 4px;
}

.ai-chat-placeholder {
  height: 100%;
  min-height: 180px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #909399;
  font-size: 14px;
}

.welcome-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #558b2f;
}

.welcome-desc {
  margin: 0;
  font-size: 13px;
  text-align: center;
  max-width: 320px;
  line-height: 1.6;
}

.mini-spinner {
  width: 28px;
  height: 28px;
  border: 3px solid rgba(139, 195, 74, 0.25);
  border-top-color: #8bc34a;
  border-radius: 50%;
  animation: spin 0.85s linear infinite;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 4px 0 8px 4px;
}

.typing-indicator span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #8bc34a;
  animation: bounce 1.2s infinite ease-in-out;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.15s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.3s;
}

.ai-chat-footer {
  position: relative;
  flex-shrink: 0;
}

.scroll-btn {
  position: absolute;
  right: 20px;
  top: -44px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid #e4e7ed;
  background: #fff;
  color: #606266;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  z-index: 2;
  transition: transform 0.15s;
}

.scroll-btn:hover {
  transform: scale(1.05);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes bounce {
  0%,
  80%,
  100% {
    transform: scale(0.6);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

@media (max-width: 768px) {
  .ai-chat-layout {
    flex-direction: column;
  }

  .ai-chat-main {
    border-right: none;
    border-bottom: 1px solid #eef0f3;
    flex: 1;
    min-height: 0;
  }

  .ai-chat-sidebar {
    width: 100%;
    height: 140px;
    flex-shrink: 0;
  }

  .session-list {
    display: flex;
    gap: 8px;
    overflow-x: auto;
    overflow-y: hidden;
    padding: 8px 12px;
  }

  .session-item {
    min-width: 140px;
    margin-bottom: 0;
    flex-shrink: 0;
  }
}
</style>
