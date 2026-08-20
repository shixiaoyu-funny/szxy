import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../views/Home.vue'),
      meta: { title: '首页' }
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/Login.vue'),
      meta: { title: '登录' }
    },
    {
      path: '/scenic/:id',
      name: 'scenicDetail',
      component: () => import('../views/ScenicDetail.vue'),
      meta: { title: '景点详情' }
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/Profile.vue'),
      meta: { title: '个人主页' }
    },
    {
      path: '/village',
      name: 'village',
      component: () => import('../views/Village.vue'),
      meta: { title: '优质农村' }
    },
    {
      path: '/village/:id',
      name: 'villageDetail',
      component: () => import('../views/Village.vue'),
      meta: { title: '农村详情' }
    },
    {
      path: '/edit-profile',
      name: 'editProfile',
      component: () => import('../views/EditProfile.vue'),
      meta: { title: '编辑个人信息' }
    },
    {
      path: '/my-collections',
      name: 'myCollections',
      component: () => import('../views/MyCollections.vue'),
      meta: { title: '我的收藏' }
    },
    {
      path: '/my-comments',
      name: 'myComments',
      component: () => import('../views/MyComments.vue'),
      meta: { title: '我的评论' }
    },
    {
      path: '/my-likes',
      name: 'myLikes',
      component: () => import('../views/MyLikes.vue'),
      meta: { title: '我的点赞' }
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('../views/About.vue'),
      meta: { title: '关于我们' }
    },
    {
      path: '/farmer/village',
      name: 'myVillage',
      component: () => import('../views/farmer/MyVillage.vue'),
      meta: { title: '我的村庄' }
    },
    {
      path: '/farmer/scenics',
      name: 'myScenics',
      component: () => import('../views/farmer/MyScenics.vue'),
      meta: { title: '我的景点' }
    },
    {
      path: '/farmer/scenic/add',
      name: 'addScenic',
      component: () => import('../views/farmer/AddScenic.vue'),
      meta: { title: '新增景点' }
    },
    {
      path: '/farmer/farmers',
      name: 'villageFarmers',
      component: () => import('../views/farmer/VillageFarmers.vue'),
      meta: { title: '村内农户' }
    }
  ]
})

export default router
