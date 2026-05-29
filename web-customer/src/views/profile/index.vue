<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showSuccessToast } from 'vant'
import request from '@/utils/request'

const router = useRouter()
const user = ref<any>({})

async function fetchUser() {
  try {
    const res: any = await request.get('/user/info')
    user.value = res.data || {}
  } catch { /* ignore */ }
}

function handleLogout() {
  showConfirmDialog({ title: '确认退出登录？' }).then(() => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    showSuccessToast('已退出')
    router.replace('/login')
  }).catch(() => {})
}

onMounted(fetchUser)
</script>

<template>
  <div class="profile-page">
    <van-nav-bar title="我的" fixed placeholder />

    <div class="user-card">
      <van-image :src="user.avatar || 'data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 64 64%22%3E%3Crect fill=%22%23e8e8e8%22 width=%2264%22 height=%2264%22/%3E%3Ccircle cx=%2232%22 cy=%2224%22 r=%2210%22 fill=%22%23bbb%22/%3E%3Cellipse cx=%2232%22 cy=%2254%22 rx=%2218%22 ry=%2210%22 fill=%22%23bbb%22/%3E%3C/svg%3E'" width="64" height="64" round />
      <div class="user-info">
        <h3>{{ user.nickname || '用户' }}</h3>
        <p>{{ user.phone || '' }}</p>
      </div>
    </div>

    <van-cell-group inset>
      <van-cell title="我的订单" icon="orders-o" is-link to="/order/list" />
      <van-cell title="收货地址" icon="location-o" is-link to="/address" />
      <van-cell title="我的评价" icon="star-o" is-link />
      <van-cell title="优惠券" icon="coupon-o" is-link :value="(user.couponCount || 0) + '张'" />
      <van-cell title="关于我们" icon="info-o" is-link value="v1.0.0" />
    </van-cell-group>

    <div style="padding: 24px 16px">
      <van-button round block type="danger" @click="handleLogout">退出登录</van-button>
    </div>

    <van-tabbar route>
      <van-tabbar-item to="/" icon="home-o">首页</van-tabbar-item>
      <van-tabbar-item to="/order/list" icon="orders-o">订单</van-tabbar-item>
      <van-tabbar-item to="/profile" icon="user-o">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<style scoped>
.profile-page { padding-bottom: 50px; }
.user-card {
  display: flex; align-items: center; gap: 16px;
  padding: 24px 16px; background: linear-gradient(135deg, #409EFF, #66b1ff);
  color: #fff; margin-bottom: 12px;
}
.user-info h3 { font-size: 18px; margin-bottom: 4px; }
.user-info p { font-size: 13px; opacity: 0.8; }
</style>
