<template>
  <div class="village-detail-container">
    <header class="page-header">
      <h1 class="header-title">农村详情</h1>
    </header>
    <div class="content">
      <div v-if="loading" class="placeholder">加载中...</div>
      <div v-else-if="errorMsg" class="placeholder err">{{ errorMsg }}</div>
      <div v-else-if="village" class="detail">
        <h2 class="name">{{ String(village.name ?? '') }}</h2>
        <div v-if="village.image" class="cover">
          <img :src="String(village.image).split(',')[0]" :alt="String(village.name ?? '')" />
        </div>
        <div class="meta">
          <p><span class="k">村长</span>{{ village.managerName || '—' }}</p>
          <p>
            <span class="k">地区</span>{{ village.province || '' }} {{ village.city || '' }}
            {{ village.county || '' }}
          </p>
          <p><span class="k">介绍</span>{{ village.intro || '暂无' }}</p>
          <p><span class="k">最佳游玩</span>{{ village.bestTime || '—' }}</p>
          <p><span class="k">季节性活动</span>{{ village.activity || '—' }}</p>
          <p><span class="k">联系方式</span>{{ village.contact || '—' }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { getVillageInfo } from '../api/user';
import { normalizeVillageVO } from '../utils/villageApi';

const route = useRoute();
const loading = ref(true);
const errorMsg = ref('');
const village = ref<Record<string, unknown> | null>(null);

const load = async () => {
  loading.value = true;
  errorMsg.value = '';
  village.value = null;
  try {
    const res = await getVillageInfo();
    const mine = res.data as { id?: number; name?: string } | null | undefined;
    if (!mine?.id) {
      errorMsg.value = '暂无归属村落信息';
      return;
    }
    const idParam = String(route.params.id || '');
    if (String(mine.id) !== idParam) {
      errorMsg.value = '仅可查看您已归属的村落详情（接口未提供按任意村落 ID 查询）';
      return;
    }
    village.value = normalizeVillageVO(mine as Record<string, unknown>);
  } catch {
    errorMsg.value = '加载失败';
  } finally {
    loading.value = false;
  }
};

onMounted(load);
watch(
  () => route.params.id,
  () => load()
);
</script>

<style scoped>
.village-detail-container {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 88px;
}

.page-header {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  padding: 16px;
  text-align: center;
}

.header-title {
  color: white;
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.content {
  padding: 20px;
}

.placeholder {
  text-align: center;
  color: #999;
  font-size: 14px;
  margin-top: 40px;
}

.placeholder.err {
  color: #c62828;
  padding: 0 16px;
}

.detail {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.name {
  margin: 0 0 12px;
  font-size: 18px;
  text-align: center;
}

.cover img {
  width: 100%;
  max-height: 220px;
  object-fit: cover;
  border-radius: 8px;
}

.meta {
  margin-top: 16px;
  font-size: 14px;
  color: #444;
  line-height: 1.6;
}

.meta p {
  margin: 8px 0;
}

.k {
  display: inline-block;
  min-width: 88px;
  color: #888;
  font-size: 13px;
}
</style>
