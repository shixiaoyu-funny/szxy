<template>
  <div class="my-comments-container">
    <PageLoadingOverlay :visible="!pageReady" />
    <div v-show="pageReady">
    <div v-if="error" class="error-container">
      <p>{{ error }}</p>
      <button class="btn btn-primary" @click="fetchComments">重试</button>
    </div>

    <div v-else-if="comments.length === 0" class="empty-container">
      <p>暂无评论，快去评论喜欢的景点吧！</p>
    </div>

    <div v-else class="comments-list">
      <div 
        v-for="item in comments" 
        :key="item.id" 
        class="comment-item"
        @click="navigateToDetail(item.id)"
      >
        <div class="item-image">
          <img :src="getMainImage(item.image)" :alt="item.name" />
        </div>
        <div class="item-info">
          <h3 class="item-name">{{ item.name }}</h3>
          <p v-if="item.villageName" class="item-village">{{ item.villageName }}</p>
          <p v-if="hasScenicPrice(item.price)" class="item-price">
            {{ formatScenicPrice(item.price) }}
          </p>
          <div class="item-stats">
            <span class="stat-item">❤️ {{ item.likes || 0 }}</span>
            <span class="stat-item">⭐ {{ item.collections || 0 }}</span>
          </div>
        </div>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getComments } from '../api/profile';
import PageLoadingOverlay from '../components/PageLoadingOverlay.vue';
import { collectImageUrlsFromItems, preloadImages } from '../utils/preloadImages';
import { formatScenicPrice, hasScenicPrice } from '../utils/scenicPrice';

interface ScenicVO {
  id: number;
  name: string;
  villageName?: string;
  intro?: string;
  image?: string;
  price?: number;
  type?: number;
  collections?: number;
  likes?: number;
  hasAccommodation?: number;
  accommodationInfo?: string;
}

const router = useRouter();
const comments = ref<ScenicVO[]>([]);
const pageReady = ref(false);
const error = ref('');

const goBack = () => {
  router.back();
};

const getMainImage = (imageStr: string | undefined) => {
  if (!imageStr) return '';
  return imageStr.split(',')[0];
};

const navigateToDetail = (scenicId: number) => {
  router.push(`/scenic/${scenicId}`);
};

const fetchComments = async () => {
  pageReady.value = false;
  error.value = '';
  try {
    const res = await getComments();
    console.log(res.data);
    if (res.data) {
      comments.value = res.data;
      await preloadImages(collectImageUrlsFromItems(comments.value));
      ElMessage.success('获取历史评论景点列表成功');
    }
  } catch (err) {
    console.error('获取历史评论景点列表失败:', err);
    error.value = '获取历史评论景点列表失败，请重试';
    ElMessage.error('获取历史评论景点列表失败，请重试');
  } finally {
    pageReady.value = true;
  }
};



onMounted(() => {
  fetchComments();
});
</script>

<style scoped>
.my-comments-container {
  min-height: 100vh;
  background: transparent;
}

.my-header {
  margin: 5px;
  background: linear-gradient(-45deg, #11998e, #38ef7d, #11998e, #38ef7d);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.back-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  transition: all 0.3s ease;
}

.back-btn:hover {
  background: #f0f9e8;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
  margin: 0;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: white;
  border-radius: 12px;
  margin: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: white;
  border-radius: 12px;
  margin: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.empty-container {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
  margin: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  color: #999;
  font-size: 16px;
}

.comments-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  padding: 16px;
}

.comment-item {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.3s ease;
}

.comment-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.item-image {
  width: 100%;
  height: 200px;
  overflow: hidden;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-info {
  padding: 16px;
}

.item-name {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
}

.item-village {
  font-size: 14px;
  color: #666;
  margin: 0 0 8px 0;
}

.item-price {
  font-size: 16px;
  font-weight: 600;
  color: #8BC34A;
  margin: 0 0 12px 0;
}

.item-stats {
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
  .comments-list {
    grid-template-columns: 1fr;
    padding: 12px;
  }

  .item-image {
    height: 180px;
  }
}
</style>
