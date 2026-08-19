<template>
  <div class="scenic-detail-container">
    <header class="scenic-header">
      <button type="button" class="back-btn" @click="goBack">←</button>
      <h1 class="header-title">景点详情</h1>
    </header>

    <div v-if="loading" class="loading-container">
      <div class="loading"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="scenicInfo" class="scenic-content">
      <!-- 景点图片 -->
      <div class="scenic-image">
        <img :src="getMainImage(scenicInfo.image)" :alt="scenicInfo.name" />
      </div>

      <!-- 景点信息 -->
      <div class="scenic-info">
        <h2 class="scenic-name">{{ scenicInfo.name }}</h2>
        <div class="scenic-meta">
          <span class="meta-item">📍 {{ scenicInfo.village_name }}</span>
          <span class="meta-item">🎫 {{ scenicInfo.price === 0 ? '免费' : '¥' + scenicInfo.price }}</span>
          <span class="meta-item">🏷️ {{ getScenicType(scenicInfo.type) }}</span>
        </div>
        <p class="scenic-desc">{{ scenicInfo.intro }}</p>

        <!-- 住宿信息 -->
        <div v-if="scenicInfo.has_accommodation === 1 && scenicInfo.accommodation_info" class="accommodation-info">
          <h3 class="info-title">住宿信息</h3>
          <p class="accommodation-text">{{ scenicInfo.accommodation_info }}</p>
        </div>

        <!-- 操作按钮：新增disabled防重复点击 -->
        <div class="action-buttons">
          <button type="button" class="action-btn" :class="{ active: isLiked }" @click="toggleLike"
            :disabled="likeLoading">
            <span class="btn-icon">👍</span>
            <span>{{ isLiked ? '已点赞' : '点赞' }}</span>
            <span class="btn-count">{{ scenicInfo.likes }}</span>
          </button>
          <button type="button" class="action-btn" :class="{ active: isCollected }" @click="toggleCollect"
            :disabled="collectLoading">
            <span class="btn-icon">⭐</span>
            <span>{{ isCollected ? '已收藏' : '收藏' }}</span>
            <span class="btn-count">{{ scenicInfo.collections }}</span>
          </button>
        </div>
      </div>

      <!-- 评论区：上方为评论热词 TOP10（原生进度条） -->
      <div class="comment-section">
        <h3 class="section-title">评论</h3>

        <div v-if="comments.length > 0" class="comment-rank-section">
          <h4 class="comment-rank-heading">评论热词 TOP10</h4>
          <ul
            v-if="hasCommentRank"
            class="comment-rank-list"
            :style="{ fontFamily: WORD_CLOUD_FONT_FAMILY }"
          >
            <li v-for="(row, index) in commentRankRows" :key="row.name" class="comment-rank-item">
              <span class="comment-rank-badge">{{ index + 1 }}</span>
              <span class="comment-rank-name" :title="row.name">{{ row.name }}</span>
              <div class="comment-rank-track" aria-hidden="true">
                <div class="comment-rank-fill" :style="commentRankBarStyle(row)" />
              </div>
              <div class="comment-rank-count">
                <span class="comment-rank-count-num">{{ row.value }}</span>
                <span class="comment-rank-count-unit">次</span>
              </div>
            </li>
          </ul>
          <p v-show="!hasCommentRank" class="comment-rank-hint">
            暂无足够有效词语生成排行，试试写更具体的游玩感受吧～
          </p>
        </div>

        <div class="comment-input-container">
          <div class="rating-container">
            <span class="rating-label">评分：</span>
            <div class="stars">
              <span v-for="star in 5" :key="star" class="star" :class="{ active: selectedRating >= star }"
                @click="selectedRating = star">
                ★
              </span>
            </div>
          </div>
          <input type="text" v-model="commentContent" class="input" placeholder="写下您的评论..." @keyup.enter="submitComment"
            :disabled="submitting" />
          <div class="image-upload">
            <input type="file" multiple accept="image/*" class="file-input" id="image-upload"
              @change.prevent="handleImageUpload" :disabled="submitting" />
            <label class="upload-btn" for="image-upload">
              📷 上传图片（图片大小≤5M）
            </label>
            <div v-if="commentPreviewUrls.length > 0" class="comment-preview-wrapper">
              <div class="preview-list">
                <div v-for="(url, index) in commentPreviewUrls" :key="index" class="preview-item">
                  <img :src="url" class="preview-img" alt="预览" />
                  <button type="button" class="del-preview-btn" @click="removePreviewImage(index)">×</button>
                </div>
              </div>
            </div>
          </div>
          <button type="button" class="btn btn-primary submit-btn" @click="submitComment"
            :disabled="!commentContent.trim() || submitting || selectedRating === 0">
            {{ submitting ? '发布中...' : '发布' }}
          </button>
        </div>
        <div class="comment-list">
          <div v-for="comment in comments" :key="comment.id" class="comment-item">
            <div class="comment-content">
              <div class="comment-header">
                <div class="comment-user">用户：{{ comment.username || '匿名用户' }}</div>
              </div>
              <p class="comment-text">{{ comment.content }}</p>
              <div v-if="comment.comment_img" class="comment-images">
                <img v-for="(img, index) in comment.comment_img.split(',')" :key="index" :src="img"
                  alt="评论图片" class="comment-image" />
              </div>
              <div v-if="comment.score" class="comment-rating">
                <span v-for="star in 5" :key="star" class="star" :class="{ active: comment.score >= star }">
                  ★
                </span>
              </div>
              <div class="comment-time">评论时间：{{ comment.create_time || '未知时间' }}</div>
            </div>
          </div>
          <div v-if="comments.length === 0" class="no-comments">
            暂无评论，快来发表第一条评论吧！
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useUserStore } from '../stores/user';
import { userLike, userCollect, userComment, isLike, isCollect } from '../api/user';
import { getScenicDetail, getScComments } from '../api/scenic';
import { uploadFile } from '../api/upload';
import {
  type CommentRankRow,
  WORD_CLOUD_FONT_FAMILY,
  commentRankBarPercent,
  getCommentRankRows,
  wordColorByName
} from '../utils/commentWordCloud';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const scenicId = ref(Number(route.params.id));
const scenicInfo = ref<any>(null);
const comments = ref<any[]>([]);
const commentContent = ref('');
const selectedRating = ref(0);
const selectedImages = ref<File[]>([]);
const commentPreviewUrls = ref<string[]>([]);
// 核心状态：严格同步服务端0/1
const isLiked = ref(false);
const isCollected = ref(false);
// 提交/操作loading
const submitting = ref(false);
const loading = ref(true);
// 🔥 新增：点赞/收藏防重复点击loading
const likeLoading = ref(false);
const collectLoading = ref(false);

