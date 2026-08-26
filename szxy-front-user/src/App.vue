<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import {
  House,
  Location,
  User,
  SwitchButton,
  ArrowLeft,
  MagicStick,
  InfoFilled,
  OfficeBuilding,
  Picture,
  UserFilled,
  Key,
  Bell,
  Checked
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
/** 禾小智 AI 仅游客可用（未登录视为游客，农户/村长隐藏入口） */
const showAiEntry = computed(() => {
  if (!userStore.isLoggedIn) return true;
  return role.value === 1;
});

/** 灯光按钮：切换左侧栏黄绿 / 黑紫渐变 */
const asidePurple = ref(false);
const toggleAsideTheme = () => {
  asidePurple.value = !asidePurple.value;
};

const activeMenu = computed(() => {
  if (route.path.startsWith('/village')) return '/village';
  if (route.path.startsWith('/ai')) return '/ai';
  if (
    route.path.startsWith('/my-collections') ||
    route.path.startsWith('/my-likes') ||
    route.path.startsWith('/my-comments') ||
    route.path.startsWith('/profile') ||
    route.path.startsWith('/edit') ||
    route.path.startsWith('/apply-farmer')
  ) {
    return '/profile';
  }
  if (route.path.startsWith('/messages')) return '/messages';
  if (route.path.startsWith('/farmer/village')) return '/farmer/village';
  if (route.path.startsWith('/farmer/scenic')) return '/farmer/scenics';
  if (route.path.startsWith('/farmer/farmers')) return '/farmer/farmers';
  if (route.path.startsWith('/farmer/vghead')) return '/farmer/vghead';
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
    p.startsWith('/messages') ||
    p.startsWith('/edit') ||
    p.startsWith('/apply-farmer') ||
    p.startsWith('/farmer/')
  );
});

const handleBack = () => {
  if (window.history.length > 1) router.back();
  else router.push('/');
};

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '退出登录', {
      confirmButtonText: '确认退出',
      cancelButtonText: '取消',
      type: 'warning',
      customClass: 'logout-confirm'
    });
    userStore.logout();
    router.push('/login');
  } catch {
    // 用户取消
  }
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

watch(showAiEntry, (allowed) => {
  if (!allowed && route.path.startsWith('/ai')) {
    router.replace('/');
  }
});
</script>

<template>
  <div class="common-layout" :class="{ 'is-dark': asidePurple }">
    <el-container class="layout-root">
      <el-aside
        v-if="showLayout"
        width="220px"
        class="layout-aside"
        :class="{ 'is-purple': asidePurple }"
      >
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
          <el-menu-item v-if="showAiEntry" index="/ai">
            <el-icon><MagicStick /></el-icon>
            <span>禾小智AI</span>
          </el-menu-item>
          <el-menu-item v-if="userStore.isLoggedIn" index="/messages">
            <el-icon><Bell /></el-icon>
            <span>消息</span>
          </el-menu-item>
          <el-menu-item v-if="isFarmerOrChief" index="/farmer/village">
            <el-icon><OfficeBuilding /></el-icon>
            <span>本村详情</span>
          </el-menu-item>
          <el-menu-item v-if="isFarmerOrChief" index="/farmer/scenics">
            <el-icon><Picture /></el-icon>
            <span>我的景点</span>
          </el-menu-item>
          <el-menu-item v-if="isChief" index="/farmer/farmers">
            <el-icon><UserFilled /></el-icon>
            <span>本村农户管理</span>
          </el-menu-item>
          <el-menu-item v-if="isChief" index="/farmer/vghead">
            <el-icon><Checked /></el-icon>
            <span>村长审批</span>
          </el-menu-item>
          <el-menu-item v-if="!userStore.isLoggedIn" index="/login">
            <el-icon><Key /></el-icon>
            <span>登录/注册</span>
          </el-menu-item>
          <el-menu-item index="/profile">
            <el-icon><User /></el-icon>
            <span>个人主页</span>
          </el-menu-item>
          <el-menu-item index="/about">
            <el-icon><InfoFilled /></el-icon>
            <span>关于我们</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-container class="layout-right">
        <el-header v-if="showLayout" class="layout-header">
          <el-button v-if="showBack" class="header-back" text @click="handleBack">
            <el-icon><ArrowLeft /></el-icon>
          </el-button>
          <span class="header-title">{{ pageTitle }}</span>
          <div class="header-actions">
            <button
              class="light-btn"
              :class="{ active: asidePurple }"
              :title="asidePurple ? '恢复浅色主题' : '切换深色主题'"
              @click="toggleAsideTheme"
            >
              <i class="iconfont-light icon-dengguang" />
            </button>
            <button
              v-if="userStore.isLoggedIn"
              class="logout-btn"
              title="退出登录"
              @click="handleLogout"
            >
              <el-icon :size="18"><SwitchButton /></el-icon>
              <span>退出登录</span>
            </button>
          </div>
        </el-header>

        <el-main class="layout-main" :class="{ 'is-login': !showLayout }">
          <div id="page-loading-host" class="layout-main-overlay-host" aria-hidden="true" />
          <div class="layout-main-body">
          <!-- 外层单根包裹：避免页面多根节点导致 transition out-in 卡住白屏 -->
          <router-view v-slot="{ Component, route: viewRoute }">
            <transition name="fade" mode="out-in">
              <div v-if="Component" :key="viewRoute.fullPath" class="route-view">
                <component :is="Component" />
              </div>
            </transition>
          </router-view>
          </div>
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
  transition: background 0.45s ease;
}

