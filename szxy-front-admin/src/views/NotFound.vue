<template>
  <div class="not-found">
    <h1 class="code">404</h1>
    <p class="tip">抱歉，你访问的页面不存在</p>
    <el-button type="primary" @click="goHome">返回首页</el-button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();

const goHome = () => {
  router.push('/');
};
</script>

<style scoped>
.not-found {
  min-height: calc(100vh - 160px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  text-align: center;
}

.code {
  font-size: 72px;
  font-weight: 700;
  color: #42a5f5;
  margin: 0;
}

.tip {
  font-size: 16px;
  color: #666;
  margin: 0;
}
</style>
