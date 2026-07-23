<template>
  <div class="attraction-manage-container">
    <header class="page-header">
      <div class="header-content">
        <h1 class="header-title">景点管理</h1>
        <router-link to="/access" class="apply-btn">景点申请</router-link>
      </div>
    </header>
    <div class="content">
      <div v-if="scenicList.length === 0" class="empty">
        <p>暂无景点信息</p>
      </div>
      <div v-else class="scenic-list">
        <div v-for="scenic in scenicList" :key="scenic.id" class="scenic-item">
          <!-- 景区图片 -->
          <div v-if="scenic.image" class="scenic-image">
            <img :src="scenic.image.split(',')[0]" :alt="scenic.name" class="image" />
          </div>
          
          <div class="scenic-info">
            <h3 class="scenic-name">{{ scenic.name || '未命名景点' }}</h3>
            <p class="scenic-intro">{{ scenic.intro || '暂无介绍' }}</p>
            
            <div class="scenic-meta">
              <div class="meta-row">
                <span class="meta-label">关联村落：</span>
                <span class="meta-value">{{ scenic.villageName || scenic.village_name || '未设置' }}</span>
              </div>
              <div class="meta-row">
                <span class="meta-label">景区类型：</span>
                <span class="meta-value">{{ getScenicTypeText(scenic.type) }}</span>
              </div>
              <div class="meta-row">
                <span class="meta-label">门票价格：</span>
                <span class="meta-value">{{ scenic.price === 0 ? '免费' : `${scenic.price || 0} 元` }}</span>
              </div>
              <div class="meta-row">
                <span class="meta-label">收藏/点赞：</span>
                <span class="meta-value">{{ scenic.collections || 0 }} / {{ scenic.likes || 0 }}</span>
              </div>
              <div class="meta-row">
                <span class="meta-label">是否提供住宿：</span>
                <span class="meta-value">{{ scenic.hasAccommodation === 1 ? '是' : '否' }}</span>
              </div>
              <div v-if="scenic.accommodationInfo" class="meta-row">
                <span class="meta-label">住宿信息：</span>
                <span class="meta-value">{{ scenic.accommodationInfo }}</span>
              </div>
            </div>
            <button
              type="button"
              class="toggle-comments-btn"
              @click="toggleComments(scenic.id)"
            >
              {{ expandedScenicId === scenic.id ? '收起评论与热词' : '查看评论与热词' }}
            </button>
            <ScenicCommentWordCloud
              v-if="expandedScenicId === scenic.id"
              :scenic-id="scenic.id"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { getScenicInfo } from '../api/user';
import { ElLoading } from 'element-plus';
import { normalizeScenicVO } from '../utils/scenicApi';
import ScenicCommentWordCloud from '../components/ScenicCommentWordCloud.vue';

const expandedScenicId = ref<number | null>(null);

function toggleComments(id: number) {
  expandedScenicId.value = expandedScenicId.value === id ? null : id;
}

const error = ref('');
const scenicList = ref<any[]>([]);

const getScenicTypeText = (type: number) => {
  switch (type) {
    case 1:
      return '自然景观';
    case 2:
      return '人文景观';
    case 3:
      return '娱乐体验';
    case 4:
      return '民俗体验';
    default:
      return '未设置';
  }
};

const loadScenicInfo = async () => {
  const loadingInstance = ElLoading.service({
    lock: true,
    text: '加载中...',
    background: 'rgba(0, 0, 0, 0.7)',
  });
  
  error.value = '';
  try {
    const res = await getScenicInfo();
    const list = res.data || [];
    scenicList.value = Array.isArray(list) ? list.map((s) => normalizeScenicVO(s as Record<string, unknown>)) : [];
  } catch (err: any) {
    error.value = '获取景点信息失败，请重试';
    console.error('获取景点信息失败:', err);
  } finally {
    loadingInstance.close();
  }
};

onMounted(() => {
  loadScenicInfo();
});
</script>

<style scoped>
.attraction-manage-container {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 80px;
}

.page-header {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  padding: 16px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  max-width: 1200px;
  margin: 0 auto;
}

.header-title {
  color: white;
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.apply-btn {
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  text-decoration: none;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  transition: background-color 0.3s;
}

.apply-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.content {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.error {
  text-align: center;
  padding: 40px 0;
  color: #f44336;
}

.error button {
  margin-top: 16px;
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  background: #11998e;
  color: white;
  cursor: pointer;
}

.empty {
  text-align: center;
  padding: 40px 0;
  color: #999;
  font-size: 14px;
}

.scenic-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.scenic-item {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.scenic-image {
  width: 100%;
  height: 200px;
  overflow: hidden;
}

.scenic-image .image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.scenic-info {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.scenic-name {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  color: #333;
}

.scenic-intro {
  font-size: 14px;
  color: #666;
  margin: 0;
  line-height: 1.4;
}

.toggle-comments-btn {
  margin-top: 14px;
  padding: 8px 14px;
  font-size: 14px;
  color: #2e7d32;
  background: #f1f8e9;
  border: 1px solid #c5e1a5;
  border-radius: 8px;
  cursor: pointer;
  width: 100%;
  max-width: 280px;
}

.toggle-comments-btn:hover {
  background: #e8f5e9;
}

.scenic-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.meta-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.meta-label {
  font-size: 14px;
  font-weight: 500;
  color: #666;
  min-width: 100px;
}

.meta-value {
  font-size: 14px;
  color: #333;
  flex: 1;
  line-height: 1.4;
}
</style>