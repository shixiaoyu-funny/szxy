import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior() {
    return { top: 0 }
  },
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
      path: '/search',
      name: 'search',
      component: () => import('../views/Search.vue'),
      meta: { title: '搜索结果' }
    },
    {
      path: '/ai',
      name: 'aiChat',
      component: () => import('../views/AiChat.vue'),
      meta: { title: '禾小智AI' }
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
      path: '/apply-farmer',
      name: 'applyFarmer',
      component: () => import('../views/ApplyFarmer.vue'),
      meta: { title: '申请成为农户' }
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
      path: '/messages',
      name: 'messages',
      component: () => import('../views/Messages.vue'),
      meta: { title: '消息' }
    },
    {
      path: '/messages/apply/:id',
      name: 'messageApplyDetail',
      component: () => import('../views/MessageApplyDetail.vue'),
      meta: { title: '申请详情' }
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
    },
    {
      path: '/farmer/vghead',
      name: 'vgHeadApprove',
      component: () => import('../views/farmer/VgHeadApprove.vue'),
      meta: { title: '村长审批' }
    },
    {
      path: '/farmer/vghead/:id',
      name: 'vgHeadDetail',
      component: () => import('../views/farmer/VgHeadDetail.vue'),
      meta: { title: '审批详情' }
    }
  ]
})

/** 禾小智 AI 仅游客可用 */
router.beforeEach((to) => {
  if (!to.path.startsWith('/ai')) return true
  const userStore = useUserStore()
  if (!userStore.isLoggedIn) return true
  const role = Number((userStore.userInfo as { role?: number } | null)?.role)
  if (role !== 1) return '/'
  return true
})

/** 懒加载 chunk 失败（热更新/部署后旧缓存）时整页跳转，避免卡在空白 */
router.onError((err, to) => {
  const msg = String(err?.message || err || '')
  const isChunkError =
    msg.includes('Failed to fetch dynamically imported module') ||
    msg.includes('Importing a module script failed') ||
    msg.includes('Loading chunk') ||
    msg.includes('Unable to preload CSS')
  if (isChunkError && to?.fullPath) {
    window.location.assign(to.fullPath)
  }
})

export default router
