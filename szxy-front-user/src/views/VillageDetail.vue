<template>
  <div class="village-detail-container">
    <PageLoadingOverlay :visible="!pageReady" />

    <div v-if="pageReady && loadError && !villageInfo" class="load-error">
      <p>{{ loadError }}</p>
      <button type="button" class="btn btn-primary" @click="fetchVillageDetail">重试</button>
    </div>

    <div v-if="pageReady && villageInfo" class="village-content">
      <div class="village-image">
        <img :src="getMainImage(villageInfo.image)" :alt="villageInfo.name" />
      </div>

      <div class="village-info">
        <h2 class="village-name">{{ villageInfo.name }}</h2>
        <div class="village-meta">
          <span class="meta-item">📍 {{ locationText }}</span>
          <span v-if="villageInfo.managerName" class="meta-item">👤 {{ villageInfo.managerName }}</span>
          <span v-if="villageInfo.type" class="meta-item">🏷️ {{ getVillageTypeLabel(villageInfo.type) }}</span>
        </div>
        <p v-if="villageInfo.intro" class="village-desc">{{ villageInfo.intro }}</p>

        <div v-if="villageInfo.bestTime" class="extra-info">
          <h3 class="info-title">最佳游玩时间</h3>
          <p class="info-text">{{ villageInfo.bestTime }}</p>
        </div>

        <div v-if="villageInfo.activity" class="extra-info">
          <h3 class="info-title">特色活动</h3>
          <p class="info-text">{{ villageInfo.activity }}</p>
        </div>

        <div v-if="villageInfo.contact" class="extra-info">
          <h3 class="info-title">联系方式</h3>
          <p class="info-text">
            <template v-for="(seg, index) in contactSegments" :key="index">
              <a
                v-if="seg.type === 'phone'"
                :href="seg.href"
                class="contact-link"
              >{{ seg.value }}</a>
              <a
                v-else-if="seg.type === 'email'"
                :href="seg.href"
                class="contact-link"
              >{{ seg.value }}</a>
              <span v-else>{{ seg.value }}</span>
            </template>
          </p>
        </div>

        <div class="stat-buttons">
          <div class="stat-btn">
            <span class="btn-icon">👍</span>
            <span>总点赞</span>
            <span class="btn-count">{{ villageInfo.likes ?? 0 }}</span>
          </div>
          <div class="stat-btn">
            <span class="btn-icon">⭐</span>
            <span>总收藏</span>
            <span class="btn-count">{{ villageInfo.collects ?? 0 }}</span>
          </div>
        </div>
      </div>

      <div class="scenic-section">
        <h3 class="section-title">
          村内景点
          <span v-if="scenicList.length > 0" class="section-count">{{ scenicList.length }}</span>
        </h3>

        <div v-if="!scenicList.length" class="empty-tip">暂无景点，敬请期待</div>

        <div v-else class="scenic-list">
          <article
            v-for="scenic in scenicList"
            :key="scenic.id"
            class="scenic-card"
            @click="goScenicDetail(scenic.id)"
          >
            <div class="scenic-card-image">
              <img :src="getMainImage(scenic.image)" :alt="scenic.name" />
            </div>
            <div class="scenic-card-body">
              <div class="scenic-card-top">
                <h4 class="scenic-card-name">{{ scenic.name }}</h4>
                <span v-if="hasScenicPrice(scenic.price)" class="scenic-card-price">
                  {{ formatScenicPrice(scenic.price) }}
                </span>
              </div>
              <p v-if="scenic.intro" class="scenic-card-intro">{{ scenic.intro }}</p>
              <div class="scenic-card-stats">
                <span v-if="scenic.type != null">🏷️ {{ getScenicType(scenic.type) }}</span>
                <span>👍 {{ scenic.likes ?? 0 }}</span>
                <span>⭐ {{ scenic.collections ?? 0 }}</span>
              </div>
            </div>
          </article>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getVillageDetail } from '../api/village';
import PageLoadingOverlay from '../components/PageLoadingOverlay.vue';
import { getErrorMessage } from '../api/axios';
import { collectImageUrls, collectImageUrlsFromItems, preloadImages } from '../utils/preloadImages';
import { formatScenicPrice, hasScenicPrice } from '../utils/scenicPrice';
import { parseContactSegments } from '../utils/contactLinks';
import { getVillageTypeLabel } from '../utils/villageType';

interface ScenicItem {
  id: number;
  name: string;
  intro?: string;
  image?: string;
  price?: number;
  type?: number;
  likes?: number;
  collections?: number;
}

interface VillageDetail {
  id: number;
  name: string;
  managerName?: string;
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
  scenics?: ScenicItem[];
}

const DEFAULT_COVER =
  'https://shixiaoyu-funny.oss-cn-beijing.aliyuncs.com/%E6%95%B0%E6%99%BA%E4%B9%A1%E7%BA%A6%E6%B3%A8%E5%86%8C%E5%A4%B4%E5%83%8F%E8%AE%BE%E8%AE%A1.png';

