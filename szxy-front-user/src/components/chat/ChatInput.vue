<script setup lang="ts">
import { ref } from 'vue';

defineProps<{ disabled?: boolean }>();
const emit = defineEmits<{ send: [text: string] }>();
const text = ref('');

const handleSend = () => {
  const t = text.value.trim();
  if (!t) return;
  emit('send', t);
  text.value = '';
};
</script>

<template>
  <div class="chat-input">
    <textarea
      v-model="text"
      placeholder="向禾小智提问，例如：推荐附近适合亲子游的景点…"
      rows="1"
      :disabled="disabled"
      @keydown.enter.exact.prevent="!disabled && handleSend()"
    />
    <button class="send-btn" type="button" :disabled="disabled" @click="handleSend">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M22 2L11 13" />
        <path d="M22 2L15 22L11 13L2 9L22 2Z" />
      </svg>
    </button>
  </div>
</template>

<style scoped>
.chat-input {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 12px 16px;
  border-top: 1px solid #eef0f3;
  background: #fff;
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
