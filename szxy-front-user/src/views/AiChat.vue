<template>
  <div class="ai-chat-page">
    <AiChatPanel />
  </div>
</template>

<script setup lang="ts">
import AiChatPanel from '../components/AiChatPanel.vue';
</script>

<style scoped>
.ai-chat-page {
  height: calc(100vh - 60px - 48px - 48px);
  min-height: 480px;
}

.ai-chat-page :deep(.ai-chat-panel) {
  margin: 0;
  aspect-ratio: unset;
  height: 100%;
}
</style>