.layout-aside.is-purple {
  background: linear-gradient(180deg, #0d0d0d 0%, #2d0a3a 40%, #6b2d8b 100%);
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
  z-index: 200;
  position: sticky;
  top: 0;
  gap: 8px;
  transition: background 0.45s ease, box-shadow 0.45s ease;
}

.header-back {
  font-size: 18px;
  color: #333;
  transition: color 0.45s ease;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  transition: color 0.45s ease;
}

.header-actions {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  position: relative;
  z-index: 201;
}

.logout-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 14px;
  border: none;
  border-radius: 18px;
  background: linear-gradient(135deg, #8BC34A 0%, #66BB6A 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(139, 195, 74, 0.25);
  transition: all 0.3s ease;
}

.logout-btn:hover {
  background: linear-gradient(135deg, #ef5350 0%, #e57373 100%);
  box-shadow: 0 4px 12px rgba(244, 67, 54, 0.25);
  transform: translateY(-1px);
}

.light-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: linear-gradient(135deg, #8BC34A 0%, #66BB6A 100%);
  color: #fff;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(139, 195, 74, 0.25);
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.light-btn .iconfont-light {
  font-size: 18px;
  line-height: 1;
  color: #fff;
}

.light-btn:hover {
  transform: translateY(-1px) scale(1.04);
  box-shadow: 0 4px 12px rgba(139, 195, 74, 0.35);
}

.light-btn.active {
  background: linear-gradient(135deg, #1a0a2e 0%, #6b2d8b 100%);
  box-shadow: 0 2px 8px rgba(74, 20, 140, 0.4);
}

.layout-main {
  position: relative;
  background: #f5f7fa;
  padding: 24px;
  transition: background 0.45s ease;
}

.layout-main-overlay-host {
  position: absolute;
  inset: 0;
  z-index: 1000;
  pointer-events: none;
}

.layout-main-body {
  position: relative;
  z-index: 1;
  min-height: 100%;
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
  transition: background 0.45s ease, color 0.45s ease;
}

/* 深色主题：顶栏黑灰、内容区纯黑 */
.common-layout.is-dark .layout-header {
  background: #1c1c1e;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.45);
}

.common-layout.is-dark .header-back,
.common-layout.is-dark .header-title {
  color: #e8e8ea;
}

.common-layout.is-dark .layout-main {
  background: #000;
}

.common-layout.is-dark .layout-footer {
  background: #141416;
  color: #8e8e93;
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

<style>
/* 退出登录确认弹窗（ElMessageBox Teleport 到 body） */
.logout-confirm {
  border-radius: 12px;
}

.logout-confirm .el-message-box__header {
  padding: 20px 24px 12px;
}

.logout-confirm .el-message-box__title {
  font-size: 17px;
  font-weight: 600;
  color: #2E7D32;
}

.logout-confirm .el-message-box__content {
  padding: 16px 24px;
}

.logout-confirm .el-message-box__message {
  font-size: 14px;
  color: #555;
  line-height: 1.6;
}

.logout-confirm .el-message-box__btns {
  padding: 12px 24px 20px;
}

.logout-confirm .el-message-box__btns .el-button--primary {
  background: linear-gradient(135deg, #8BC34A 0%, #66BB6A 100%);
  border: none;
  transition: all 0.3s ease;
}

.logout-confirm .el-message-box__btns .el-button--primary:hover {
  background: linear-gradient(135deg, #7CB342 0%, #558B2F 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(139, 195, 74, 0.4);
}

.logout-confirm .el-message-box__btns .el-button:not(.el-button--primary):hover {
  color: #558B2F;
  border-color: #8BC34A;
  background: #f0f9e8;
}
</style>
