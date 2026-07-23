<template>
  <div class="village-container">
    <header class="village-header">
      <button class="back-btn" @click="goBack">←</button>
      <h1 class="header-title">优质农村</h1>
      <Location />
    </header>
    
    <div class="village-tabs">
      <button 
        v-for="tab in tabs" 
        :key="tab.value"
        :class="['tab-btn', { active: activeTab === tab.value }]"
        @click="activeTab = tab.value"
      >
        {{ tab.label }}
      </button>
    </div>
    
    <div class="village-content">
      <div v-if="loading" class="loading-container">
        <div class="loading"></div>
        <p>加载中...</p>
      </div>
      <div v-else-if="error" class="error-container">
        <p>{{ error }}</p>
        <button class="btn btn-primary" @click="fetchVillageList">重试</button>
      </div>
      <div v-else class="village-list">
        <div 
          v-for="village in villageList" 
          :key="village.id"
          class="village-card"
        >
          <div class="village-image">
            <img :src="village.image" :alt="village.name" />
          </div>
          <div class="village-info">
            <h3 class="village-name">{{ village.name }}</h3>
            <p class="village-location">{{ village.province }} {{ village.city }} {{ village.county }}</p>
            <p class="village-desc">{{ village.intro }}</p>
            <div class="village-features">
              <div class="feature-item" v-if="village.best_time">
                <span class="feature-label">最佳游玩时间：</span>
                <span class="feature-value">{{ village.best_time }}</span>
              </div>
              <div class="feature-item" v-if="village.activity">
                <span class="feature-label">特色活动：</span>
                <span class="feature-value">{{ village.activity }}</span>
              </div>
              <div class="feature-item" v-if="village.contact">
                <span class="feature-label">联系电话：</span>
                <span class="feature-value">{{ village.contact }}</span>
              </div>
              <div class="feature-item" v-if="village.contact">
                <span class="feature-label">村长/负责人：</span>
                <span class="feature-value">{{ village.manager_name }}</span>
              </div>
            </div>
            <div class="village-stats">
              <span class="stat-item" v-if="activeTab === 'likes'">❤️ {{ village.likes }}</span>
              <span class="stat-item" v-if="activeTab === 'collects'">⭐ {{ village.collects }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { getTopVillageByLikes, getTopVillageByCollections } from '../api/village';
import Location from '../components/Location.vue';

interface Village {
  id: number;
  manager_name: string;
  name: string;
  province: string;
  city: string;
  county: string;
  type: number;
  intro: string;
  image: string;
  best_time: string;
  activity: string;
  contact: string;
  likes: number;
  collects: number;
}

const router = useRouter();
const activeTab = ref('likes');
const loading = ref(false);
const error = ref('');
const villageList = ref<Village[]>([]);

const tabs = [
  { label: '按点赞排序', value: 'likes' },
  { label: '按收藏排序', value: 'collects' }
];

const goBack = () => {
  router.back();
};

const fetchVillageList = async () => {
  loading.value = true;
  error.value = '';
  
  try {
    let res;
    if (activeTab.value === 'likes') {
      res = await getTopVillageByLikes();
    } else {
      res = await getTopVillageByCollections();
    }
    villageList.value = res.data;
    console.log('农村列表：', villageList.value);
  } catch (err) {
    error.value = '获取农村信息失败，请重试';
    console.error('获取农村信息失败:', err);
  } finally {
    loading.value = false;
  }
};

watch(activeTab, () => {
  fetchVillageList();
});

onMounted(() => {
  fetchVillageList();
});
</script>

<style scoped>
.village-container {
  min-height: 100vh;
  background: transparent;
}

.village-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  padding-top: 6px;
  padding-bottom: 6px;
  background: linear-gradient(to right, #ee813990, #2cdf71d8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 100;
}

.back-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  transition: all 0.3s ease;
  margin-right: 16px;
}

.back-btn:hover {
  background: #f0f9e8;
}

.header-title {
  font-size: 24px;
  font-weight: 700;
  color: #75ef1d;
  flex: 1;
  padding-top: 20px;
}

.village-tabs {
  display: flex;
  background: white;
  margin: 12px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.tab-btn {
  flex: 1;
  padding: 12px;
  border: none;
  background: none;
  font-size: 16px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
}

.tab-btn.active {
  background: #8BC34A;
  color: white;
  font-weight: 500;
}

.village-content {
  padding: 0 12px 24px;
}

.loading-container,
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 40vh;
  gap: 16px;
}

.error-container p {
  color: #f44336;
  font-size: 16px;
}

.village-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.village-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.village-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.village-image {
  width: 100%;
  height: 180px;
  overflow: hidden;
}

.village-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.village-card:hover .village-image img {
  transform: scale(1.05);
}

.village-info {
  padding: 16px;
}

.village-name {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
  color: #333;
}

.village-location {
  font-size: 13px;
  color: #999;
  margin-bottom: 8px;
}

.village-desc {
  font-size: 14px;
  color: #666;
  margin-bottom: 12px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.village-features {
  margin-bottom: 12px;
}

.feature-item {
  font-size: 13px;
  margin-bottom: 4px;
}

.feature-label {
  color: #999;
  margin-right: 4px;
}

.feature-value {
  color: #666;
}

.village-stats {
  display: flex;
  gap: 16px;
  font-size: 14px;
  color: #999;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

@media (max-width: 480px) {
  .village-image {
    height: 160px;
  }
  
  .village-info {
    padding: 12px;
  }
  
  .village-name {
    font-size: 15px;
  }
  
  .village-desc {
    font-size: 13px;
  }
}
</style>