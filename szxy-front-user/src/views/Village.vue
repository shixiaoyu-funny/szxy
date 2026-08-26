<template>
  <div class="village-container">
    <PageLoadingOverlay :visible="!pageReady" />

    <div v-show="pageReady">
      <div class="village-tabs" role="tablist">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          type="button"
          role="tab"
          :aria-selected="activeTab === tab.value"
          :class="['tab-btn', { active: activeTab === tab.value }]"
          @click="activeTab = tab.value"
        >
          {{ tab.label }}
        </button>
        <span class="tab-indicator" :class="{ right: activeTab === 'collects' }" />
      </div>

      <div class="village-content">
        <div v-if="error" class="error-container">
          <p>{{ error }}</p>
          <button class="btn btn-primary" type="button" @click="fetchTopList">重试</button>
        </div>

        <div v-else-if="!villageList.length" class="empty-container">
          <p>暂无优质农村数据</p>
        </div>

        <ol v-else class="village-list">
          <li
            v-for="(village, index) in villageList"
            :key="village.id"
            class="village-row"
            @click="goVillageDetail(village.id)"
          >
            <span class="rank" :class="rankClass(index)">{{ index + 1 }}</span>
            <div class="village-image">
              <img :src="coverOf(village)" :alt="village.name" />
            </div>
            <div class="village-info">
              <div class="info-top">
                <h3 class="village-name">{{ village.name }}</h3>
                <span class="stat-value">
                  <template v-if="activeTab === 'likes'">{{ village.likes ?? 0 }} 点赞</template>
                  <template v-else>{{ village.collects ?? 0 }} 收藏</template>
                </span>
              </div>
              <p class="village-location">
                {{ [village.province, village.city, village.county].filter(Boolean).join(' · ') }}
              </p>
              <p v-if="village.intro" class="village-desc">{{ village.intro }}</p>
            </div>
          </li>
        </ol>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getTopVillageByLikes, getTopVillageByCollections } from '../api/village';
import { getErrorMessage } from '../api/axios';
import PageLoadingOverlay from '../components/PageLoadingOverlay.vue';
import { collectImageUrlsFromItems, preloadImages } from '../utils/preloadImages';

interface Village {
  id: number;
  managerName?: string;
  name: string;
  province?: string;
  city?: string;
  county?: string;
  type?: number;
  intro?: string;
  image?: string;
  bestTime?: string;
  activity?: string;
  contact?: string;
  likes?: number;
  collects?: number;
}

const route = useRoute();
const router = useRouter();

const activeTab = ref<'likes' | 'collects'>('likes');
const pageReady = ref(false);
const error = ref('');
const villageList = ref<Village[]>([]);

const tabs = [
  { label: '按点赞排序', value: 'likes' as const },
  { label: '按收藏排序', value: 'collects' as const }
];

function rankClass(index: number) {
  if (index === 0) return 'top1';
  if (index === 1) return 'top2';
  if (index === 2) return 'top3';
  return '';
}

function coverOf(village: Village) {
  const raw = village.image || '';
  return raw.split(',')[0]?.trim() || '';
}

function goVillageDetail(id: number) {
  router.push(`/village/${id}`);
}

async function fetchTopList() {
  pageReady.value = false;
  error.value = '';
  try {
    const res =
      activeTab.value === 'likes'
        ? await getTopVillageByLikes()
        : await getTopVillageByCollections();
    villageList.value = (res as { data?: Village[] }).data ?? [];
    await preloadImages(collectImageUrlsFromItems(villageList.value));
  } catch (err) {
    error.value = getErrorMessage(err);
  } finally {
    pageReady.value = true;
  }
}

onMounted(() => {
  const q = String(route.query.q ?? '').trim();
  if (q && route.path === '/village') {
    router.replace({
      path: '/search',
      query: {
        q,
        type: String(route.query.type ?? '1')
      }
    });
  }
});

watch(activeTab, () => {
  if (route.path !== '/village' || String(route.query.q ?? '').trim()) return;
  fetchTopList();
}, { immediate: true });
</script>

<style scoped>
.village-container {
  min-height: 100vh;
  background: transparent;
}

.village-tabs {
  position: relative;
  display: grid;
  grid-template-columns: 1fr 1fr;
  margin: 12px 16px 8px;
  padding: 4px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.tab-btn {
  position: relative;
  z-index: 1;
  padding: 10px 8px;
  border: none;
  background: transparent;
  font-size: 14px;
  color: #888;
  cursor: pointer;
  transition: color 0.2s ease;
}

.tab-btn.active {
  color: #558b2f;
  font-weight: 600;
}

.tab-indicator {
  position: absolute;
  left: 4px;
  bottom: 4px;
  width: calc(50% - 4px);
  height: 3px;
  border-radius: 2px;
  background: #8bc34a;
  transition: transform 0.25s ease;
  pointer-events: none;
}

.tab-indicator.right {
  transform: translateX(100%);
}

.village-content {
  padding: 0 16px 28px;
}

.error-container,
.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 40vh;
  gap: 16px;
  color: #888;
}

.error-container p {
  color: #f44336;
  font-size: 15px;
}

.village-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.village-row {
  display: grid;
  grid-template-columns: 28px 120px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  padding: 10px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.2s ease, transform 0.2s ease;
  cursor: pointer;
}

.village-row:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.rank {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: #aaa;
  line-height: 1;
}

.rank.top1 { color: #e6a23c; }
.rank.top2 { color: #909399; }
.rank.top3 { color: #b87333; }

.village-image {
  width: 120px;
  aspect-ratio: 4 / 3;
  border-radius: 8px;
  overflow: hidden;
  background: #eef2e8;
  flex-shrink: 0;
}

.village-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.4s ease;
}

.village-row:hover .village-image img {
  transform: scale(1.04);
}

.village-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-right: 4px;
}

.info-top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.village-name {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #2d331f;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stat-value {
  flex-shrink: 0;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  color: #7a8f5a;
}

.village-location {
  margin: 0;
  font-size: 12px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.village-desc {
  margin: 0;
  font-size: 13px;
  color: #666;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

@media (max-width: 480px) {
  .village-row {
    grid-template-columns: 24px 96px minmax(0, 1fr);
    gap: 8px;
    padding: 8px;
  }

  .village-image {
    width: 96px;
  }

  .village-name {
    font-size: 15px;
  }

  .village-desc {
    -webkit-line-clamp: 1;
    font-size: 12px;
  }
}
</style>
