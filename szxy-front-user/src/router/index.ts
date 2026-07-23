import { createRouter, createWebHistory } from 'vue-router'

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
      path: '/ai/recommend',
      name: 'aiRecommend',
      component: () => import('../views/ai/Recommend.vue')
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
      path: '/scenic/:id',
      name: 'scenicDetail',
      component: () => import('../views/ScenicDetail.vue')
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/Profile.vue')
    },
    {
      path: '/village',
      name: 'village',
      component: () => import('../views/Village.vue')
    },
    {
      path: '/village/:id',
      name: 'villageDetail',
      component: () => import('../views/Village.vue')
    },
    {
      path: '/edit-profile',
      name: 'editProfile',
      component: () => import('../views/EditProfile.vue')
    },
    {
      path: '/my-collections',
      name: 'myCollections',
      component: () => import('../views/MyCollections.vue')
    },
    {
      path: '/my-comments',
      name: 'myComments',
      component: () => import('../views/MyComments.vue')
    },
    {
      path: '/my-likes',
      name: 'myLikes',
      component: () => import('../views/MyLikes.vue')
    }
  ]
})

export default router