const commentRankRows = computed(() => getCommentRankRows(comments.value));
const commentRankMax = computed(() => commentRankRows.value[0]?.value ?? 1);
const hasCommentRank = computed(() => commentRankRows.value.length > 0);

function commentRankBarStyle(row: CommentRankRow) {
  const pct = commentRankBarPercent(row.value, commentRankMax.value);
  return {
    width: `${pct}%`,
    backgroundColor: wordColorByName(row.name)
  };
}

const goBack = () => {
  router.back();
};

const fetchScenicDetail = async () => {
  loading.value = true;
  try {
    const res = await getScenicDetail(scenicId.value);
    scenicInfo.value = res.data;
    console.log('景点详情：', scenicInfo.value);
    // 🔥 强制等待：先拿景点详情，再同步点赞/收藏真实状态
    await fetchUserStatus();
    // 获取景点评论
    await fetchComments();
  } catch (err) {
    ElMessage.error('获取景点详情失败，请重试');
    console.error('获取景点详情失败:', err);
  } finally {
    loading.value = false;
  }
};

// 获取景点评论
const fetchComments = async () => {
  try {
    const res = await getScComments(scenicId.value);
    console.log('评论接口返回：', res);
    comments.value = res.data || [];
  } catch (err) {
    console.error('获取评论失败:', err);
    comments.value = [];
  }
};

