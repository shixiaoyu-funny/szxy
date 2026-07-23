<template>
  <div class="ai-inquire-container">
    <header class="ai-header">
      <h1 class="ai-title">智能问答</h1>
      <Location />
    </header>
    
    <div class="inquire-content">
      <div class="chat-container">
        <div class="chat-messages">
          <div 
            v-for="(message, index) in messages" 
            :key="index"
            :class="['message', message.isUser ? 'user-message' : 'ai-message']"
          >
            <div class="message-content">
              <div class="message-text" v-html="formatMessage(message.text)"></div>
            </div>
          </div>
          
          <div v-if="loading" class="loading-message">
            <div class="loading-dots">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        </div>
        
        <div class="chat-input">
          <input 
            v-model="inputMessage" 
            placeholder="请输入您的问题..."
            class="input-field"
            @keyup.enter="sendMessage"
          />
          <button @click="sendMessage" class="send-btn">
            发送
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';

import { aiInquire, memory } from '../../api/ai';
import Location from '../../components/Location.vue';

interface Message {
  text: string;
  isUser: boolean;
}

const messages = ref<Message[]>([]);
const inputMessage = ref('');
const loading = ref(false);

const formatMessage = (text: string) => {
  return text.replace(/\n/g, '<br>');
};

const streamText = (text: string, messageIndex: number) => {
  let index = 0;
  const interval = setInterval(() => {
    if (index < text.length) {
      if (messages.value[messageIndex]) {
        messages.value[messageIndex].text += text[index];
      }
      index++;
    } else {
      clearInterval(interval);
    }
  }, 30);
};

const sendMessage = async () => {
  if (!inputMessage.value.trim()) return;
  
  const userMessage = inputMessage.value.trim();
  messages.value.push({ text: userMessage, isUser: true });
  inputMessage.value = '';
  loading.value = true;
  
  try {
    const res = await aiInquire(userMessage);
    const aiResponse = res.data;
    messages.value.push({ text: '', isUser: false });
    const messageIndex = messages.value.length - 1;
    streamText(aiResponse, messageIndex);
  } catch (error) {
    console.error('发送消息失败:', error);
    messages.value.push({ text: '抱歉，我暂时无法回答您的问题，请稍后再试。', isUser: false });
  } finally {
    loading.value = false;
  }
};

const loadChatHistory = async () => {
  try {
    const res = await memory();
    const history = res.data;
    if (Array.isArray(history)) {
      history.forEach((item, index) => {
        messages.value.push({
          text: item,
          isUser: index % 2 === 0
        });
      });
    }
  } catch (error) {
    console.error('加载聊天历史失败:', error);
  }
};

onMounted(() => {
  loadChatHistory();
});
</script>

<style scoped>
.ai-inquire-container {
  min-height: 100vh;
  background: transparent;
  padding: 16px;
  margin: 5px;
}

.ai-header {
  background: linear-gradient(-45deg, #11998e, #38ef7d, #11998e, #38ef7d);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ai-title {
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
  margin: 0;
}

.inquire-content {
  height: calc(100vh - 180px);
}

.chat-container {
  height: 100%;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-messages {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  scrollbar-width: thin;
}

.message {
  margin-bottom: 16px;
  display: flex;
  max-width: 80%;
}

.user-message {
  justify-content: flex-end;
}

.ai-message {
  justify-content: flex-start;
}

.message-content {
  padding: 12px 16px;
  border-radius: 18px;
  position: relative;
}

.user-message .message-content {
  background: #8BC34A;
  color: white;
  border-bottom-right-radius: 4px;
}

.ai-message .message-content {
  background: #f0f0f0;
  color: #333;
  border-bottom-left-radius: 4px;
}

.message-text {
  line-height: 1.4;
  word-break: break-word;
}

.loading-message {
  display: flex;
  justify-content: flex-start;
  margin-bottom: 16px;
}

.loading-dots {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
  background: #f0f0f0;
  border-radius: 18px;
  border-bottom-left-radius: 4px;
}

.loading-dots span {
  width: 8px;
  height: 8px;
  background: #8BC34A;
  border-radius: 50%;
  animation: pulse 1.4s infinite ease-in-out both;
}

.loading-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.loading-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes pulse {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

.chat-input {
  padding: 16px;
  border-top: 1px solid #e0e0e0;
  display: flex;
  gap: 8px;
  align-items: center;
}

.input-field {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 24px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.3s ease;
}

.input-field:focus {
  border-color: #8BC34A;
}

.send-btn {
  padding: 12px 20px;
  background: #8BC34A;
  color: white;
  border: none;
  border-radius: 24px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.3s ease;
}

.send-btn:hover {
  background: #7CB342;
}

@media (max-width: 480px) {
  .ai-inquire-container {
    padding: 12px;
  }
  
  .inquire-content {
    height: calc(100vh - 160px);
  }
  
  .message {
    max-width: 90%;
  }
}
</style>