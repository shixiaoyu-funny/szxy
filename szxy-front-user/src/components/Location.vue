<template>
  <div class="location-container">
    <div class="location-icon"><svg t="1775225499985" class="icon" viewBox="0 0 1024 1024" version="1.1"
        xmlns="http://www.w3.org/2000/svg" p-id="3251" width="20" height="20">
        <path
          d="M503.3 0c-201.3 0-365 163.8-365 365 0 129.3 82 263.5 152.2 339.1 61.5 66.4 188.8 277.4 190 279.5l24.2 40.3 22.9-41.1c4.7-8.5 117-208.2 176-270.3 79.6-83.8 164.7-219.1 164.7-347.6C868.3 163.8 704.6 0 503.3 0z m0 54.5c171.2 0 310.5 139.3 310.5 310.5 0 112.4-77.3 233.9-149.7 310-47.9 50.5-123 175.4-161.1 241.1-41.5-67.1-123.4-195.9-172.7-249-68.4-73.8-137.5-195.9-137.5-302.1 0-171.2 139.3-310.5 310.5-310.5z"
          fill="#050101" p-id="3252"></path>
        <path
          d="M503.3 530.1c82.1 0 148.9-66.8 148.9-148.8 0-82.1-66.8-148.9-148.9-148.9s-148.9 66.8-148.9 148.9c0 82 66.8 148.8 148.9 148.8z m0-243.2c52 0 94.4 42.3 94.4 94.4s-42.3 94.3-94.4 94.3-94.4-42.3-94.4-94.3 42.4-94.4 94.4-94.4z"
          fill="#050101" p-id="3253"></path>
      </svg></div>
    <div class="location-info">
      <span v-if="location" class="location-text">
        {{ location.province }} {{ location.city }}
      </span>
      <span v-else class="location-loading">
        定位中...
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { getUserLocation } from '../api/user';

interface Location {
  province: string;
  city: string;
}

const location = ref<Location | null>(null);
const pollingInterval = ref<number | null>(null);

const fetchLocation = async () => {
  try {
    const res = await getUserLocation();
    location.value = res.data;
  } catch (error) {
    console.error('获取位置失败:', error);
  }
};

onMounted(() => {
  // 初始获取位置
  fetchLocation();
  // 每30秒轮询一次
  pollingInterval.value = window.setInterval(fetchLocation, 30000);
});

onUnmounted(() => {
  if (pollingInterval.value) {
    clearInterval(pollingInterval.value);
  }
});
</script>

<style scoped>
.location-container {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  font-size: 14px;
  color: #666;
  transition: all 0.3s ease;
}

.location-container:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.location-icon {
  font-size: 16px;
}

.location-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 150px;
}

.location-loading {
  color: #999;
  font-size: 12px;
}
</style>