// 🔥 修复：强同步服务端点赞/收藏状态（刷新页面必执行）
const fetchUserStatus = async () => {
  try {
    // 并行请求，提升速度
    const [likeRes, collectRes] = await Promise.all([
      isLike(scenicId.value),
      isCollect(scenicId.value)
    ]);
    // 严格按照服务端返回：0=未操作，1=已操作
    isLiked.value = likeRes.data === true;
    isCollected.value = collectRes.data === true;
    console.log('✅ 服务端同步状态：', '点赞=', isLiked.value, '收藏=', isCollected.value);
  } catch (err) {
    console.error('❌ 获取用户状态失败:', err);
    // 失败时清空状态，避免错误显示
    isLiked.value = false;
    isCollected.value = false;
  }
};

// 🔥 修复：点赞逻辑（防重复+先调接口再更新状态）
const toggleLike = async () => {
  if (likeLoading.value) return; // 防重复点击
  likeLoading.value = true;

  try {
    // 1. 先调用服务端接口（核心：以服务端为准）
    await userLike(scenicId.value);
    // 2. 接口成功后，翻转前端状态+更新计数
    isLiked.value = !isLiked.value;
    scenicInfo.value.likes += isLiked.value ? 1 : -1;
    ElMessage.success(isLiked.value ? '点赞成功' : '取消点赞成功');
  } catch (error) {
    console.error('点赞失败：', error);
    // 3. 接口失败：不更新状态，保持和服务端一致
    ElMessage.error('操作失败，请重试');
  } finally {
    likeLoading.value = false;
  }
};

// 🔥 修复：收藏逻辑（和点赞完全对齐，杜绝数据不一致）
const toggleCollect = async () => {
  if (collectLoading.value) return; // 防重复点击
  collectLoading.value = true;

  try {
    // 1. 先调用服务端接口
    await userCollect(scenicId.value);
    // 2. 接口成功后，翻转状态+更新计数
    isCollected.value = !isCollected.value;
    scenicInfo.value.collections += isCollected.value ? 1 : -1;
    ElMessage.success(isCollected.value ? '收藏成功' : '取消收藏成功');
  } catch (error) {
    console.error('收藏失败：', error);
    // 3. 接口失败：不更新状态
    ElMessage.error('操作失败，请重试');
  } finally {
    collectLoading.value = false;
  }
};

const getMainImage = (imageStr: string) => {
  if (!imageStr) return 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=default%20scenic%20spot&image_size=landscape_16_9';
  return imageStr.split(',')[0] || '';
};

const getScenicType = (type: number) => {
  const typeMap = { 1: '自然景观', 2: '人文景观', 3: '娱乐体验', 4: '民俗体验' };
  return typeMap[type as keyof typeof typeMap] || '未知类型';
};

const handleImageUpload = (event: Event) => {
  event.preventDefault();
  event.stopPropagation();
  const target = event.target as HTMLInputElement;
  if (!target.files?.length) return;

  const newFiles = Array.from(target.files);
  selectedImages.value = [...selectedImages.value, ...newFiles];
  newFiles.forEach(file => {
    commentPreviewUrls.value.push(URL.createObjectURL(file));
  });
  target.value = '';
};

const handleImgError = (e: Event) => {
  (e.target as HTMLImageElement).src = 'https://via.placeholder.com/80?text=图片加载失败';
};

const removePreviewImage = (index: number) => {
  URL.revokeObjectURL(commentPreviewUrls.value[index] || '');
  commentPreviewUrls.value.splice(index, 1);
  selectedImages.value.splice(index, 1);
};

