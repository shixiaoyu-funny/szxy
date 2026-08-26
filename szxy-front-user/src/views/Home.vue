<template>
  <div class="home-container">
    <PageLoadingOverlay :visible="!pageReady" />
    <div v-show="pageReady" class="home-content">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <div class="search-type-switch">
        <button
          type="button"
          :class="['type-btn', { active: searchType === 1 }]"
          @click="searchType = 1"
        >
          农村
        </button>
        <button
          type="button"
          :class="['type-btn', { active: searchType === 2 }]"
          @click="searchType = 2"
        >
          景点
        </button>
      </div>
      <div class="search-input-row">
      <input
        type="text"
        class="search-input"
        :placeholder="searchType === 2 ? '搜索景点名称、所属农村...' : '搜索农村名称、地区、特色活动...'"
        v-model="searchQuery"
        @keyup.enter="search"
      />
      <button type="button" class="search-btn" @click="search">
        <svg t="1775232150984" class="icon" viewBox="0 0 1024 1024" version="1.1"
          xmlns="http://www.w3.org/2000/svg" p-id="4238" width="25" height="25">
          <path
            d="M934.966272 879.950848 709.541888 654.526464c47.665152-59.81696 76.166144-135.581696 76.166144-218.012672 0-193.36704-156.752896-350.124032-350.12096-350.124032S85.464064 243.145728 85.464064 436.513792s156.756992 350.124032 350.124032 350.124032c79.875072 0 153.486336-26.766336 212.39808-71.79264l226.04288 226.04288c8.416256 8.416256 19.441664 12.62592 30.470144 12.62592 11.025408 0 22.050816-4.209664 30.466048-12.62592C951.798784 924.055552 951.798784 896.782336 934.966272 879.950848zM165.953536 436.513792c0-148.912128 120.722432-269.630464 269.63456-269.630464S705.21856 287.60064 705.21856 436.513792 584.500224 706.148352 435.588096 706.148352 165.953536 585.42592 165.953536 436.513792z"
            fill="#FFFFFF" p-id="4239"></path>
          <path
            d="M291.135488 302.036992c-8.997888-4.864-20.23424-1.511424-25.097216 7.486464-20.256768 37.476352-32.600064 78.152704-36.688896 120.899584-0.973824 10.181632 6.491136 19.224576 16.672768 20.1984 0.59904 0.057344 1.195008 0.084992 1.784832 0.084992 9.433088 0 17.497088-7.175168 18.413568-16.75776 3.613696-37.7856 14.5152-73.72288 32.401408-106.814464C303.485952 318.138368 300.1344 306.900992 291.135488 302.036992z"
            fill="#FFFFFF" p-id="4240"></path>
        </svg>
      </button>
      </div>
    </div>

    <!-- 优质农村 + 热门景点 并排 -->
    <div class="dual-section">
      <!-- 优质农村轮播 -->
      <div class="village-section">
        <h2 class="section-title">优质农村TOP10<i class="iconfont icon-huo" style="color: red;"></i></h2>
        <div class="carousel-container">
          <div class="carousel-wrapper">
            <div class="carousel" :style="villageCarouselStyle" @mouseenter="onVillageEnter"
              @mouseleave="onVillageLeave">
              <div v-for="(village, index) in villageList" :key="village.id" class="carousel-item"
                :class="{ active: index === currentVillageIndex }" :style="getVillageItemStyle(index)"
                @click="navigateToVillageDetail(village.id)">
                <div class="village-image">
                  <img :src="village.image" :alt="village.name" />
                </div>
                <div class="village-info">
                  <h3 class="village-name">{{ village.name }}</h3>
                  <p class="village-location">{{ village.province }} {{ village.city }} {{ village.county }}</p>
                  <p class="village-desc">{{ village.intro }}</p>
                  <div class="village-features">
                    <div class="feature-item" v-if="village.bestTime">
                      <span class="feature-label">最佳时间：</span>
                      <span class="feature-value">{{ village.bestTime }}</span>
                    </div>
                    <div class="feature-item" v-if="village.activity">
                      <span class="feature-label">特色活动：</span>
                      <span class="feature-value">{{ village.activity }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <button class="carousel-btn prev" @click="prevVillage">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                <path d="M15 18l-6-6 6-6v12z" />
              </svg>
            </button>
            <button class="carousel-btn next" @click="nextVillage">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                <path d="M9 6l6 6-6 6V6z" />
              </svg>
            </button>
          </div>

          <div class="carousel-indicators" v-if="villageList.length > 1">
            <span v-for="(village, index) in villageList" :key="village.id" class="indicator"
              :class="{ active: index === currentVillageIndex }" @click="goToVillage(index)"></span>
          </div>
        </div>
      </div>

      <!-- 景点推荐区 -->
      <div class="scenic-section">
        <h2 class="section-title">热门景点TOP10<i class="iconfont icon-huo" style="color: red;"></i></h2>
        <div class="carousel-container">
          <div class="carousel-wrapper">
            <div class="carousel" :style="carouselStyle" @mouseenter="onScenicEnter" @mouseleave="onScenicLeave">
              <div v-for="(scenic, index) in scenicList" :key="scenic.id" class="carousel-item"
                :class="{ active: index === currentIndex }" :style="getItemStyle(index)"
                @click="navigateToScenicDetail(scenic.id)">
                <div class="scenic-image">
                  <img :src="scenic.image" :alt="scenic.name" />
                </div>
                <div class="scenic-info">
                  <div class="scenic-header">
                    <h3 class="scenic-name">{{ scenic.name }}</h3>
                    <div class="scenic-tags">
                      <span class="tag type-tag" v-if="scenic.type">{{ scenic.type }}</span>
                      <span class="tag village-tag" v-if="scenic.villageName">{{ scenic.villageName }}</span>
                    </div>
                  </div>
                  <p class="scenic-desc" v-if="scenic.intro">{{ scenic.intro }}</p>
                  <div class="scenic-bottom">
                    <span class="price-tag" v-if="hasScenicPrice(scenic.price)">
                      {{ formatScenicPrice(scenic.price, { suffix: '/人' }) }}
                    </span>
                    <div class="scenic-stats">
                      <div class="stat-item" v-if="scenic.likes">👍 {{ scenic.likes }}</div>
                      <div class="stat-item" v-if="scenic.collections">⭐ {{ scenic.collections }}</div>
                      <div class="stat-item" v-if="scenic.hasAccommodation">
                        🏨 {{ scenic.accommodationInfo || '可住宿' }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <button class="carousel-btn prev" @click="prev">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                <path d="M15 18l-6-6 6-6v12z" />
              </svg>
            </button>
            <button class="carousel-btn next" @click="next">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                <path d="M9 6l6 6-6 6V6z" />
              </svg>
            </button>
          </div>

          <div class="carousel-indicators" v-if="scenicList.length > 1">
            <span v-for="(scenic, index) in scenicList" :key="scenic.id" class="indicator"
              :class="{ active: index === currentIndex }" @click="goTo(index)"></span>
          </div>
        </div>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue';

import { useRouter } from 'vue-router';
import { getTopScenic } from '../api/scenic';
import { getTopVillageByLikes } from '../api/village';
import PageLoadingOverlay from '../components/PageLoadingOverlay.vue';
import { collectImageUrlsFromItems, preloadImages } from '../utils/preloadImages';
import { formatScenicPrice, hasScenicPrice } from '../utils/scenicPrice';

interface Scenic {
  id: number;
  name?: string;
  villageName?: string;
  intro?: string;
  image?: string;
  price?: number;
  type?: string;
  collections?: number;
  likes?: number;
  hasAccommodation?: boolean;
  accommodationInfo?: string;
}

interface Village {
  id: number;
  managerName: string;
  name: string;
  province: string;
  city: string;
  county: string;
  type: number;
  intro: string;
  image: string;
  bestTime: string;
  activity: string;
  contact: string;
  likes: number;
  collects: number;
}

const router = useRouter();
const searchQuery = ref('');
const searchType = ref<1 | 2>(1);
const scenicList = ref<Scenic[]>([]);
const villageList = ref<Village[]>([]);
const pageReady = ref(false);

// 轮播：统一向左切「下一张」（translateX 更负），循环时瞬时跳转避免反向长滑
const currentIndex = ref(0);
const currentVillageIndex = ref(0);
const scenicInstant = ref(false);
const villageInstant = ref(false);
const autoPlayTimer = ref<number | null>(null);
const autoPlayResumeTimer = ref<number | null>(null);
const autoPlayInterval = 3000;
const scenicPaused = ref(false);
const villagePaused = ref(false);

const navigateToScenicDetail = (id: number) => {
  router.push(`/scenic/${id}`);
};

const navigateToVillageDetail = (id: number) => {
  router.push(`/village/${id}`);
};

const search = () => {
  if (!searchQuery.value?.trim()) return;
  router.push({
    path: '/search',
    query: {
      q: searchQuery.value.trim(),
      type: String(searchType.value)
    }
  });
};

const fetchScenicList = async () => {
  try {
    const res = await getTopScenic();
    scenicList.value = res.data;
  } catch (error) {
    console.error('获取景点列表失败:', error);
  }
};

const fetchVillageList = async () => {
  try {
    const res = await getTopVillageByLikes();
    villageList.value = res.data;
  } catch (error) {
    console.error('获取农村列表失败:', error);
  }
};

const carouselStyle = computed(() => ({
  transform: `translateX(-${currentIndex.value * 100}%)`,
  transition: scenicInstant.value ? 'none' : 'transform 0.5s ease'
}));

const villageCarouselStyle = computed(() => ({
  transform: `translateX(-${currentVillageIndex.value * 100}%)`,
  transition: villageInstant.value ? 'none' : 'transform 0.5s ease'
}));

const getScaleStyle = (index: number, current: number, total: number) => {
  const distance = Math.abs(index - current);
  if (distance > 2) return { opacity: 0, transform: 'scale(0.8)' };

  let opacity = 1;
  let scale = 1;
  switch (distance) {
    case 0:
      opacity = 1;
      scale = 1;
      break;
    case 1:
      opacity = 0.8;
      scale = 0.9;
      break;
    case 2:
      opacity = 0.6;
      scale = 0.8;
      break;
  }

  return {
    opacity,
    transform: `scale(${scale})`,
    zIndex: total - distance,
    transition: 'opacity 0.5s ease, transform 0.5s ease'
  };
};

const getItemStyle = (index: number) =>
  getScaleStyle(index, currentIndex.value, scenicList.value.length);

const getVillageItemStyle = (index: number) =>
  getScaleStyle(index, currentVillageIndex.value, villageList.value.length);

/** step: 1=下一张(向左), -1=上一张(向右)；循环首尾瞬时跳转，避免整列反向滑 */
const moveIndex = async (
  current: { value: number },
  length: number,
  step: 1 | -1,
  instantFlag: { value: boolean }
) => {
  if (length <= 1) return;
  const from = current.value;
  const to = (from + step + length) % length;
  const wrapping =
    (step === 1 && from === length - 1 && to === 0) ||
    (step === -1 && from === 0 && to === length - 1);

  if (wrapping) {
    instantFlag.value = true;
    current.value = to;
    await nextTick();
    requestAnimationFrame(() => {
      instantFlag.value = false;
    });
  } else {
    current.value = to;
  }
};

const prev = async () => {
  await moveIndex(currentIndex, scenicList.value.length, -1, scenicInstant);
  pauseSharedAutoPlay();
};

const next = async () => {
  await moveIndex(currentIndex, scenicList.value.length, 1, scenicInstant);
  pauseSharedAutoPlay();
};

const prevVillage = async () => {
  await moveIndex(currentVillageIndex, villageList.value.length, -1, villageInstant);
  pauseSharedAutoPlay();
};

const nextVillage = async () => {
  await moveIndex(currentVillageIndex, villageList.value.length, 1, villageInstant);
  pauseSharedAutoPlay();
};

const goTo = (index: number) => {
  currentIndex.value = index;
  pauseSharedAutoPlay();
};

const goToVillage = (index: number) => {
  currentVillageIndex.value = index;
  pauseSharedAutoPlay();
};

/** 自动轮播只推进，不触发 pause（避免每次前进后空等 5 秒） */
const tickAutoPlay = () => {
  if (!scenicPaused.value && scenicList.value.length > 1) {
    void moveIndex(currentIndex, scenicList.value.length, 1, scenicInstant);
  }
  if (!villagePaused.value && villageList.value.length > 1) {
    void moveIndex(currentVillageIndex, villageList.value.length, 1, villageInstant);
  }
};

const startSharedAutoPlay = () => {
  if (autoPlayTimer.value) {
    clearInterval(autoPlayTimer.value);
  }
  if (scenicList.value.length <= 1 && villageList.value.length <= 1) return;
  autoPlayTimer.value = window.setInterval(tickAutoPlay, autoPlayInterval);
};

const stopSharedAutoPlay = () => {
  if (autoPlayTimer.value) {
    clearInterval(autoPlayTimer.value);
    autoPlayTimer.value = null;
  }
};

const pauseSharedAutoPlay = () => {
  stopSharedAutoPlay();
  if (autoPlayResumeTimer.value) {
    clearTimeout(autoPlayResumeTimer.value);
  }
  autoPlayResumeTimer.value = window.setTimeout(() => {
    autoPlayResumeTimer.value = null;
    startSharedAutoPlay();
  }, 5000);
};

const onScenicEnter = () => {
  scenicPaused.value = true;
};

const onScenicLeave = () => {
  scenicPaused.value = false;
};

const onVillageEnter = () => {
  villagePaused.value = true;
};

const onVillageLeave = () => {
  villagePaused.value = false;
};

onMounted(async () => {
  pageReady.value = false;
  try {
    await Promise.all([fetchScenicList(), fetchVillageList()]);
    const urls = [
      ...collectImageUrlsFromItems(scenicList.value),
      ...collectImageUrlsFromItems(villageList.value),
    ];
    await preloadImages(urls);
  } finally {
    pageReady.value = true;
    startSharedAutoPlay();
  }
});

onUnmounted(() => {
  stopSharedAutoPlay();
  if (autoPlayResumeTimer.value) {
    clearTimeout(autoPlayResumeTimer.value);
    autoPlayResumeTimer.value = null;
  }
});
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  background: transparent;
}

.home-header {
  background: linear-gradient(-45deg, #11998e, #38ef7d, #11998e, #38ef7d);
  padding: 16px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
  margin: 0;
}

@media (max-width: 480px) {
  .header-title {
    font-size: 18px;
  }
}

.header-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  transition: all 0.3s ease;
}

.header-btn:hover {
  background: #f0f9e8;
}

.search-bar {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 16px;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  padding: 10px 10px 0;
}

.search-type-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  padding: 0 6px;
}

