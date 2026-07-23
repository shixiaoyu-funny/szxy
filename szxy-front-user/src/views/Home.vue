<template>
  <div class="home-container">
    <!-- 顶部导航 -->
    <header class="home-header">
      <h1 class="header-title">首页</h1>
      <Location />
    </header>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <input type="text" class="search-input" placeholder="搜索景点、农产品..." v-model="searchQuery" @keyup.enter="search" />
      <button class="search-btn"><svg t="1775232150984" class="icon" viewBox="0 0 1024 1024" version="1.1"
          xmlns="http://www.w3.org/2000/svg" p-id="4238" width="25" height="25">
          <path
            d="M934.966272 879.950848 709.541888 654.526464c47.665152-59.81696 76.166144-135.581696 76.166144-218.012672 0-193.36704-156.752896-350.124032-350.12096-350.124032S85.464064 243.145728 85.464064 436.513792s156.756992 350.124032 350.124032 350.124032c79.875072 0 153.486336-26.766336 212.39808-71.79264l226.04288 226.04288c8.416256 8.416256 19.441664 12.62592 30.470144 12.62592 11.025408 0 22.050816-4.209664 30.466048-12.62592C951.798784 924.055552 951.798784 896.782336 934.966272 879.950848zM165.953536 436.513792c0-148.912128 120.722432-269.630464 269.63456-269.630464S705.21856 287.60064 705.21856 436.513792 584.500224 706.148352 435.588096 706.148352 165.953536 585.42592 165.953536 436.513792z"
            fill="#FFFFFF" p-id="4239"></path>
          <path
            d="M291.135488 302.036992c-8.997888-4.864-20.23424-1.511424-25.097216 7.486464-20.256768 37.476352-32.600064 78.152704-36.688896 120.899584-0.973824 10.181632 6.491136 19.224576 16.672768 20.1984 0.59904 0.057344 1.195008 0.084992 1.784832 0.084992 9.433088 0 17.497088-7.175168 18.413568-16.75776 3.613696-37.7856 14.5152-73.72288 32.401408-106.814464C303.485952 318.138368 300.1344 306.900992 291.135488 302.036992z"
            fill="#FFFFFF" p-id="4240"></path>
        </svg></button>
    </div>

    <!-- 优质农村轮播 -->
    <div class="village-section">
      <h2 class="section-title">优质农村TOP10<i class="iconfont icon-huo" style="color: red;"></i></h2>
      <div class="carousel-container">
        <!-- 轮播图容器 -->
        <div class="carousel-wrapper" ref="villageCarousel">
          <div class="carousel" :style="villageCarouselStyle" @mouseenter="stopVillageAutoPlay"
            @mouseleave="startVillageAutoPlay">
            <div v-for="(village, index) in villageList" :key="village.id" class="carousel-item"
              :class="{ active: index === currentVillageIndex }" :style="getVillageItemStyle(index)"
              @click="navigateToVillageDetail(village.id)">
              <!-- 农村图片 -->
              <div class="village-image">
                <img :src="village.image" :alt="village.name" />
              </div>

              <!-- 农村信息 -->
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

          <!-- 导航按钮 -->
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

        <!-- 指示器 -->
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
        <!-- 轮播图容器 -->
        <div class="carousel-wrapper" ref="carouselWrapper">
          <div class="carousel" :style="carouselStyle" @mouseenter="stopAutoPlay" @mouseleave="startAutoPlay">
            <div v-for="(scenic, index) in scenicList" :key="scenic.id" class="carousel-item"
              :class="{ active: index === currentIndex }" :style="getItemStyle(index)"
              @click="navigateToScenicDetail(scenic.id)">
              <!-- 景点图片 -->
              <div class="scenic-image">
                <img :src="scenic.image" :alt="scenic.name" />
              </div>

              <!-- 景点完整信息（按接口字段展示） -->
              <div class="scenic-info">
                <!-- 名称 + 所属乡村 + 类型 -->
                <div class="scenic-header">
                  <h3 class="scenic-name">{{ scenic.name }}</h3>
                  <div class="scenic-tags">
                    <span class="tag type-tag" v-if="scenic.type">{{ scenic.type }}</span>
                    <span class="tag village-tag" v-if="scenic.villageName">{{ scenic.villageName }}</span>
                  </div>
                </div>

                <!-- 简介 -->
                <p class="scenic-desc" v-if="scenic.intro">{{ scenic.intro }}</p>

                <!-- 价格 + 互动数据 + 住宿信息 -->
                <div class="scenic-bottom">
                  <span class="price-tag" v-if="scenic.price">¥{{ scenic.price }}/人</span>
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

          <!-- 导航按钮 -->
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

        <!-- 指示器 -->
        <div class="carousel-indicators" v-if="scenicList.length > 1">
          <span v-for="(scenic, index) in scenicList" :key="scenic.id" class="indicator"
            :class="{ active: index === currentIndex }" @click="goTo(index)"></span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue';

