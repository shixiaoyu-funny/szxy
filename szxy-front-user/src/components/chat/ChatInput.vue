<script setup lang="ts">
import { computed, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { uploadFile } from '../../api/upload';
import { getErrorMessage } from '../../api/axios';

defineProps<{ disabled?: boolean }>();
const emit = defineEmits<{
  send: [payload: { text: string; mediaUrls: string[] }];
}>();

const text = ref('');
const mediaUrls = ref<string[]>([]);
const uploading = ref(false);
const fileInput = ref<HTMLInputElement | null>(null);
const MAX_IMAGES = 6;

const canSend = computed(
  () => !uploading.value && (!!text.value.trim() || mediaUrls.value.length > 0)
);

const pickImages = () => {
  fileInput.value?.click();
};

const onFiles = async (e: Event) => {
  const input = e.target as HTMLInputElement;
  const files = Array.from(input.files || []);
  input.value = '';
  if (!files.length) return;

  const remain = MAX_IMAGES - mediaUrls.value.length;
  if (remain <= 0) {
    ElMessage.warning(`一次最多上传 ${MAX_IMAGES} 张图片`);
    return;
  }
  const selected = files.slice(0, remain);
  if (files.length > remain) {
    ElMessage.warning(`一次最多 ${MAX_IMAGES} 张，已取前 ${remain} 张`);
  }

  uploading.value = true;
  try {
    for (const file of selected) {
      if (!file.type.startsWith('image/')) {
        ElMessage.warning(`已跳过非图片文件：${file.name}`);
        continue;
      }
      if (file.size > 5 * 1024 * 1024) {
        ElMessage.warning(`${file.name} 超过 5MB`);
        continue;
      }
      const res = await uploadFile(file);
      const url = (res as { data?: string }).data;
      if (!url) {
        ElMessage.error(`${file.name} 上传失败`);
        continue;
      }
      mediaUrls.value.push(url);
    }
  } catch (err) {
    ElMessage.error(getErrorMessage(err) || '图片上传失败');
  } finally {
    uploading.value = false;
  }
};

const removeImage = (idx: number) => {
  mediaUrls.value.splice(idx, 1);
};

const handleSend = () => {
  if (!canSend.value) return;
  const payload = {
    text: text.value.trim(),
    mediaUrls: [...mediaUrls.value],
  };
  emit('send', payload);
  text.value = '';
  mediaUrls.value = [];
};
</script>

<template>
  <div class="chat-input">
    <div v-if="mediaUrls.length" class="preview-row">
      <div v-for="(url, idx) in mediaUrls" :key="url + idx" class="preview-item">
        <img :src="url" alt="预览" loading="lazy" />
        <button type="button" class="preview-remove" :disabled="disabled || uploading" @click="removeImage(idx)">
          ×
        </button>
      </div>
      <span v-if="uploading" class="uploading-tip">上传中…</span>
    </div>

    <div class="input-row">
      <input
        ref="fileInput"
        type="file"
        accept="image/*"
        multiple
        class="file-hidden"
        :disabled="disabled || uploading"
        @change="onFiles"
      />
      <button
        type="button"
        class="attach-btn"
        title="上传图片"
        :disabled="disabled || uploading || mediaUrls.length >= MAX_IMAGES"
        @click="pickImages"
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
          <circle cx="8.5" cy="8.5" r="1.5" />
          <polyline points="21 15 16 10 5 21" />
        </svg>
      </button>
      <textarea
        v-model="text"
        placeholder="向禾小智提问，可附带图片…"
        rows="1"
        :disabled="disabled || uploading"
        @keydown.enter.exact.prevent="!disabled && canSend && handleSend()"
      />
      <button class="send-btn" type="button" :disabled="disabled || !canSend" @click="handleSend">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M22 2L11 13" />
          <path d="M22 2L15 22L11 13L2 9L22 2Z" />
        </svg>
      </button>
    </div>
  </div>
</template>

<style scoped>
.chat-input {
  border-top: 1px solid #eef0f3;
  background: #fff;
  padding: 10px 16px 12px;
}

.preview-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 10px;
}

.preview-item {
  position: relative;
  width: 64px;
  height: 64px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e4e7ed;
  background: #f5f7fa;
}

.preview-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.preview-remove {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 18px;
  height: 18px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 12px;
  line-height: 1;
  cursor: pointer;
  padding: 0;
}

.uploading-tip {
  font-size: 12px;
  color: #888;
}

.input-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
}

.file-hidden {
  display: none;
}

.attach-btn {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  border: 1px solid #e4e7ed;
  background: #fafbfc;
  color: #66bb6a;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.attach-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.chat-input textarea {
  flex: 1;
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  outline: none;
  resize: none;
  padding: 10px 12px;
  font-size: 14px;
  line-height: 1.5;
  background: #fafbfc;
  color: #333;
  min-height: 20px;
  max-height: 100px;
  transition: border-color 0.2s;
}

.chat-input textarea:focus {
  border-color: #8bc34a;
  background: #fff;
}

.chat-input textarea::placeholder {
  color: #b0b3b8;
}

.chat-input textarea:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.send-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: linear-gradient(135deg, #8bc34a 0%, #66bb6a 100%);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.15s, box-shadow 0.15s;
  box-shadow: 0 2px 8px rgba(139, 195, 74, 0.35);
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(139, 195, 74, 0.45);
}

.send-btn:disabled {
  background: #d0d3d9;
  box-shadow: none;
  cursor: not-allowed;
}
</style>