.type-btn {
  padding: 8px 12px;
  border: 1px solid #e8f5e9;
  border-radius: 8px;
  background: #fafdf7;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.type-btn.active {
  border-color: #8BC34A;
  background: #e8f5e9;
  color: #558B2F;
  font-weight: 600;
}

.search-input-row {
  display: flex;
  overflow: hidden;
  border-radius: 0 0 12px 12px;
}

.search-input {
  flex: 1;
  padding: 12px 16px;
  border: none;
  font-size: 16px;
  outline: none;
}

.search-btn {
  background: #8BC34A;
  border: none;
  padding: 0 16px;
  font-size: 18px;
  cursor: pointer;
  transition: background 0.3s ease;
}

.search-btn:hover {
  background: #7CB342;
}

.dual-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  padding: 0 16px 24px;
  align-items: stretch;
}

.village-section,
.scenic-section {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #8BC34A;
  margin: 24px 0 16px;
}

.village-image,
.scenic-image {
  width: 100%;
  flex: 0 0 58%;
  overflow: hidden;
}

.village-image img,
.scenic-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.carousel-item:hover .village-image img,
.carousel-item:hover .scenic-image img {
  transform: scale(1.05);
}

.village-info,
.scenic-info {
  flex: 1;
  min-height: 0;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow: hidden;
}

