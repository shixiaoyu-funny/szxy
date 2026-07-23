<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import BottomNav from './components/BottomNav.vue';

const route = useRoute();
const showBottomNav = computed(() => {
  // 登录页面不显示底边栏
  return route.path !== '/login';
});
</script>

<template>
  <div class="app-container" :class="{ 'no-bottom-nav': !showBottomNav }">
    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
    <BottomNav v-if="showBottomNav" />
  </div>
</template>

<style scoped>
.app-container {
  min-height: 100vh;
  padding-bottom: 60px; /* 为底边栏留出空间 */
}

.no-bottom-nav {
  padding-bottom: 0;
}
</style>
