import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/index.vue'),
      meta: { title: '登录' }
    },
    {
      path: '/',
      name: 'Home',
      component: () => import('@/views/home/index.vue'),
      meta: { title: '附近商家', requireAuth: true }
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('@/views/profile/index.vue'),
      meta: { title: '我的', requireAuth: true }
    },
    {
      path: '/address',
      name: 'Address',
      component: () => import('@/views/address/index.vue'),
      meta: { title: '收货地址', requireAuth: true }
    },
    {
      path: '/merchant/:id',
      name: 'MerchantDetail',
      component: () => import('@/views/merchant/detail.vue'),
      meta: { title: '商家详情', requireAuth: true }
    },
    {
      path: '/order/confirm',
      name: 'OrderConfirm',
      component: () => import('@/views/order/confirm.vue'),
      meta: { title: '确认订单', requireAuth: true }
    },
    {
      path: '/order/pay/:id',
      name: 'OrderPay',
      component: () => import('@/views/order/pay.vue'),
      meta: { title: '支付', requireAuth: true }
    },
    {
      path: '/order/list',
      name: 'OrderList',
      component: () => import('@/views/order/list.vue'),
      meta: { title: '我的订单', requireAuth: true }
    },
    {
      path: '/order/:id',
      name: 'OrderDetail',
      component: () => import('@/views/order/detail.vue'),
      meta: { title: '订单详情', requireAuth: true }
    },
    {
      path: '/review/list',
      name: 'MyReviews',
      component: () => import('@/views/review/index.vue'),
      meta: { title: '我的评价', requireAuth: true }
    },
    {
      path: '/dispute/list',
      name: 'MyDisputes',
      component: () => import('@/views/dispute/list.vue'),
      meta: { title: '我的纠纷', requireAuth: true }
    }
  ]
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('accessToken')
  if (to.meta.requireAuth && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