import { useRouter } from 'vue-router';
import { getTopScenic } from '../api/scenic';
import { getTopVillageByLikes } from '../api/village';
import Location from '../components/Location.vue';

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
const scenicList = ref<Scenic[]>([]);
const villageList = ref<Village[]>([]);

// 轮播图相关
const carouselWrapper = ref<HTMLElement | null>(null);
const currentIndex = ref(0);
const autoPlayTimer = ref<number | null>(null);
const autoPlayInterval = 3000; // 3秒自动轮播

// 农村轮播相关
const villageCarousel = ref<HTMLElement | null>(null);
const currentVillageIndex = ref(0);
const villageAutoPlayTimer = ref<number | null>(null);

const navigateToScenicDetail = (id: number) => {
  router.push(`/scenic/${id}`);
};

const navigateToVillageDetail = (id: number) => {
  router.push(`/village/${id}`);
};

const search = () => {
  // 搜索功能实现
  console.log('搜索:', searchQuery.value);
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

// 轮播图样式计算
const carouselStyle = computed(() => {
  return {
    transform: `translateX(-${currentIndex.value * 100}%)`,
    transition: 'transform 0.5s ease'
  };
});

// 农村轮播图样式计算
const villageCarouselStyle = computed(() => {
  return {
    transform: `translateX(-${currentVillageIndex.value * 100}%)`,
    transition: 'transform 0.5s ease'
  };
});

// 获取每个轮播项的样式（3D效果）
const getItemStyle = (index: number) => {
  const distance = Math.abs(index - currentIndex.value);
  if (distance > 2) return { opacity: 0, transform: 'scale(0.8)' };

  let opacity = 1;
  let scale = 1;
  let zIndex = scenicList.value.length - distance;

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
    zIndex,
    transition: 'all 0.5s ease'
  };
};

// 获取农村轮播项的样式（3D效果）
const getVillageItemStyle = (index: number) => {
  const distance = Math.abs(index - currentVillageIndex.value);
  if (distance > 2) return { opacity: 0, transform: 'scale(0.8)' };

  let opacity = 1;
  let scale = 1;
  let zIndex = villageList.value.length - distance;

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
    zIndex,
    transition: 'all 0.5s ease'
  };
};

// 上一张
const prev = () => {
  currentIndex.value = (currentIndex.value - 1 + scenicList.value.length) % scenicList.value.length;
  // 暂停自动轮播5秒
  pauseAutoPlay();
};

// 下一张
const next = () => {
  currentIndex.value = (currentIndex.value + 1) % scenicList.value.length;
  // 暂停自动轮播5秒
  pauseAutoPlay();
};

// 农村轮播上一张
const prevVillage = () => {
  currentVillageIndex.value = (currentVillageIndex.value - 1 + villageList.value.length) % villageList.value.length;
  // 暂停农村轮播5秒
  pauseVillageAutoPlay();
};

// 农村轮播下一张
const nextVillage = () => {
  currentVillageIndex.value = (currentVillageIndex.value + 1) % villageList.value.length;
  // 暂停农村轮播5秒
  pauseVillageAutoPlay();
};

// 跳转到指定索引
const goTo = (index: number) => {
  currentIndex.value = index;
  // 暂停自动轮播5秒
  pauseAutoPlay();
};

// 跳转到农村轮播指定索引
const goToVillage = (index: number) => {
  currentVillageIndex.value = index;
  // 暂停农村轮播5秒
  pauseVillageAutoPlay();
};

// 开始自动轮播
const startAutoPlay = () => {
  if (autoPlayTimer.value) {
    clearInterval(autoPlayTimer.value);
  }
  autoPlayTimer.value = setInterval(() => {
    next();
  }, autoPlayInterval);
};

