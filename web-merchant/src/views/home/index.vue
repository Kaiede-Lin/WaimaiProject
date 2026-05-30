<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast, showConfirmDialog } from 'vant'
import request from '@/utils/request'

const router = useRouter()
const info = ref<any>({})
const todayStats = ref({ revenue: 0, orderCount: 0, pendingOrders: 0 })
const isOpen = ref(false)

const statusText = computed(() => isOpen.value ? '营业中' : '已休息')
const statusColor = computed(() => isOpen.value ? '#07c160' : '#999')

async function fetchInfo() {
  try {
    const res: any = await request.get('/merchant/info')
    info.value = res.data || {}
    isOpen.value = info.value.status === 1
  } catch {}
}

async function fetchTodayStats() {
  try {
    const today = new Date().toISOString().substring(0, 10)
    const res: any = await request.get('/merchant/report/daily', { params: { date: today } })
    const data = res.data || {}
    todayStats.value = {
      revenue: data.totalRevenue || 0,
      orderCount: data.orderCount || 0,
      pendingOrders: data.pendingCount || 0
    }
  } catch {}
}

async function toggleStatus() {
  try {
    const newStatus = isOpen.value ? 0 : 1
    await request.put('/merchant/info', { ...info.value, status: newStatus })
    isOpen.value = !isOpen.value
    showSuccessToast(isOpen.value ? '已开门营业' : '已休息')
  } catch {}
}

function handleLogout() {
  showConfirmDialog({ title: '确认退出登录？' }).then(() => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    router.replace('/login')
  }).catch(() => {})
}

onMounted(() => {
  fetchInfo()
  fetchTodayStats()
})
</script>

<template>
  <div class="home-page">
    <van-nav-bar title="营业看板" fixed placeholder>
      <template #right>
        <van-icon name="setting-o" size="20" @click="router.push('/settings')" />
      </template>
    </van-nav-bar>

    <div class="shop-card">
      <div class="shop-top">
        <van-image :src="info.logo || 'data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 48 48%22%3E%3Crect fill=%22%23fff0e8%22 width=%2248%22 height=%2248%22/%3E%3Crect x=%2212%22 y=%2212%22 width=%2224%22 height=%2224%22 rx=%224%22 fill=%22%23ff6b35%22/%3E%3Ctext x=%2224%22 y=%2230%22 text-anchor=%22middle%22 fill=%22white%22 font-size=%2216%22%3E店%3C/text%3E%3C/svg%3E'" width="48" height="48" radius="8" />
        <div class="shop-info">
          <h3>{{ info.name || '商家名称' }}</h3>
          <div class="shop-meta">
            <van-rate v-model="info.score" readonly size="12" allow-half />
            <span>{{ info.score?.toFixed(1) || '5.0' }}</span>
            <span class="divider">|</span>
            <span>月售{{ info.monthlySales || 0 }}单</span>
          </div>
        </div>
        <div :class="['status-toggle', { open: isOpen }]" @click="toggleStatus">
          <div class="toggle-dot"></div>
          <span class="toggle-label">{{ statusText }}</span>
        </div>
      </div>
      <div class="shop-desc" v-if="info.description">{{ info.description }}</div>
    </div>

    <div class="stats-row">
      <div class="stat-item">
        <span class="stat-val">¥{{ todayStats.revenue.toFixed(2) }}</span>
        <span class="stat-label">今日营收</span>
      </div>
      <div class="stat-item">
        <span class="stat-val">{{ todayStats.orderCount }}</span>
        <span class="stat-label">今日订单</span>
      </div>
      <div class="stat-item">
        <span class="stat-val highlight">{{ todayStats.pendingOrders }}</span>
        <span class="stat-label">待处理</span>
      </div>
    </div>

    <div class="action-grid">
      <van-grid :column-num="3" :border="false">
        <van-grid-item icon="orders-o" text="订单管理" to="/orders" />
        <van-grid-item icon="wap-home-o" text="菜单管理" to="/menu" />
        <van-grid-item icon="chart-trending-o" text="销售报表" to="/reports" />
        <van-grid-item icon="setting-o" text="店铺设置" to="/settings" />
        <van-grid-item icon="phone-o" :text="info.phone || '未设置电话'" />
        <van-grid-item icon="clock-o" :text="info.businessHours || '09:00-22:00'" />
      </van-grid>
    </div>

    <div class="quick-actions">
      <van-button type="primary" block round to="/orders" color="#ff6b35">查看新订单</van-button>
      <van-button plain block round type="danger" @click="handleLogout" style="margin-top:8px">退出登录</van-button>
    </div>

    <van-tabbar route>
      <van-tabbar-item to="/" icon="home-o">首页</van-tabbar-item>
      <van-tabbar-item to="/orders" icon="orders-o">订单</van-tabbar-item>
      <van-tabbar-item to="/menu" icon="wap-home-o">菜单</van-tabbar-item>
      <van-tabbar-item to="/reports" icon="chart-trending-o">报表</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<style scoped>
.home-page { padding-bottom: 50px; }
.shop-card {
  background: var(--primary-gradient); color: #fff; margin: 8px; padding: 20px;
  border-radius: var(--radius-lg); box-shadow: 0 8px 24px rgba(255,107,53,0.25);
}
.shop-top { display: flex; align-items: center; gap: 12px; }
.shop-info { flex: 1; }
.shop-info h3 { font-size: 18px; margin-bottom: 4px; }
.shop-meta { display: flex; align-items: center; gap: 6px; font-size: 12px; opacity: 0.9; }
.divider { opacity: 0.5; }
.status-toggle {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  background: rgba(255,255,255,0.2); border-radius: var(--radius-round); padding: 10px 14px;
  cursor: pointer; min-width: 60px; transition: background 0.2s;
}
.status-toggle:hover { background: rgba(255,255,255,0.3); }
.status-toggle .toggle-dot {
  width: 12px; height: 12px; border-radius: 50%; background: #999; transition: background 0.3s;
}
.status-toggle.open .toggle-dot { background: var(--success); box-shadow: 0 0 8px rgba(7,193,96,0.6); }
.toggle-label { font-size: 10px; }
.shop-desc { margin-top: 8px; font-size: 12px; opacity: 0.85; }

.stats-row { display: flex; gap: 8px; padding: 8px; }
.stat-item {
  flex: 1; background: var(--card-bg); border-radius: var(--radius); padding: 16px 12px;
  text-align: center; box-shadow: var(--shadow-sm);
}
.stat-val { font-size: 22px; font-weight: 700; color: var(--text); display: block; }
.stat-val.highlight { color: var(--danger); }
.stat-label { font-size: 12px; color: var(--text-secondary); margin-top: 4px; display: block; }

.quick-actions { padding: 0 8px; }
</style>
