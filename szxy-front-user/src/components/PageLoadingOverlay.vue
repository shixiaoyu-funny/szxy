<template>
  <Teleport to="#page-loading-host">
    <Transition name="page-loading-fade">
      <div v-if="visible" class="page-loading-indicator" aria-busy="true" aria-label="页面加载中">
        <div class="page-loading-spinner" />
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
defineProps<{
  visible: boolean;
}>();
</script>

<style scoped>
.page-loading-indicator {
  position: absolute;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.page-loading-spinner {
  width: 44px;
  height: 44px;
  border: 3px solid rgba(139, 195, 74, 0.25);
  border-top-color: #8bc34a;
  border-radius: 50%;
  animation: page-loading-spin 0.85s linear infinite;
}

.page-loading-fade-enter-active,
.page-loading-fade-leave-active {
  transition: opacity 0.3s ease;
}

.page-loading-fade-enter-from,
.page-loading-fade-leave-to {
  opacity: 0;
}

@keyframes page-loading-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
