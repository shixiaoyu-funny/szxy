<template>
  <div class="layout-container">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <h1 class="logo">数智乡约</h1>
        <p class="sub-title">AI 驱动乡村振兴服务平台</p>
      </div>
      <nav class="sidebar-menu">
        <!-- 使用 custom + isExactActive：避免 to="/" 作为前缀导致所有路由下「报表查询」始终高亮 -->
        <router-link v-slot="{ navigate, href, isExactActive }" to="/" custom>
          <a :href="href" class="menu-item" :class="{ active: isExactActive }" @click="navigate">
            <el-icon><House /></el-icon>
            <span>报表查询</span>
          </a>
        </router-link>
        <router-link v-for="link in subNav" :key="link.to" v-slot="{ navigate, href, isActive }" :to="link.to" custom>
          <a :href="href" class="menu-item" :class="{ active: isActive }" @click="navigate">
            <el-icon><component :is="link.icon" /></el-icon>
            <span>{{ link.label }}</span>
          </a>
        </router-link>
      </nav>
      <div class="sidebar-footer">
        <el-button type="primary" @click="handleLogout" plain>
          <el-icon><Switch /></el-icon>
          <span>退出登录</span>
        </el-button>
      </div>
    </aside>
    
    <!-- 主内容区 -->
    <main class="main-content">
      <header class="main-header">
        <div class="header-left">
          <el-button type="primary" @click="toggleSidebar" plain>
            <el-icon><Menu /></el-icon>
          </el-button>
        </div>
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
      </header>
      <div class="content-wrapper">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { House, Picture, Location, User, Menu, Switch, ArrowDown, Avatar } from '@element-plus/icons-vue';
import type { Component } from 'vue';
import store from '../store';
import { loginApi } from '../api';

const router = useRouter();
const isSidebarCollapsed = ref(false);

const subNav: { to: string; label: string; icon: Component }[] = [
  { to: '/scenic', label: '景点资质审批', icon: Picture },
  { to: '/village', label: '农村信息管理', icon: Location },
  { to: '/farmer', label: '农户与村长资质', icon: User },
  { to: '/profile', label: '个人中心', icon: Avatar }
];

const userInfo = computed(() => store.state.user.info);
const displayName = computed(() => {
  const u = userInfo.value as Record<string, unknown> | null;
  if (!u) return '管理员';
  return String(u.username ?? u.user_name ?? '管理员');
});

const toggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value;
};

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
.layout-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e9f2 100%);
}

.sidebar {
  width: 240px;
  height: 100%;
  background: linear-gradient(180deg, #4CAF50 0%, #8BC34A 100%);
  color: white;
  transition: width 0.3s ease;
  box-shadow: 2px 0 10px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
}

.sidebar.collapsed {
  width: 80px;
}

.sidebar-header {
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
}

.logo {
  font-size: 24px;
  font-weight: bold;
  margin: 0;
  margin-bottom: 5px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.sub-title {
  font-size: 14px;
  margin: 0;
  opacity: 0.9;
}

.sidebar-menu {
  flex: 1;
  padding: 20px 0;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  color: white;
  text-decoration: none;
  transition: all 0.3s ease;
  border-radius: 0 20px 20px 0;
  margin: 5px 0;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: translateX(5px);
}

.menu-item.active {
  background: rgba(255, 255, 255, 0.2);
  font-weight: bold;
}

.menu-item el-icon {
  margin-right: 12px;
  font-size: 18px;
}

.sidebar-footer {
  padding: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.main-header {
  height: 60px;
  background: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  z-index: 100;
}

.header-left el-button,
.header-right el-button {
  transition: all 0.3s ease;
}

.header-left el-button:hover,
.header-right el-button:hover {
  transform: scale(1.05);
}

.content-wrapper {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

/* 动画效果 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>