// 停止自动轮播
const stopAutoPlay = () => {
  if (autoPlayTimer.value) {
    clearInterval(autoPlayTimer.value);
    autoPlayTimer.value = null;
  }
};

// 暂停自动轮播5秒
const pauseAutoPlay = () => {
  stopAutoPlay();
  // 5秒后重新开始自动轮播
  setTimeout(() => {
    startAutoPlay();
  }, 5000);
};

// 开始农村轮播
const startVillageAutoPlay = () => {
  if (villageAutoPlayTimer.value) return;
  villageAutoPlayTimer.value = setInterval(() => {
    nextVillage();
  }, autoPlayInterval);
};

// 停止农村轮播
const stopVillageAutoPlay = () => {
  if (villageAutoPlayTimer.value) {
    clearInterval(villageAutoPlayTimer.value);
    villageAutoPlayTimer.value = null;
  }
};

// 暂停农村轮播5秒
const pauseVillageAutoPlay = () => {
  stopVillageAutoPlay();
  // 5秒后重新开始自动轮播
  setTimeout(() => {
    startVillageAutoPlay();
  }, 5000);
};

onMounted(() => {
  fetchScenicList();
  fetchVillageList();
  startAutoPlay();
  startVillageAutoPlay();
});

onUnmounted(() => {
  stopAutoPlay();
  stopVillageAutoPlay();
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
  margin: 16px;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
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

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #8BC34A;
  margin: 24px 16px 16px;
}

.ai-cards {
  display: flex;
  gap: 12px;
  padding: 0 16px;
  margin-bottom: 24px;
  overflow-x: auto;
  scrollbar-width: none;
}

.ai-cards::-webkit-scrollbar {
  display: none;
}

.ai-card {
  flex: 0 0 120px;
  background: white;
  border-radius: 12px;
  padding: 16px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  cursor: pointer;
}

.village-image {
  width: 100%;
  height: 200px;
  overflow: hidden;
}

.village-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.carousel-item:hover .village-image img {
  transform: scale(1.05);
}

.village-info {
  padding: 16px;
  height: calc(100% - 200px);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.village-name {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #333;
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
  flex: 1;
}

.village-meta {
  display: flex;
  gap: 16px;
  font-size: 14px;
  color: #999;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.carousel-container {
  padding: 0 16px 24px;
  position: relative;
}

.carousel-wrapper {
  position: relative;
  overflow: hidden;
  border-radius: 12px;
  height: 400px;
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
}

.carousel-item.active {
  z-index: 10;
}

.carousel-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
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
  left: 10px;
}

.carousel-btn.next {
  right: 10px;
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
  margin-top: 16px;
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

.scenic-image {
  width: 100%;
  height: 200px;
  overflow: hidden;
}

.scenic-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.carousel-item:hover .scenic-image img {
  transform: scale(1.05);
}

.scenic-info {
  padding: 16px;
  height: calc(100% - 200px);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.scenic-name {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #333;
}

.scenic-desc {
  font-size: 14px;
  color: #666;
  margin-bottom: 12px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.scenic-stats {
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
  .home-header {
    padding: 12px 16px;
  }

  .header-title {
    font-size: 18px;
  }

  .search-bar {
    margin: 12px 16px;
  }

  .section-title {
    margin: 20px 16px 12px;
  }

  .ai-card {
    flex: 0 0 100px;
    padding: 12px;
  }

  .ai-icon {
    font-size: 28px;
  }

  .ai-card h3 {
    font-size: 13px;
  }

  .ai-card p {
    font-size: 11px;
  }

  .scenic-image {
    height: 160px;
  }

  .scenic-info {
    padding: 12px;
  }

  .scenic-name {
    font-size: 15px;
  }

  .scenic-desc {
    font-size: 13px;
  }

  .carousel-wrapper {
    height: 350px;
  }

  .scenic-image {
    height: 180px;
  }

  .scenic-info {
    height: calc(100% - 180px);
    padding: 12px;
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

/* 新增：景点头部布局 */
.scenic-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
  gap: 8px;
}

/* 新增：标签样式 */
.scenic-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
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

/* 新增：底部布局 */
.scenic-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.price-tag {
  font-size: 16px;
  font-weight: 600;
  color: #FF5722;
}

.feature-label{
  font-size: 18px;
  color: #8be6bd;
}

.feature-value{
  font-size: 18px;
  color: #6f6f6f;
}

i {
  font-size: 24px;
  margin-left: 8px;
}
</style>