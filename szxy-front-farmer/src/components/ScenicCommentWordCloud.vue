<template>
  <div class="scenic-comments-panel">
    <div v-if="loading" class="panel-loading">加载评论中…</div>
    <template v-else>
      <div v-if="comments.length" class="word-block">
        <h4 class="sub-title">评论热词 TOP10</h4>
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
        <p v-else class="hint">暂无有效词语生成排行</p>
      </div>
      <p v-else class="empty">暂无用户评论</p>

      <div v-if="comments.length" class="comment-snippet-list">
        <h4 class="sub-title">评论摘录</h4>
        <div v-for="c in comments" :key="c.id ?? c.content" class="snippet">
          <span class="snippet-user">{{ c.username || '匿名' }}</span>
          <span class="snippet-text">{{ c.content }}</span>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { getScComments } from '../api/scenicComments';
import {
  type CommentRankRow,
  WORD_CLOUD_FONT_FAMILY,
  commentRankBarPercent,
  getCommentRankRows,
  wordColorByName
} from '../utils/commentWordCloud';

const props = defineProps<{ scenicId: number }>();

const loading = ref(true);
const comments = ref<Array<{ id?: number; username?: string; content?: string }>>([]);

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

onMounted(async () => {
  try {
    const res = (await getScComments(props.scenicId)) as { data?: typeof comments.value };
    comments.value = res.data ?? [];
  } catch (e) {
    console.error(e);
    comments.value = [];
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.scenic-comments-panel {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #eee;
}

.panel-loading {
  font-size: 14px;
  color: #666;
  padding: 8px 0;
}

.sub-title {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 10px;
  color: #2e7d32;
}

.word-block {
  margin-bottom: 16px;
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

.hint {
  font-size: 13px;
  color: #888;
  margin: 0;
}

.empty {
  font-size: 14px;
  color: #888;
  margin: 0 0 12px;
}

.comment-snippet-list {
  max-height: 220px;
  overflow-y: auto;
}

.snippet {
  font-size: 13px;
  line-height: 1.5;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.snippet-user {
  color: #2e7d32;
  font-weight: 500;
}

.snippet-text {
  color: #444;
  word-break: break-all;
}
</style>
