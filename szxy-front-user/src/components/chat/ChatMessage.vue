<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { MarkdownUI } from '@markdown-ui/vue';
import '@markdown-ui/vue/widgets.css';
import { parseCartProductId, renderMarkdownUi } from '../../utils/renderMarkdownUi';
import { getErrorMessage } from '../../api/axios';

const props = defineProps<{
  role: 'user' | 'assistant';
  content: string;
  mediaUrls?: string[];
  time?: string;
}>();

const router = useRouter();
const buyBusy = ref(false);

const assistantHtml = computed(() =>
  props.role === 'assistant' ? renderMarkdownUi(props.content) : ''
);

const images = computed(() => (props.mediaUrls || []).filter(Boolean));

function formatTime(iso?: string): string {
  if (!iso) return '';
  const d = new Date(iso);
  const now = new Date();
  const t = d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  if (d.toDateString() === now.toDateString()) return t;
  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  if (d.toDateString() === yesterday.toDateString()) return `昨天 ${t}`;
  return `${d.getMonth() + 1}月${d.getDate()}日 ${t}`;
}

const onWidgetEvent = async (event: CustomEvent<{ id: string; value: unknown }>) => {
  const detail = event?.detail;
  const productId = parseCartProductId(detail?.id);
  if (productId == null) return;
  if (buyBusy.value) return;
  buyBusy.value = true;
  try {
    await router.push({
      path: '/order/confirm',
      query: { productId: String(productId), quantity: '1' },
    });
  } catch (e) {
    ElMessage.error(getErrorMessage(e) || '跳转下单失败');
  } finally {
    buyBusy.value = false;
  }
};
</script>

<template>
  <div class="msg" :class="role">
    <div class="msg-bubble">
      <div v-if="images.length" class="msg-images" :class="{ 'is-user': role === 'user' }">
        <a
          v-for="(url, i) in images"
          :key="url + i"
          :href="url"
          target="_blank"
          rel="noopener noreferrer"
          class="msg-image-link"
        >
          <img :src="url" alt="聊天图片" loading="lazy" />
        </a>
      </div>
      <div v-if="role === 'assistant'" class="msg-text markdown-body">
        <MarkdownUI :html="assistantHtml" @widget-event="onWidgetEvent" />
      </div>
      <span v-else-if="content" class="msg-text">{{ content }}</span>
      <span v-if="time" class="msg-time">{{ formatTime(time) }}</span>
    </div>
  </div>
</template>

<style scoped>
.msg {
  display: flex;
  margin-bottom: 16px;
  animation: fadeUp 0.35s ease both;
}

.msg.user {
  justify-content: flex-end;
}

.msg-bubble {
  max-width: 78%;
  padding: 10px 14px 8px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.user .msg-bubble {
  background: linear-gradient(135deg, #8bc34a 0%, #66bb6a 100%);
  color: #fff;
  border-bottom-right-radius: 4px;
  white-space: pre-wrap;
}

.assistant .msg-bubble {
  background: #f5f7fa;
  color: #333;
  border-bottom-left-radius: 4px;
}

.msg-images {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.msg-image-link {
  display: block;
  width: 120px;
  max-width: 40vw;
  border-radius: 8px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.2);
}

.msg-images:not(.is-user) .msg-image-link {
  background: #eef2f6;
}

.msg-image-link img {
  width: 100%;
  height: auto;
  max-height: 180px;
  object-fit: cover;
  display: block;
  vertical-align: middle;
}

.msg-text {
  display: block;
}

.msg-time {
  display: block;
  text-align: right;
  font-size: 11px;
  margin-top: 6px;
  opacity: 0.55;
}

.markdown-body :deep(p) {
  margin: 0 0 0.6em;
}

.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  margin: 0.8em 0 0.4em;
  font-weight: 600;
  line-height: 1.35;
}

.markdown-body :deep(h1) {
  font-size: 1.25em;
}

.markdown-body :deep(h2) {
  font-size: 1.15em;
}

.markdown-body :deep(h3) {
  font-size: 1.05em;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 0.4em 0 0.6em;
  padding-left: 1.4em;
}

.markdown-body :deep(li) {
  margin: 0.2em 0;
}

.markdown-body :deep(blockquote) {
  margin: 0.5em 0;
  padding: 0.4em 0.8em;
  border-left: 3px solid #8bc34a;
  background: rgba(139, 195, 74, 0.08);
  color: #555;
}

.markdown-body :deep(code) {
  padding: 0.15em 0.35em;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.06);
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 0.92em;
}

.markdown-body :deep(pre) {
  margin: 0.6em 0;
  padding: 10px 12px;
  border-radius: 8px;
  background: #2d2d2d;
  color: #f5f5f5;
  overflow-x: auto;
}

.markdown-body :deep(pre code) {
  padding: 0;
  background: transparent;
  color: inherit;
  font-size: 0.88em;
}

.markdown-body :deep(a) {
  color: #558b2f;
  text-decoration: underline;
  word-break: break-all;
}

.markdown-body :deep(a:hover) {
  color: #33691e;
}

.markdown-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 0.6em 0;
  font-size: 0.92em;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid #e0e0e0;
  padding: 6px 8px;
}

.markdown-body :deep(th) {
  background: rgba(139, 195, 74, 0.12);
}

.markdown-body :deep(hr) {
  border: none;
  border-top: 1px solid #e0e0e0;
  margin: 0.8em 0;
}

.markdown-body :deep(img),
.markdown-body :deep(video),
.markdown-body :deep(iframe) {
  zoom: 0.25;
  display: block;
  height: auto;
  margin: 4px 0;
}

.markdown-body :deep(pre img),
.markdown-body :deep(pre video) {
  zoom: 1;
  display: inline;
  margin: 0;
}

/* markdown-ui 加购按钮：贴合绿主题，可放在推荐图文旁 */
.markdown-body :deep(.widget-button-group) {
  display: inline-flex;
  margin: 8px 0 4px;
  vertical-align: middle;
}

.markdown-body :deep(.widget-button-group > div[role='group']) {
  display: inline-flex;
  gap: 8px;
  flex-wrap: wrap;
}

.markdown-body :deep(.widget-button-group button) {
  border: none;
  background: linear-gradient(135deg, #8bc34a 0%, #66bb6a 100%);
  color: #fff;
  padding: 8px 16px;
  border-radius: 18px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(139, 195, 74, 0.3);
  transition: transform 0.15s, box-shadow 0.15s;
}

.markdown-body :deep(.widget-button-group button:hover) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(139, 195, 74, 0.4);
}

.markdown-body :deep(.widget-button-group button[aria-pressed='true']) {
  filter: brightness(0.95);
}

@keyframes fadeUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
