<template>
  <div class="scenic-detail-container">
    <PageLoadingOverlay :visible="!pageReady" />
    <div v-if="pageReady && loadError && !scenicInfo" class="load-error">
      <p>{{ loadError }}</p>
      <button type="button" class="btn btn-primary" @click="fetchScenicDetail">重试</button>
    </div>
    <div v-if="pageReady && scenicInfo" class="scenic-content">
      <!-- 景点图片 -->
      <div class="scenic-image">
        <img :src="getMainImage(scenicInfo.image)" :alt="scenicInfo.name" />
      </div>

      <!-- 景点信息 -->
      <div class="scenic-info">
        <h2 class="scenic-name">{{ scenicInfo.name }}</h2>
        <div class="scenic-meta">
          <span class="meta-item">📍 {{ scenicInfo.villageName }}</span>
          <span v-if="hasScenicPrice(scenicInfo.price)" class="meta-item">
            🎫 {{ formatScenicPrice(scenicInfo.price) }}
          </span>
          <span class="meta-item">🏷️ {{ getScenicType(scenicInfo.type) }}</span>
        </div>
        <p class="scenic-desc">{{ scenicInfo.intro }}</p>

        <!-- 住宿信息 -->
        <div v-if="scenicInfo.hasAccommodation === 1 && scenicInfo.accommodationInfo" class="accommodation-info">
          <h3 class="info-title">住宿信息</h3>
          <p class="accommodation-text">{{ scenicInfo.accommodationInfo }}</p>
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

      <!-- 评论区 -->
      <div class="comment-section">
        <h3 class="section-title">评论 <span v-if="commentTotal > 0" class="comment-count">{{ commentTotal }}</span></h3>

        <div class="comment-input-container">
          <div class="rating-container">
            <span class="rating-label">评分：</span>
            <div class="stars">
              <span
                v-for="star in 5"
                :key="'root-' + star"
                class="star"
                :class="{ active: selectedRating >= star }"
                @click="selectedRating = star"
              >★</span>
            </div>
          </div>
          <textarea
            v-model="commentContent"
            class="comment-textarea"
            rows="3"
            placeholder="写下您的评论..."
            :disabled="submitting"
          />
          <div class="image-upload">
            <input
              id="image-upload"
              type="file"
              multiple
              accept="image/*"
              class="file-input"
              :disabled="submitting"
              @change.prevent="handleImageUpload"
            />
            <label class="upload-btn" for="image-upload">📷 上传图片（≤5M）</label>
            <div v-if="commentPreviewUrls.length > 0" class="comment-preview-wrapper">
              <div class="preview-list">
                <div v-for="(url, index) in commentPreviewUrls" :key="index" class="preview-item">
                  <img :src="url" class="preview-img" alt="预览" />
                  <button type="button" class="del-preview-btn" @click="removePreviewImage(index)">×</button>
                </div>
              </div>
            </div>
          </div>
          <button
            type="button"
            class="btn btn-primary submit-btn"
            :disabled="!commentContent.trim() || submitting || selectedRating === 0"
            @click="submitComment"
          >
            {{ submitting ? '发布中...' : '发布' }}
          </button>
        </div>

        <div class="comment-list">
          <div v-if="comments.length === 0" class="no-comments">暂无评论，快来发表第一条评论吧！</div>

          <div v-for="comment in comments" :key="comment.id" class="comment-thread">
            <div class="comment-row">
              <img
                class="comment-avatar"
                :src="avatarOf(comment)"
                alt=""
                @error="onAvatarError"
              />
              <div class="comment-main">
                <div class="comment-user">{{ comment.username || '匿名用户' }}</div>
                <p class="comment-text">{{ comment.content }}</p>
                <div v-if="comment.commentImg" class="comment-images">
                  <img
                    v-for="(img, index) in splitImgs(comment.commentImg)"
                    :key="index"
                    :src="img"
                    alt="评论图片"
                    class="comment-image"
                    @error="handleImgError"
                  />
                </div>
                <div v-if="comment.score" class="comment-rating">
                  <span
                    v-for="star in 5"
                    :key="'s-' + comment.id + '-' + star"
                    class="star"
                    :class="{ active: comment.score >= star }"
                  >★</span>
                </div>
                <div class="comment-actions">
                  <span class="comment-time">{{ formatCommentTime(comment.createTime) }}</span>
                  <button type="button" class="action-placeholder" title="占位">👍</button>
                  <button type="button" class="action-placeholder" title="占位">👎</button>
                  <button type="button" class="reply-btn" @click="toggleReplyBox(comment)">回复</button>
                </div>

                <div v-if="replyingToId === comment.id" class="reply-composer">
                  <div class="rating-container compact">
                    <span class="rating-label">评分：</span>
                    <div class="stars">
                      <span
                        v-for="star in 5"
                        :key="'r-' + star"
                        class="star"
                        :class="{ active: replyRating >= star }"
                        @click="replyRating = star"
                      >★</span>
                    </div>
                  </div>
                  <textarea
                    v-model="replyContent"
                    class="comment-textarea compact"
                    rows="2"
                    :placeholder="`回复 @${comment.username || '用户'}（不可附带图片）`"
                    :disabled="replySubmitting"
                  />
                  <div class="reply-composer-actions">
                    <button type="button" class="btn-cancel" :disabled="replySubmitting" @click="cancelReply">取消</button>
                    <button
                      type="button"
                      class="btn btn-primary submit-btn compact"
                      :disabled="!replyContent.trim() || replySubmitting || replyRating === 0"
                      @click="submitReply"
                    >
                      {{ replySubmitting ? '发布中...' : '发布回复' }}
                    </button>
                  </div>
                </div>

                <div v-if="comment.replies?.length" class="reply-list">
                  <div v-for="reply in comment.replies" :key="reply.id" class="comment-row is-reply">
                    <img
                      class="comment-avatar sm"
                      :src="avatarOf(reply)"
                      alt=""
                      @error="onAvatarError"
                    />
                    <div class="comment-main">
                      <div class="comment-user">{{ reply.username || '匿名用户' }}</div>
                      <p class="comment-text">{{ reply.content }}</p>
                      <div v-if="reply.score" class="comment-rating">
                        <span
                          v-for="star in 5"
                          :key="'rs-' + reply.id + '-' + star"
                          class="star"
                          :class="{ active: reply.score >= star }"
                        >★</span>
                      </div>
                      <div class="comment-actions">
                        <span class="comment-time">{{ formatCommentTime(reply.createTime) }}</span>
                        <button type="button" class="action-placeholder" title="占位">👍</button>
                        <button type="button" class="action-placeholder" title="占位">👎</button>
                        <button type="button" class="reply-btn" @click="toggleReplyBox(reply)">回复</button>
                      </div>

                      <div v-if="replyingToId === reply.id" class="reply-composer">
                        <div class="rating-container compact">
                          <span class="rating-label">评分：</span>
                          <div class="stars">
                            <span
                              v-for="star in 5"
                              :key="'rr-' + star"
                              class="star"
                              :class="{ active: replyRating >= star }"
                              @click="replyRating = star"
                            >★</span>
                          </div>
                        </div>
                        <textarea
                          v-model="replyContent"
                          class="comment-textarea compact"
                          rows="2"
                          :placeholder="`回复 @${reply.username || '用户'}（不可附带图片）`"
                          :disabled="replySubmitting"
                        />
                        <div class="reply-composer-actions">
                          <button type="button" class="btn-cancel" :disabled="replySubmitting" @click="cancelReply">取消</button>
                          <button
                            type="button"
                            class="btn btn-primary submit-btn compact"
                            :disabled="!replyContent.trim() || replySubmitting || replyRating === 0"
                            @click="submitReply"
                          >
                            {{ replySubmitting ? '发布中...' : '发布回复' }}
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useUserStore } from '../stores/user';
import { userLike, userCollect, userComment, isLike, isCollect } from '../api/user';
import { getScenicDetail, getScComments } from '../api/scenic';
import { uploadFile } from '../api/upload';
import PageLoadingOverlay from '../components/PageLoadingOverlay.vue';
import { getErrorMessage } from '../api/axios';
import { collectImageUrls, collectImageUrlsFromItems, preloadImages } from '../utils/preloadImages';
import { formatScenicPrice, hasScenicPrice } from '../utils/scenicPrice';

