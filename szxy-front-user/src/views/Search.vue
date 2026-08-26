<template>
  <div class="search-container">
    <PageLoadingOverlay :visible="!pageReady" />

    <div v-show="pageReady">
      <div class="search-banner">
        <div class="search-banner-top">
          <button type="button" class="back-btn" aria-label="返回" @click="goBack">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
              <path d="M15 18l-6-6 6-6v12z" />
            </svg>
          </button>
          <p class="search-keyword">搜索「{{ keyword }}」</p>
        </div>
        <p v-if="!error" class="search-meta">共 {{ searchTotal }} 条{{ typeLabel }}结果</p>
      </div>

      <div class="search-tabs" role="tablist">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          type="button"
          role="tab"
          :aria-selected="searchType === tab.value"
          :class="['tab-btn', { active: searchType === tab.value }]"
          @click="switchType(tab.value)"
        >
          {{ tab.label }}
        </button>
        <span class="tab-indicator" :class="{ right: searchType === 2 }" />
      </div>

      <div class="search-content">
        <div v-if="error" class="error-container">
          <p>{{ error }}</p>
          <button class="btn btn-primary" type="button" @click="fetchSearch(true)">重试</button>
        </div>

        <div v-else-if="!resultList.length" class="empty-container">
          <p>未找到匹配的{{ typeLabel }}</p>
        </div>

        <ol v-else-if="searchType === 1" class="result-list">
          <li
            v-for="(item, index) in villageList"
            :key="item.id"
            class="result-row village-row"
            @click="goVillageDetail(item.id)"
          >
            <span class="rank">{{ rowNo(index) }}</span>
            <div class="result-image">
              <img :src="coverOf(item.image)" :alt="item.name" />
            </div>
            <div class="result-info">
              <h3 class="result-name">{{ item.name }}</h3>
              <p class="result-sub">
                {{ [item.province, item.city, item.county].filter(Boolean).join(' · ') }}
              </p>
              <p v-if="item.intro" class="result-desc">{{ item.intro }}</p>
            </div>
          </li>
        </ol>

        <ol v-else class="result-list">
          <li
            v-for="(item, index) in scenicList"
            :key="item.id"
            class="result-row scenic-row"
            @click="goScenicDetail(item.id)"
          >
            <span class="rank">{{ rowNo(index) }}</span>
            <div class="result-image">
              <img :src="coverOf(item.image)" :alt="item.name" />
            </div>
            <div class="result-info">
              <div class="info-top">
                <h3 class="result-name">{{ item.name }}</h3>
                <span v-if="hasScenicPrice(item.price)" class="price-tag">
                  {{ formatScenicPrice(item.price) }}
                </span>
              </div>
              <p v-if="item.villageName" class="result-sub">{{ item.villageName }}</p>
              <p v-if="item.intro" class="result-desc">{{ item.intro }}</p>
              <div class="scenic-tags">
                <span v-if="item.type" class="tag">{{ scenicTypeLabel(item.type) }}</span>
                <span v-if="item.likes != null" class="tag muted">👍 {{ item.likes }}</span>
                <span v-if="item.collections != null" class="tag muted">⭐ {{ item.collections }}</span>
              </div>
            </div>
          </li>
        </ol>

        <div v-if="!error && searchTotal > pageSize" class="search-pagination">
          <button
            type="button"
            class="page-btn"
            :disabled="pageNo <= 1 || loading"
            @click="changePage(pageNo - 1)"
          >
            上一页
          </button>
          <span class="page-info">{{ pageNo }} / {{ totalPages }}</span>
          <button
            type="button"
            class="page-btn"
            :disabled="pageNo >= totalPages || loading"
            @click="changePage(pageNo + 1)"
          >
            下一页
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { searchContent } from '../api/user';
import { getErrorMessage } from '../api/axios';
import PageLoadingOverlay from '../components/PageLoadingOverlay.vue';
import { collectImageUrlsFromItems, preloadImages } from '../utils/preloadImages';
import { formatScenicPrice, hasScenicPrice } from '../utils/scenicPrice';

interface VillageItem {
  id: number;
  name: string;
  province?: string;
  city?: string;
  county?: string;
  intro?: string;
  image?: string;
}

interface ScenicItem {
  id: number;
  name: string;
  villageName?: string;
  intro?: string;
  image?: string;
  price?: number;
  type?: number;
  likes?: number;
  collections?: number;
}

const route = useRoute();
const router = useRouter();

const pageReady = ref(false);
const loading = ref(false);
const error = ref('');
const villageList = ref<VillageItem[]>([]);
const scenicList = ref<ScenicItem[]>([]);
const searchTotal = ref(0);
const pageNo = ref(1);
const pageSize = 10;

const tabs = [
  { label: '农村', value: 1 as const },
  { label: '景点', value: 2 as const }
];

