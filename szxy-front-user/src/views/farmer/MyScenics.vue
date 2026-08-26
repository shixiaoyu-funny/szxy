<template>
  <div class="my-scenics-container">
    <div class="list-toolbar">
      <el-button type="primary" @click="addScenic">＋ 新增景点</el-button>
    </div>

    <div v-if="loading" class="loading-container">
      <div class="loading"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="error" class="error-container">
      <p>{{ error }}</p>
      <button class="btn btn-primary" @click="fetchScenics">重试</button>
    </div>

    <div v-else-if="scenics.length === 0" class="empty-container">
      <p>暂无景点，点击上方「新增景点」</p>
    </div>

    <div v-else class="scenics-list">
      <div v-for="item in scenics" :key="item.id" class="scenic-item" @click="goDetail(item.id)">
        <div class="item-image">
          <img :src="getMainImage(item.image)" :alt="item.name" />
        </div>
        <div class="item-info">
          <h3 class="item-name">{{ item.name }}</h3>
          <p class="item-intro">{{ item.intro }}</p>
          <p class="item-price">{{ formatScenicPrice(item.price, { empty: '—' }) }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getMyScenics } from '../../api/farmer';
import { formatScenicPrice } from '../../utils/scenicPrice';

interface ScenicVO {
  id: number;
  name: string;
  intro?: string;
  image?: string;
  price?: number;
}

const router = useRouter();
const scenics = ref<ScenicVO[]>([]);
const loading = ref(true);
const error = ref('');

const goBack = () => router.back();
const addScenic = () => router.push('/farmer/scenic/add');

const getMainImage = (imageStr: string | undefined) => {
  if (!imageStr) return '';
  return imageStr.split(',')[0];
};

const goDetail = (id: number) => {
  router.push(`/scenic/${id}`);
};

const fetchScenics = async () => {
  loading.value = true;
  error.value = '';
  try {
    const res = await getMyScenics();
    scenics.value = res.data;
  } catch (e: any) {
    error.value = e?.response?.data?.message || '获取失败';
  } finally {
    loading.value = false;
  }
};

onMounted(fetchScenics);
</script>

<style scoped>
.my-scenics-container {
  min-height: 100vh;
  background: transparent;
}

.list-toolbar {
  padding: 0 16px 12px;
}

.my-header {
  background: linear-gradient(-45deg, #11998e, #38ef7d, #11998e, #38ef7d);
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.back-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #fff;
  cursor: pointer;
}

.add-btn {
  background: rgba(255, 255, 255, 0.25);
  border: none;
  color: #fff;
  font-size: 22px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  cursor: pointer;
}

.header-title {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  margin: 0;
}

.scenics-list {
  padding: 16px;
}

.scenic-item {
  display: flex;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  margin-bottom: 12px;
  cursor: pointer;
}

.item-image {
  width: 110px;
  height: 90px;
  flex-shrink: 0;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-info {
  flex: 1;
  padding: 12px;
}

.item-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
}

.item-intro {
  font-size: 13px;
  color: #666;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-price {
  font-size: 14px;
  color: #ff5722;
  font-weight: 600;
}

.loading-container,
.error-container,
.empty-container {
  text-align: center;
  padding: 60px 20px;
  color: #666;
}
</style>