const DEFAULT_AVATAR =
  'https://shixiaoyu-funny.oss-cn-beijing.aliyuncs.com/%E6%95%B0%E6%99%BA%E4%B9%A1%E7%BA%A6%E6%B3%A8%E5%86%8C%E5%A4%B4%E5%83%8F%E8%AE%BE%E8%AE%A1.png';
const DEFAULT_SCENIC_COVER = DEFAULT_AVATAR;

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
const isLiked = ref(false);
const isCollected = ref(false);
const submitting = ref(false);
const pageReady = ref(false);
const loadError = ref('');
const likeLoading = ref(false);
const collectLoading = ref(false);

const replyingToId = ref<number | null>(null);
const replyContent = ref('');
const replyRating = ref(0);
const replySubmitting = ref(false);

const commentTotal = computed(() => {
  return comments.value.reduce((n, c) => n + 1 + (c.replies?.length || 0), 0);
});

const avatarOf = (c: any) => c?.avatar || DEFAULT_AVATAR;
const onAvatarError = (e: Event) => {
  (e.target as HTMLImageElement).src = DEFAULT_AVATAR;
};
const splitImgs = (s: string) => (s || '').split(',').map((x) => x.trim()).filter(Boolean);

const flattenComments = (list: any[]) => {
  if (!Array.isArray(list)) return [];
  const flat: any[] = [];
  for (const c of list) {
    flat.push(c);
    if (c.replies?.length) flat.push(...c.replies);
  }
  return flat;
};

