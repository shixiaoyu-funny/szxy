<template>
  <div class="home">
    <header class="hero">
      <h1 class="title">数智乡约 · 农户端</h1>
      <p class="subtitle">乡村文旅服务与 AI 助手</p>
    </header>

    <section class="cards">
      <router-link to="/village-manage" class="card">
        <span class="card-icon">🏡</span>
        <div class="card-body">
          <h2>农村管理</h2>
          <p>农户资质申请、村落信息、村长资质申请</p>
        </div>
      </router-link>

      <router-link to="/attraction-manage" class="card">
        <span class="card-icon">🗺️</span>
        <div class="card-body">
          <h2>景点管理</h2>
          <p>查看已上架景点，申请私人/公共景点上报</p>
        </div>
      </router-link>

      <router-link v-if="isChief" to="/village-farmers" class="card chief">
        <span class="card-icon">👥</span>
        <div class="card-body">
          <h2>本村农户管理</h2>
          <p>村长专属：维护本村农户账号</p>
        </div>
      </router-link>

      <router-link to="/ai/inquire" class="card">
        <span class="card-icon">💬</span>
        <div class="card-body">
          <h2>AI 智能问答</h2>
        </div>
      </router-link>

      <router-link to="/profile" class="card">
        <span class="card-icon">👤</span>
        <div class="card-body">
          <h2>个人信息</h2>
        </div>
      </router-link>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { useFarmerContext } from '../composables/useFarmerContext';

const { isChief, refreshFarmerContext } = useFarmerContext();

onMounted(() => {
  refreshFarmerContext();
});
</script>

<style scoped>
.home {
  min-height: 100vh;
  padding: 20px 16px 88px;
  background: linear-gradient(180deg, #e8f5e9 0%, #f5f5f5 40%);
}

.hero {
  text-align: center;
  margin-bottom: 24px;
}

.title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 700;
  color: #1b5e20;
}

.subtitle {
  margin: 0;
  font-size: 14px;
  color: #666;
}

.cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 520px;
  margin: 0 auto;
}

.card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  text-decoration: none;
  color: inherit;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.card:active {
  transform: scale(0.99);
}

.card.chief {
  border: 1px solid #81c784;
}

.card-icon {
  font-size: 28px;
  line-height: 1;
}

.card-body h2 {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.card-body p {
  margin: 0;
  font-size: 13px;
  color: #777;
  line-height: 1.4;
}
</style>