const submitComment = async () => {
  if (!commentContent.value.trim() || selectedRating.value === 0) return;

  submitting.value = true;
  try {
    let commentImg = '';
    if (selectedImages.value.length > 0) {
      const uploadPromises = selectedImages.value.map(file => uploadFile(file));
      const uploadResults = await Promise.all(uploadPromises);
      const imgUrls = uploadResults.map(res => {
        return typeof res === 'string' ? res : (res as any).url || (res as any).data || '';
      });
      commentImg = imgUrls.join(',');
    }

    const commentData = {
      userId: userStore.userInfo?.id || 1,
      targetId: scenicId.value,
      targetType: 1,
      content: commentContent.value,
      score: selectedRating.value,
      commentImg: commentImg,
      isShow: 1
    };

    const response = await userComment(commentData);
    const username = response.data || userStore.userInfo?.username || '用户';

    comments.value.unshift({
      id: Date.now(),
      username,
      content: commentContent.value,
      score: selectedRating.value,
      comment_img: commentData.commentImg, // ✅ 统一用下划线
      create_time: new Date().toLocaleString('zh-CN'),
      isShow: 1
    });

    commentContent.value = '';
    selectedRating.value = 0;
    selectedImages.value.forEach((_, index) => {
      URL.revokeObjectURL(commentPreviewUrls.value[index] || '');
    });
    selectedImages.value = [];
    commentPreviewUrls.value = [];

    ElMessage.success('评论发布成功！');
  } catch (error) {
    console.error('评论失败:', error);
    ElMessage.error('评论失败，请重试');
  } finally {
    submitting.value = false;
  }
};

onUnmounted(() => {
  commentPreviewUrls.value.forEach(url => URL.revokeObjectURL(url));
});

onMounted(() => {
  if (!userStore.isLoggedIn) {
    router.push('/login');
    return;
  }
  // 刷新页面→自动加载详情+同步点赞收藏状态
  fetchScenicDetail();
});
</script>

<style scoped>
/* 样式无修改，完全保留原有效果 */
.scenic-detail-container {
  min-height: 100vh;
  background: transparent;
}

.scenic-header {
  display: flex;
  align-items: center;
  padding: 16px;
  background: white;
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
  font-size: 18px;
  font-weight: 600;
  color: #2E7D32;
  flex: 1;
}

.scenic-content {
  padding-bottom: 24px;
}

.scenic-image {
  width: 100%;
  height: 240px;
  overflow: hidden;
}

.scenic-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.scenic-info {
  background: white;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.scenic-name {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #333;
}

.scenic-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
  font-size: 14px;
  color: #666;
}

.scenic-desc {
  font-size: 14px;
  line-height: 1.6;
  color: #333;
  margin-bottom: 16px;
}

.accommodation-info {
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

.accommodation-text {
  font-size: 14px;
  line-height: 1.6;
  color: #666;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.action-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: white;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
}

.action-btn:hover {
  background: #f0f9e8;
  transform: translateY(-2px);
}

.action-btn.active {
  background: #8BC34A;
  color: white;
  border-color: #8BC34A;
}

/* 按钮禁用样式：防重复点击视觉反馈 */
.action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-icon {
  font-size: 16px;
}

.btn-count {
  font-size: 12px;
  opacity: 0.8;
}

.comment-section {
  background: white;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
  color: #333;
}

.comment-rank-section {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.comment-rank-heading {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 10px;
  color: #2e7d32;
}

.comment-rank-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
  box-sizing: border-box;
  -webkit-tap-highlight-color: transparent;
}

.comment-rank-item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 10px 12px;
  border-radius: 14px;
  background: linear-gradient(145deg, #f8faf8 0%, #f0f4f1 100%);
  box-shadow: 0 2px 10px rgba(46, 125, 50, 0.06);
  border: 1px solid rgba(46, 125, 50, 0.08);
}

.comment-rank-badge {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  color: #2e7d32;
  background: rgba(139, 195, 74, 0.22);
}

.comment-rank-name {
  flex: 0 1 26%;
  min-width: 0;
  font-size: 13px;
  font-weight: 600;
  color: #2d3e35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.comment-rank-track {
  flex: 1;
  min-width: 0;
  height: 10px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.06);
  overflow: hidden;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.04);
}

