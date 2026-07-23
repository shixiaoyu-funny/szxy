import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'
import { getVillageInfo } from '../api/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../views/Home.vue')
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/Login.vue')
    },
    {
      path: '/ai/inquire',
      name: 'aiInquire',
      component: () => import('../views/ai/Inquire.vue')
    },
    {
      path: '/ai/multimodal',
      name: 'aiMultimodal',
      component: () => import('../views/ai/Multimodal.vue')
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/Profile.vue')
    },
    {
      path: '/village/:id',
      name: 'villageDetail',
      component: () => import('../views/VillageDetail.vue')
    },
    {
      path: '/edit-profile',
      name: 'editProfile',
      component: () => import('../views/EditProfile.vue')
    },
    {
      path: '/access',
      name: 'access',
      component: () => import('../views/Access.vue')
    },
    {
      path: '/attraction-manage',
      name: 'attractionManage',
      component: () => import('../views/AttractionManage.vue')
    },
    {
      path: '/village-manage',
      name: 'villageManage',
      component: () => import('../views/VillageManage.vue')
    },
    {
      path: '/village-farmers',
      name: 'villageFarmers',
      component: () => import('../views/VillageFarmers.vue')
    }
  ]
})

// 全局前置守卫
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  
  // 如果用户未登录且不是前往登录页，则跳转到登录页
  if (!userStore.isLoggedIn && to.name !== 'login') {
    next({ name: 'login' })
    return
  }
  
  // 如果用户已登录且是农户（type === 2），检查其是否有归属农村
  if (userStore.isLoggedIn && userStore.userInfo && userStore.userInfo.type === 2) {
    try {
      // 尝试获取农户归属的农村信息
      const res = await getVillageInfo()
      const hasVillage = !!res.data
      
      // 如果用户没有归属农村且尝试访问除个人主页和农村管理外的其他页面，则重定向到农村管理页面
      if (
        !hasVillage &&
        !['villageManage', 'profile', 'login', 'editProfile', 'home'].includes(to.name as string)
      ) {
        next({ name: 'villageManage' })
        return
      }
      
      // 如果用户没有归属农村但在农村管理页面，允许访问
      if (!hasVillage && to.name === 'villageManage') {
        next()
        return
      }
    } catch (error) {
      console.error('检查农村归属信息失败:', error)
      // 如果检查失败，仍然允许访问，避免阻塞用户
      next()
      return
    }
  }
  
  // 其他情况正常访问
  next()
})

export default router