.village-name,
.scenic-name {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.village-location {
  font-size: 13px;
  color: #666;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.village-desc,
.scenic-desc {
  font-size: 13px;
  color: #666;
  margin: 0;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.village-features {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-top: auto;
}

.feature-item {
  font-size: 12px;
  line-height: 1.35;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.carousel-container {
  padding: 0;
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.carousel-wrapper {
  position: relative;
  overflow: hidden;
  border-radius: 12px;
  width: 100%;
  aspect-ratio: 4 / 3;
  height: auto;
}

.carousel {
  display: flex;
  height: 100%;
  transition: transform 0.5s ease;
}

.carousel-item {
  flex: 0 0 100%;
  height: 100%;
  position: relative;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.5s ease;
  display: flex;
  flex-direction: column;
}

.carousel-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.carousel-item.active {
  z-index: 10;
}

.carousel-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  border: 2px solid #8BC34A;
  color: #8BC34A;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  z-index: 20;
}

.carousel-btn.prev {
  left: 8px;
}

.carousel-btn.next {
  right: 8px;
}

.carousel-btn:hover {
  background: #8BC34A;
  color: white;
  transform: translateY(-50%) scale(1.1);
}

.carousel-indicators {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 12px;
}

.indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e0e0e0;
  cursor: pointer;
  transition: all 0.3s ease;
}

.indicator.active {
  width: 24px;
  border-radius: 4px;
  background: #8BC34A;
}

.scenic-stats {
  display: flex;
  gap: 12px;
  font-size: 14px;
  color: #999;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.scenic-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}

.scenic-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  flex-shrink: 0;
}

.tag {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
}

.type-tag {
  background: #e8f5e9;
  color: #2E7D32;
}

.village-tag {
  background: #f1f8e9;
  color: #558B2F;
}

.scenic-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.price-tag {
  font-size: 16px;
  font-weight: 600;
  color: #8BC34A;
}

.feature-label {
  font-size: 13px;
  color: #8be6bd;
}

.feature-value {
  font-size: 13px;
  color: #6f6f6f;
}

i {
  font-size: 24px;
  margin-left: 8px;
}

@media (max-width: 768px) {
  .dual-section {
    grid-template-columns: 1fr;
    padding: 0 12px 16px;
  }

  .section-title {
    margin: 16px 0 12px;
    font-size: 16px;
  }

  .carousel-btn {
    width: 32px;
    height: 32px;
  }

  .carousel-btn svg {
    width: 16px;
    height: 16px;
  }
}
</style>