.comment-rank-fill {
  height: 100%;
  border-radius: 999px;
  min-width: 4px;
  transition: width 0.45s cubic-bezier(0.22, 1, 0.36, 1);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.12);
}

.comment-rank-count {
  flex-shrink: 0;
  display: flex;
  align-items: baseline;
  gap: 2px;
  min-width: 3.2em;
  justify-content: flex-end;
}

.comment-rank-count-num {
  font-size: 14px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: #37474f;
}

.comment-rank-count-unit {
  font-size: 11px;
  font-weight: 500;
  color: #78909c;
}

@media (max-width: 380px) {
  .comment-rank-name {
    flex-basis: 22%;
    font-size: 12px;
  }

  .comment-rank-item {
    padding: 9px 10px;
    gap: 6px;
  }
}

.comment-rank-hint {
  font-size: 13px;
  color: #888;
  margin: 0;
  line-height: 1.5;
}

.comment-input-container {
  margin-bottom: 20px;
}

.rating-container {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.rating-label {
  font-size: 14px;
  color: #333;
  margin-right: 8px;
}

.stars {
  display: flex;
  gap: 4px;
}

.star {
  font-size: 20px;
  color: #e0e0e0;
  cursor: pointer;
  transition: color 0.3s ease;
}

.star.active {
  color: #FFC107;
}

.image-upload {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 12px 0;
}

.file-input {
  display: none;
}

.upload-btn {
  padding: 8px 12px;
  background: #f5f5f5;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
  width: fit-content;
}

.upload-btn:hover {
  background: #e0e0e0;
}

.comment-preview-wrapper {
  width: 100%;
}

.preview-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.preview-item {
  position: relative;
  width: 60px;
  height: 60px;
  border-radius: 4px;
  overflow: hidden;
}

.preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.del-preview-btn {
  position: absolute;
  top: 0;
  right: 0;
  width: 18px;
  height: 18px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border: none;
  border-radius: 50%;
  font-size: 12px;
  line-height: 18px;
  text-align: center;
  cursor: pointer;
  padding: 0;
}

.comment-input-container input {
  width: 100%;
  padding: 10px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  margin-bottom: 12px;
}

.submit-btn {
  white-space: nowrap;
  padding: 0 16px;
  height: 36px;
  background: #8BC34A;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.submit-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.comment-rating {
  display: flex;
  gap: 2px;
  margin: 4px 0;
  margin-left: 280px;
}

.comment-rating .star {
  font-size: 14px;
  cursor: default;
}

.comment-images {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.comment-content{
  margin-bottom: 8px;
  padding: 12px;
  padding-left: 0;
  width: 400px;
  border-radius: 8px;
  background-color: #93f8a788;
  border-bottom: 1px solid #a7a7a7;
}

.comment-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
  margin-left: 12px;
}

.comment-time{
  font-size: 12px;
  color: #666;
  margin-left: 12px;
}

.comment-user{
  width: 50%;
  font-size: 16px;
  border-radius: 0 8px 8px 0;
  background-color: #3cfcffcd;
  border-bottom: 1px solid #6d6d6d;
  padding: 4px 8px;
  margin-right: 8px;
}

.comment-text{
  font-size: 14px;
  line-height: 1.6;
  color: #333;
  margin-left: 12px;
}

@media (max-width: 480px) {
  .comment-image {
    width: 60px;
    height: 60px;
  }

  .preview-item {
    width: 50px;
    height: 50px;
  }
}

.comment-list {
  max-height: 300px;
  overflow-y: auto;
}

.comment-item {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 16px
}
</style>