const route = useRoute();
const router = useRouter();

const villageId = ref(Number(route.params.id));
const villageInfo = ref<VillageDetail | null>(null);
const pageReady = ref(false);
const loadError = ref('');

const scenicList = computed(() => villageInfo.value?.scenics ?? []);

const contactSegments = computed(() =>
  parseContactSegments(villageInfo.value?.contact ?? '')
);

const locationText = computed(() => {
  if (!villageInfo.value) return '';
  return [villageInfo.value.province, villageInfo.value.city, villageInfo.value.county]
    .filter(Boolean)
    .join(' ');
});

function getMainImage(imageStr?: string) {
  if (!imageStr) return DEFAULT_COVER;
  return imageStr.split(',')[0]?.trim() || DEFAULT_COVER;
}

function getScenicType(type: number) {
  const map: Record<number, string> = {
    1: '自然景观',
    2: '人文景观',
    3: '娱乐体验',
    4: '民俗体验'
  };
  return map[type] ?? '景点';
}

function goScenicDetail(id: number) {
  router.push(`/scenic/${id}`);
}

async function fetchVillageDetail() {
  pageReady.value = false;
  loadError.value = '';
  villageInfo.value = null;

  if (!Number.isFinite(villageId.value)) {
    loadError.value = '无效的农村 ID';
    pageReady.value = true;
    return;
  }

  try {
    const res = await getVillageDetail(villageId.value);
    villageInfo.value = (res as { data?: VillageDetail }).data ?? null;
    if (!villageInfo.value) {
      throw new Error('');
    }
    pageReady.value = true;
    const urls = [
      ...collectImageUrls(villageInfo.value.image),
      ...collectImageUrlsFromItems(scenicList.value)
    ];
    void preloadImages(urls);
  } catch (err) {
    villageInfo.value = null;
    loadError.value = getErrorMessage(err);
    if (loadError.value) {
      ElMessage.error(loadError.value);
    }
    pageReady.value = true;
  }
}

watch(
  () => route.params.id,
  (id) => {
    villageId.value = Number(id);
    fetchVillageDetail();
  },
  { immediate: true }
);
</script>

<style scoped>
.village-detail-container {
  min-height: 100vh;
  background: transparent;
}

.load-error {
  padding: 48px 16px;
  text-align: center;
  color: #666;
}

.load-error p {
  margin-bottom: 16px;
}

.village-content {
  padding-bottom: 24px;
}

.village-image {
  width: 100%;
  height: 240px;
  overflow: hidden;
}

.village-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.village-info {
  background: white;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.village-name {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #333;
}

.village-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 16px;
  margin-bottom: 12px;
  font-size: 14px;
  color: #666;
}

.village-desc {
  font-size: 14px;
  line-height: 1.6;
  color: #333;
  margin-bottom: 16px;
}

.extra-info {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.info-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #333;
}

.info-text {
  font-size: 14px;
  line-height: 1.6;
  color: #666;
  margin: 0;
}

.contact-link {
  color: #558b2f;
  text-decoration: none;
  font-weight: 500;
  border-bottom: 1px dashed rgba(85, 139, 47, 0.45);
  transition: color 0.2s ease, border-color 0.2s ease;
}

.contact-link:hover {
  color: #33691e;
  border-bottom-color: #33691e;
}

.stat-buttons {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.stat-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: #fafdf7;
  font-size: 14px;
  color: #555;
}

.btn-icon {
  font-size: 16px;
}

.btn-count {
  font-size: 12px;
  opacity: 0.85;
  font-variant-numeric: tabular-nums;
}

.scenic-section {
  background: white;
  padding: 16px 20px 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 16px;
  color: #18191c;
}

.section-count {
  font-size: 14px;
  font-weight: 500;
  color: #9499a0;
  margin-left: 6px;
}

.empty-tip {
  text-align: center;
  color: #9499a0;
  padding: 24px 0;
  font-size: 14px;
}

.scenic-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.scenic-card {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 12px;
  padding: 10px;
  border: 1px solid #eef4e6;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.25s ease;
}

.scenic-card:hover {
  border-color: #8bc34a;
  box-shadow: 0 4px 12px rgba(139, 195, 74, 0.18);
  transform: translateY(-2px);
}

.scenic-card-image {
  width: 120px;
  aspect-ratio: 4 / 3;
  border-radius: 8px;
  overflow: hidden;
  background: #eef2e8;
}

.scenic-card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.scenic-card-body {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.scenic-card-top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.scenic-card-name {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #2d331f;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.scenic-card-price {
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 600;
  color: #8bc34a;
}

.scenic-card-intro {
  margin: 0;
  font-size: 13px;
  color: #666;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.scenic-card-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 12px;
  color: #888;
}

@media (max-width: 480px) {
  .scenic-card {
    grid-template-columns: 96px minmax(0, 1fr);
    gap: 8px;
    padding: 8px;
  }

  .scenic-card-image {
    width: 96px;
  }
}
</style>
