import { createRouter, createWebHistory } from 'vue-router'
import store from '../store'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/Login.vue'),
      meta: { title: '登录', requiresAuth: false }
    },
    {
      path: '/',
      name: 'layout',
      component: () => import('../components/Layout.vue'),
      meta: {
        title: '管理端',
        requiresAuth: true
      },
      children: [
        {
          path: '',
          name: 'dashboard',
          component: () => import('../views/Dashboard.vue'),
          meta: { title: '报表查询' }
        },
        {
          path: 'scenic',
          name: 'scenic',
          component: () => import('../views/Scenic.vue'),
          meta: { title: '景点管理' }
        },
        {
          path: 'village',
          name: 'village',
          component: () => import('../views/Village.vue'),
          meta: { title: '农村信息管理' }
        },
        {
          path: 'farmer',
          name: 'farmer',
          component: () => import('../views/Farmer.vue'),
          meta: { title: '农户管理' }
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('../views/Profile.vue'),
          meta: { title: '个人中心' }
        }
      ]
    }
  ],
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title as string || '数智乡约管理端'
  
  // 检查是否需要登录
  const requiresAuth = to.meta.requiresAuth !== undefined ? to.meta.requiresAuth : true;
  if (requiresAuth && !store.state.user.token) {
    next({
      name: 'login',
      query: { redirect: to.fullPath }
    })
  } else {
    next()
  }
})

export default router