const keyword = computed(() => String(route.query.q ?? '').trim());

const searchType = computed(() => {
  const t = Number(route.query.type);
  return t === 2 ? 2 : 1;
});

const typeLabel = computed(() => (searchType.value === 2 ? '景点' : '农村'));

const resultList = computed(() =>
  searchType.value === 2 ? scenicList.value : villageList.value
);

const totalPages = computed(() => Math.max(1, Math.ceil(searchTotal.value / pageSize)));

function coverOf(image?: string) {
  return (image || '').split(',')[0]?.trim() || '';
}

function rowNo(index: number) {
  return (pageNo.value - 1) * pageSize + index + 1;
}

function scenicTypeLabel(type: number) {
  const map: Record<number, string> = {
    1: '自然景观',
    2: '人文景观',
    3: '娱乐体验',
    4: '民俗体验'
  };
  return map[type] ?? '景点';
}

function goVillageDetail(id: number) {
  router.push(`/village/${id}`);
}

function goScenicDetail(id: number) {
  router.push(`/scenic/${id}`);
}

function goBack() {
  if (window.history.length > 1) {
    router.back();
  } else {
    router.push('/');
  }
}

function switchType(type: 1 | 2) {
  if (type === searchType.value) return;
  router.replace({
    path: '/search',
    query: { q: keyword.value, type: String(type) }
  });
}

async function fetchSearch(resetPage = true) {
  if (!keyword.value) {
    villageList.value = [];
    scenicList.value = [];
    searchTotal.value = 0;
    pageReady.value = true;
    return;
  }

  if (resetPage) {
    pageNo.value = 1;
  }

  pageReady.value = false;
  loading.value = true;
  error.value = '';

  try {
    const res = await searchContent(keyword.value, searchType.value, pageNo.value, pageSize);
    const page = (res as { data?: { total?: number; data?: unknown[] } }).data;
    const rows = page?.data ?? [];
    searchTotal.value = Number(page?.total ?? 0);

    if (searchType.value === 2) {
      scenicList.value = rows as ScenicItem[];
      villageList.value = [];
      await preloadImages(collectImageUrlsFromItems(scenicList.value));
    } else {
      villageList.value = rows as VillageItem[];
      scenicList.value = [];
      await preloadImages(collectImageUrlsFromItems(villageList.value));
    }
  } catch (err) {
    error.value = getErrorMessage(err);
    villageList.value = [];
    scenicList.value = [];
    searchTotal.value = 0;
  } finally {
    loading.value = false;
    pageReady.value = true;
  }
}

async function changePage(next: number) {
  if (next < 1 || next > totalPages.value) return;
  pageNo.value = next;
  await fetchSearch(false);
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

watch(
  () => [route.query.q, route.query.type] as const,
  () => {
    fetchSearch(true);
  },
  { immediate: true }
);
</script>

<style scoped>
.search-container {
  min-height: 100vh;
  background: transparent;
}

.search-banner {
  margin: 12px 16px 8px;
  padding: 14px 16px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.search-banner-top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.back-btn {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  margin: 0;
  padding: 0;
  border: none;
  border-radius: 8px;
  background: #f5f7fa;
  color: #606266;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

.back-btn:hover {
  background: #e8f5e9;
  color: #558b2f;
}

.search-keyword {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #2d331f;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.search-meta {
  margin: 6px 0 0 40px;
  font-size: 13px;
  color: #888;
}

.search-tabs {
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

.search-content {
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

.result-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.result-row {
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

.result-row:hover {
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
}

.result-image {
  width: 120px;
  aspect-ratio: 4 / 3;
  border-radius: 8px;
  overflow: hidden;
  background: #eef2e8;
  flex-shrink: 0;
}

.result-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.result-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.result-name {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #2d331f;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.result-sub {
  margin: 0;
  font-size: 12px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.result-desc {
  margin: 0;
  font-size: 13px;
  color: #666;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.price-tag {
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 600;
  color: #8bc34a;
}

.scenic-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 2px;
}

.tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  background: #e8f5e9;
  color: #2e7d32;
}

.tag.muted {
  background: #f5f5f5;
  color: #888;
}

.search-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 20px;
  padding: 12px 0;
}

.page-btn {
  padding: 8px 16px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background: #fff;
  color: #606266;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  border-color: #8bc34a;
  color: #558b2f;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: #666;
  font-variant-numeric: tabular-nums;
}

@media (max-width: 480px) {
  .result-row {
    grid-template-columns: 24px 96px minmax(0, 1fr);
    gap: 8px;
    padding: 8px;
  }

  .result-image {
    width: 96px;
  }

  .result-name {
    font-size: 15px;
  }

  .result-desc {
    -webkit-line-clamp: 1;
    font-size: 12px;
  }
}
</style>
