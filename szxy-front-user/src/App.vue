<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  House,
  Location,
  User,
  SwitchButton,
  ArrowLeft,
  Star,
  Goods,
  ChatDotRound,
  InfoFilled,
  OfficeBuilding,
  Picture,
  UserFilled,
  Key
} from '@element-plus/icons-vue';
import { useUserStore } from './stores/user';
import { getUserInfo } from './api/user';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const showLayout = computed(() => route.path !== '/login');

const userInfo = computed(() => userStore.userInfo || {});
const role = computed(() => (userInfo.value as Record<string, unknown>).role);
const isFarmerOrChief = computed(() => role.value === 2 || role.value === 3);
const isChief = computed(() => role.value === 3);

const activeMenu = computed(() => {
  if (route.path.startsWith('/village')) return '/village';
  if (route.path.startsWith('/my-collections')) return '/my-collections';
  if (route.path.startsWith('/my-likes')) return '/my-likes';
  if (route.path.startsWith('/my-comments')) return '/my-comments';
  if (route.path.startsWith('/profile') || route.path.startsWith('/edit')) return '/profile';
  if (route.path.startsWith('/farmer/village')) return '/farmer/village';
  if (route.path.startsWith('/farmer/scenic')) return '/farmer/scenics';
  if (route.path.startsWith('/farmer/farmers')) return '/farmer/farmers';
  if (route.path.startsWith('/about')) return '/about';
  return '/';
});

const pageTitle = computed(() => (route.meta.title as string) || '数智乡约');

const showBack = computed(() => {
  const p = route.path;
  if (p === '/village') return false;
  return (
    p.startsWith('/scenic/') ||
    p.startsWith('/village/') ||
    p.startsWith('/my-') ||
    p.startsWith('/edit') ||
    p.startsWith('/farmer/')
  );
});

const handleBack = () => {
  if (window.history.length > 1) router.back();
  else router.push('/');
};

const handleLogout = () => {
  userStore.logout();
  router.push('/login');
};

onMounted(async () => {
  if (userStore.isLoggedIn && !userStore.userInfo) {
    try {
      const res = await getUserInfo();
      if (res.data) {
        userStore.setUserInfo(res.data);
      }
    } catch (e) {
      console.error('加载用户信息失败:', e);
    }
  }
});
</script>

<template>
  <div class="common-layout">
    <el-container class="layout-root">
      <el-aside v-if="showLayout" width="220px" class="layout-aside">
        <div class="aside-brand">
          <h1 class="brand-logo">数智乡约</h1>
          <p class="brand-sub">AI 驱动乡村振兴服务平台</p>
        </div>
        <el-menu
          :default-active="activeMenu"
          router
          class="aside-menu"
          background-color="transparent"
          text-color="rgba(255,255,255,0.9)"
          active-text-color="#ffffff"
        >
          <el-menu-item index="/">
            <el-icon><House /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/village">
            <el-icon><Location /></el-icon>
            <span>优质农村</span>
          </el-menu-item>
          <el-menu-item index="/my-collections">
            <el-icon><Star /></el-icon>
            <span>我的收藏</span>
          </el-menu-item>
          <el-menu-item index="/my-likes">
            <el-icon><Goods /></el-icon>
            <span>我的点赞</span>
          </el-menu-item>
          <el-menu-item index="/my-comments">
            <el-icon><ChatDotRound /></el-icon>
            <span>我的评论</span>
          </el-menu-item>
          <el-menu-item index="/profile">
            <el-icon><User /></el-icon>
            <span>个人主页</span>
          </el-menu-item>
          <el-menu-item v-if="isFarmerOrChief" index="/farmer/village">
            <el-icon><OfficeBuilding /></el-icon>
            <span>我的村</span>
          </el-menu-item>
          <el-menu-item v-if="isFarmerOrChief" index="/farmer/scenics">
            <el-icon><Picture /></el-icon>
            <span>我的景点</span>
          </el-menu-item>
          <el-menu-item v-if="isChief" index="/farmer/farmers">
            <el-icon><UserFilled /></el-icon>
            <span>本村农户管理</span>
          </el-menu-item>
          <el-menu-item v-if="!userStore.isLoggedIn" index="/login">
            <el-icon><Key /></el-icon>
            <span>登录/注册</span>
          </el-menu-item>
          <el-menu-item index="/about">
            <el-icon><InfoFilled /></el-icon>
            <span>关于我们</span>
          </el-menu-item>
        </el-menu>
<!--        <div class="aside-footer">-->
<!--          <el-button type="primary" plain @click="handleLogout">-->
<!--            <el-icon><SwitchButton /></el-icon>-->
<!--            <span>退出登录</span>-->
<!--          </el-button>-->
<!--        </div>-->
      </el-aside>

      <el-container class="layout-right">
        <el-header v-if="showLayout" class="layout-header">
          <el-button v-if="showBack" class="header-back" text @click="handleBack">
            <el-icon><ArrowLeft /></el-icon>
          </el-button>
          <span class="header-title">{{ pageTitle }}</span>
        </el-header>

        <el-main class="layout-main" :class="{ 'is-login': !showLayout }">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </el-main>

        <el-footer v-if="showLayout" class="layout-footer">
          数智乡约 · AI 驱动乡村振兴服务平台
        </el-footer>
      </el-container>
    </el-container>
  </div>
</template>

<style scoped>
.common-layout {
  min-height: 100vh;
}

.layout-root {
  min-height: 100vh;
}

.layout-aside {
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #4CAF50 0%, #8BC34A 100%);
  box-shadow: 2px 0 10px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  height: 100vh;
  overflow-y: auto;
}

.aside-brand {
  padding: 24px 16px;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.25);
}

.brand-logo {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.brand-sub {
  margin: 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.85);
}

.aside-menu {
  flex: 1;
  border-right: none;
  padding-top: 8px;
}

.aside-menu :deep(.el-menu-item) {
  height: 52px;
  margin: 2px 8px;
  border-radius: 8px;
}

.aside-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.12);
}

.aside-menu :deep(.el-menu-item.is-active) {
  background: rgba(255, 255, 255, 0.25);
  font-weight: 600;
}

.aside-footer {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.25);
}

.aside-footer .el-button {
  width: 100%;
}

.layout-right {
  min-width: 0;
}

.layout-header {
  display: flex;
  align-items: center;
  height: 60px;
  background: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.06);
  z-index: 10;
  position: sticky;
  top: 0;
  gap: 8px;
}

.header-back {
  font-size: 18px;
  color: #333;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.layout-main {
  background: #f5f7fa;
  padding: 24px;
}

.layout-main.is-login {
  background: transparent;
  padding: 0;
}

.layout-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 48px;
  background: #fff;
  color: #909399;
  font-size: 13px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
