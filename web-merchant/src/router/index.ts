import { createRouter, createWebHistory } from 'vue-router'
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'Login', component: () => import('@/views/login/index.vue'), meta: { title: '商家登录' } },
    { path: '/', name: 'Home', component: () => import('@/views/home/index.vue'), meta: { title: '营业看板', requireAuth: true } },
    { path: '/orders', name: 'Orders', component: () => import('@/views/order/list.vue'), meta: { title: '订单管理', requireAuth: true } },
    { path: '/order/:id', name: 'OrderDetail', component: () => import('@/views/order/detail.vue'), meta: { title: '订单详情', requireAuth: true } },
    { path: '/menu', name: 'Menu', component: () => import('@/views/menu/manage.vue'), meta: { title: '菜单管理', requireAuth: true } },
    { path: '/reports', name: 'Reports', component: () => import('@/views/report/index.vue'), meta: { title: '销售报表', requireAuth: true } },
    { path: '/settings', name: 'Settings', component: () => import('@/views/settings/index.vue'), meta: { title: '店铺设置', requireAuth: true } },
    { path: '/apply', name: 'Apply', component: () => import('@/views/apply/index.vue'), meta: { title: '入驻申请' } },
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