const formatCommentTime = (t: string | Date | null | undefined) => {
  if (!t) return '未知时间';
  const d = new Date(t);
  if (Number.isNaN(d.getTime())) return String(t);
  const diff = Date.now() - d.getTime();
  const m = Math.floor(diff / 60000);
  if (m < 1) return '刚刚';
  if (m < 60) return `${m}分钟前`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}小时前`;
  const day = Math.floor(h / 24);
  if (day < 7) return `${day}天前`;
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

const toggleReplyBox = (comment: any) => {
  if (replyingToId.value === comment.id) {
    cancelReply();
    return;
  }
  replyingToId.value = comment.id;
  replyContent.value = '';
  replyRating.value = 0;
};

const cancelReply = () => {
  replyingToId.value = null;
  replyContent.value = '';
  replyRating.value = 0;
};

const goBack = () => {
  router.back();
};

const fetchScenicDetail = async () => {
  pageReady.value = false;
  loadError.value = '';
  scenicInfo.value = null;
  if (!Number.isFinite(scenicId.value)) {
    loadError.value = '无效的景点 ID';
    pageReady.value = true;
    return;
  }
  try {
    const res = await getScenicDetail(scenicId.value);
    scenicInfo.value = res.data;
    if (!scenicInfo.value) {
      throw new Error('');
    }
    await fetchUserStatus();
    await fetchComments();
    pageReady.value = true;
    const urls = [
      ...collectImageUrls(scenicInfo.value?.image),
      ...collectImageUrlsFromItems(flattenComments(comments.value)),
    ];
    void preloadImages(urls);
  } catch (err) {
    scenicInfo.value = null;
    loadError.value = getErrorMessage(err);
    if (loadError.value) {
      ElMessage.error(loadError.value);
    }
    pageReady.value = true;
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
  if (!imageStr) return DEFAULT_SCENIC_COVER;
  return imageStr.split(',')[0] || DEFAULT_SCENIC_COVER;
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
      const uploadPromises = selectedImages.value.map((file) => uploadFile(file));
      const uploadResults = await Promise.all(uploadPromises);
      const imgUrls = uploadResults.map((res) => {
        return typeof res === 'string' ? res : (res as any).url || (res as any).data || '';
      });
      commentImg = imgUrls.join(',');
    }

    await userComment({
      scenicId: scenicId.value,
      content: commentContent.value,
      score: selectedRating.value,
      commentImg: commentImg || undefined
    });

    commentContent.value = '';
    selectedRating.value = 0;
    selectedImages.value.forEach((_, index) => {
      URL.revokeObjectURL(commentPreviewUrls.value[index] || '');
    });
    selectedImages.value = [];
    commentPreviewUrls.value = [];

    await fetchComments();
    ElMessage.success('评论发布成功！');
  } catch (error: any) {
    console.error('评论失败:', error);
    ElMessage.error(error?.message || '评论失败，请重试');
  } finally {
    submitting.value = false;
  }
};

const submitReply = async () => {
  if (!replyingToId.value || !replyContent.value.trim() || replyRating.value === 0) return;
  replySubmitting.value = true;
  try {
    await userComment({
      scenicId: scenicId.value,
      parentId: replyingToId.value,
      content: replyContent.value,
      score: replyRating.value
    });
    cancelReply();
    await fetchComments();
    ElMessage.success('回复成功！');
  } catch (error: any) {
    console.error('回复失败:', error);
    ElMessage.error(error?.message || error?.response?.data?.message || '回复失败，请重试');
  } finally {
    replySubmitting.value = false;
  }
};

onUnmounted(() => {
  commentPreviewUrls.value.forEach((url) => URL.revokeObjectURL(url));
});

watch(
  () => route.params.id,
  (id) => {
    scenicId.value = Number(id);
    if (userStore.isLoggedIn) {
      fetchScenicDetail();
    }
  },
  { immediate: true }
);

onMounted(() => {
  if (!userStore.isLoggedIn) {
    router.push('/login');
  }
});
</script>

<style scoped>
/* 样式无修改，完全保留原有效果 */
.scenic-detail-container {
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
  padding: 16px 20px 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 16px;
  color: #18191c;
}

.comment-count {
  font-size: 14px;
  font-weight: 500;
  color: #9499a0;
  margin-left: 6px;
}

.comment-input-container {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e3e5e7;
}

.rating-container {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.rating-container.compact {
  margin-bottom: 8px;
}

.rating-label {
  font-size: 13px;
  color: #61666d;
}

.stars {
  display: flex;
  gap: 2px;
}

.star {
  font-size: 18px;
  color: #c9ccd0;
  cursor: pointer;
  user-select: none;
}

.star.active {
  color: #f9a825;
}

.comment-textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  border: 1px solid #e3e5e7;
  border-radius: 6px;
  font-size: 14px;
  line-height: 1.6;
  resize: vertical;
  color: #18191c;
  background: #f1f2f3;
  outline: none;
}

.comment-textarea:focus {
  background: #fff;
  border-color: #8bc34a;
}

.comment-textarea.compact {
  font-size: 13px;
  background: #f6f7f8;
}

.image-upload {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.file-input {
  display: none;
}

.upload-btn {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  font-size: 13px;
  color: #61666d;
  background: #f1f2f3;
  border-radius: 4px;
  cursor: pointer;
}

.upload-btn:hover {
  background: #e3e5e7;
}

.comment-preview-wrapper {
  width: 100%;
}

.preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.preview-item {
  position: relative;
  width: 64px;
  height: 64px;
  border-radius: 6px;
  overflow: hidden;
}

.preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.del-preview-btn {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 18px;
  height: 18px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 12px;
  line-height: 1;
  cursor: pointer;
}

.submit-btn {
  margin-top: 10px;
  white-space: nowrap;
  padding: 0 16px;
  height: 36px;
  background: #8BC34A;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.submit-btn.compact {
  margin-top: 0;
  padding: 6px 14px;
  height: auto;
  font-size: 13px;
}

.submit-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.comment-list {
  max-height: none;
}

.no-comments {
  padding: 32px 0;
  text-align: center;
  color: #9499a0;
  font-size: 14px;
}

.comment-thread {
  padding: 14px 0;
  border-bottom: 1px solid #e3e5e7;
}

.comment-thread:last-child {
  border-bottom: none;
}

.comment-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.comment-row.is-reply {
  margin-top: 12px;
  gap: 10px;
}

.comment-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  background: #e3e5e7;
}

.comment-avatar.sm {
  width: 32px;
  height: 32px;
}

.comment-main {
  flex: 1;
  min-width: 0;
}

.comment-user {
  font-size: 13px;
  font-weight: 500;
  color: #61666d;
  margin-bottom: 4px;
}

.comment-text {
  font-size: 15px;
  line-height: 1.6;
  color: #18191c;
  margin: 0 0 6px;
  word-break: break-word;
  white-space: pre-wrap;
}

.comment-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 6px 0 8px;
}

.comment-image {
  width: 96px;
  height: 96px;
  object-fit: cover;
  border-radius: 6px;
}

.comment-rating {
  display: flex;
  gap: 2px;
  margin-bottom: 4px;
}

.comment-rating .star {
  font-size: 14px;
  cursor: default;
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 4px;
  color: #9499a0;
  font-size: 12px;
}

.comment-time {
  color: #9499a0;
}

.action-placeholder,
.reply-btn {
  border: none;
  background: transparent;
  color: #9499a0;
  font-size: 12px;
  cursor: pointer;
  padding: 0;
}

.reply-btn:hover {
  color: #8bc34a;
}

.reply-list {
  margin-top: 4px;
}

.reply-composer {
  margin-top: 10px;
  padding: 10px 12px;
  background: #f6f7f8;
  border-radius: 6px;
}

.reply-composer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
}

.btn-cancel {
  border: none;
  background: transparent;
  color: #61666d;
  font-size: 13px;
  cursor: pointer;
  padding: 6px 10px;
}

.btn-cancel:hover {
  color: #18191c;
}

@media (max-width: 480px) {
  .comment-avatar {
    width: 40px;
    height: 40px;
  }

  .comment-image {
    width: 72px;
    height: 72px;
  }

  .preview-item {
    width: 50px;
    height: 50px;
  }
}
</style>
