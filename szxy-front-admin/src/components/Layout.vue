<template>
  <div class="common-layout">
    <el-container class="layout-root">
      <el-aside width="220px" class="layout-aside">
        <div class="aside-brand">
          <h1 class="brand-logo">数智乡约</h1>
          <p class="brand-sub">管理端</p>
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
            <el-icon><DataLine /></el-icon>
            <span>报表查询</span>
          </el-menu-item>
          <el-menu-item index="/scenic">
            <el-icon><Picture /></el-icon>
            <span>景点管理</span>
          </el-menu-item>
          <el-menu-item index="/village">
            <el-icon><Location /></el-icon>
            <span>农村信息管理</span>
          </el-menu-item>
          <el-menu-item index="/farmer">
            <el-icon><User /></el-icon>
            <span>农户管理</span>
          </el-menu-item>
          <el-menu-item index="/farmer-access">
            <el-icon><Checked /></el-icon>
            <span>农户审批</span>
          </el-menu-item>
          <el-menu-item index="/vghead-access">
            <el-icon><UserFilled /></el-icon>
            <span>村长审批</span>
          </el-menu-item>
          <el-menu-item index="/profile">
            <el-icon><Avatar /></el-icon>
            <span>个人中心</span>
          </el-menu-item>
        </el-menu>
        <div class="aside-footer">
          <el-button type="primary" plain @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            <span>退出登录</span>
          </el-button>
        </div>
      </el-aside>

      <el-container class="layout-right">
        <el-header class="layout-header">
          <span class="header-title">{{ pageTitle }}</span>
          <div class="header-right">
            <el-dropdown>
              <el-button type="primary" plain>
                <el-icon><User /></el-icon>
                <span>{{ displayName }}</span>
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="goProfile">个人中心</el-dropdown-item>
                  <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <el-main class="layout-main">
          <!-- 外层单根包裹：避免页面多根节点导致 transition out-in 卡住白屏 -->
          <router-view v-slot="{ Component, route: viewRoute }">
            <transition name="fade" mode="out-in">
              <div v-if="Component" :key="viewRoute.fullPath" class="route-view">
                <component :is="Component" />
              </div>
            </transition>
          </router-view>
        </el-main>

        <el-footer class="layout-footer">
          数智乡约 · AI 驱动乡村振兴服务平台（管理端）
        </el-footer>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  DataLine,
  Picture,
  Location,
  User,
  Avatar,
  SwitchButton,
  ArrowDown,
  Checked,
  UserFilled
} from '@element-plus/icons-vue';
import { loginApi } from '../api';
import store from '../store';

const route = useRoute();
const router = useRouter();

const activeMenu = computed(() => {
  const p = route.path;
  if (p.startsWith('/scenic')) return '/scenic';
  if (p.startsWith('/village')) return '/village';
  if (p.startsWith('/farmer-access')) return '/farmer-access';
  if (p.startsWith('/vghead-access')) return '/vghead-access';
  if (p.startsWith('/farmer')) return '/farmer';
  if (p.startsWith('/profile')) return '/profile';
  return '/';
});

const pageTitle = computed(() => (route.meta.title as string) || '数智乡约管理端');

const userInfo = computed(() => store.state.user.info);
const displayName = computed(() => {
  const u = userInfo.value as Record<string, unknown> | null;
  if (!u) return '管理员';
  return String(u.username ?? u.user_name ?? '管理员');
});

const goProfile = () => {
  router.push('/profile');
};

const handleLogout = async () => {
  try {
    await loginApi.logout();
    store.actions.logout();
    router.push('/login');
  } catch (error) {
    console.error('退出登录失败:', error);
  }
};
</script>

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
  background: linear-gradient(180deg, #42a5f5 0%, #1e88e5 100%);
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
  justify-content: space-between;
  height: 60px;
  background: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.06);
  z-index: 10;
  position: sticky;
  top: 0;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.layout-main {
  background: #f0f6ff;
  padding: 24px;
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

.route-view {
  min-height: 100%;
}
</style>
