<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showSuccessToast, showToast } from 'vant'
import request from '@/utils/request'
import { getRiderLevelColor, getRiderLevelLabel } from '@/utils/riderLevel'

const router = useRouter()
const online = ref(false)
const pendingOrders = ref<any[]>([])
const loading = ref(false)
const riderInfo = ref<any>({})

const isApproved = computed(() => riderInfo.value.auditStatus === 1)
const isPending = computed(() => riderInfo.value.auditStatus === 0)
const isRejected = computed(() => riderInfo.value.auditStatus === 2)

async function fetchRiderInfo() {
  try {
    const res: any = await request.get('/rider/info')
    riderInfo.value = res.data || {}
    online.value = riderInfo.value.status === 3
  } catch {}
}

async function fetchOrders() {
  loading.value = true
  try {
    const res: any = await request.get('/rider/order/pending')
    pendingOrders.value = res.data || []
  } catch {}
  loading.value = false
}

async function toggleOnline(newVal: boolean) {
  if (!isApproved.value) {
    showToast('审核通过后才能上线接单')
    return
  }

  if (newVal) {
    try {
      await request.post('/rider/online')
      online.value = true
      showSuccessToast('已上线，等待派单')
      fetchOrders()
    } catch {
      online.value = false
    }
    return
  }

  try {
    await request.post('/rider/offline')
    online.value = false
    showToast('已下线')
    pendingOrders.value = []
  } catch {
    online.value = true
  }
}

async function acceptOrder(order: any) {
  try {
    await request.post(`/rider/order/${order.orderNo}/accept`)
    showSuccessToast('接单成功，请前往商家取餐')
    router.push('/tasks')
  } catch {}
}

function handleLogout() {
  showConfirmDialog({ title: '确认退出登录？' })
    .then(() => {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      router.replace('/login')
    })
    .catch(() => {})
}

let refreshTimer: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  await fetchRiderInfo()
  if (online.value && isApproved.value) fetchOrders()
  refreshTimer = setInterval(() => {
    if (online.value && isApproved.value) fetchOrders()
  }, 30000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<template>
  <div class="home-page">
    <van-nav-bar title="接单大厅" fixed placeholder>
      <template #right>
        <van-icon name="user-o" size="20" @click="handleLogout" />
      </template>
    </van-nav-bar>

    <div v-if="isPending" class="audit-card pending">
      <van-icon name="clock-o" size="28" color="#ff976a" />
      <div class="audit-text">
        <h4>账号审核中</h4>
        <p>您的骑手认证正在审核，请等待管理员审核通过后再上线接单。</p>
      </div>
    </div>

    <div v-if="isRejected" class="audit-card rejected">
      <van-icon name="close" size="28" color="#ee0a24" />
      <div class="audit-text">
        <h4>审核未通过</h4>
        <p>{{ riderInfo.rejectionReason || '您的骑手认证未通过审核，请联系平台客服。' }}</p>
      </div>
    </div>

    <div v-if="isApproved" class="rider-card">
      <div class="rider-top">
        <div class="rider-avatar">
          <van-image
            :src="riderInfo.avatar || 'data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 48 48%22%3E%3Crect fill=%22%23e8e8e8%22 width=%2248%22 height=%2248%22/%3E%3Ccircle cx=%2224%22 cy=%2218%22 r=%228%22 fill=%22%23bbb%22/%3E%3Cellipse cx=%2224%22 cy=%2240%22 rx=%2214%22 ry=%228%22 fill=%22%23bbb%22/%3E%3C/svg%3E'"
            width="48"
            height="48"
            round
          />
          <div class="online-dot" :class="{ on: online }"></div>
        </div>

        <div class="rider-info">
          <div class="rider-name">{{ riderInfo.realName || '骑手' }}</div>
          <div class="rider-level">
            <span :style="{ color: getRiderLevelColor(riderInfo.level) }">
              {{ getRiderLevelLabel(riderInfo.level) }}
            </span>
            <van-rate v-model="riderInfo.score" readonly size="10" allow-half />
            <span class="score-num">{{ riderInfo.score?.toFixed(1) || '5.0' }}</span>
          </div>
        </div>

        <div class="online-toggle">
          <van-switch :model-value="online" @update:model-value="toggleOnline" active-color="#07c160" />
          <span class="toggle-text">{{ online ? '在线' : '离线' }}</span>
        </div>
      </div>

      <div class="rider-stats">
        <div class="stat"><span class="num">{{ riderInfo.totalOrders || 0 }}</span><span class="lbl">总单量</span></div>
        <div class="stat"><span class="num">{{ riderInfo.monthOrders || 0 }}</span><span class="lbl">本月</span></div>
        <div class="stat"><span class="num">{{ riderInfo.todayOrders || 0 }}</span><span class="lbl">今日</span></div>
        <div class="stat"><span class="num">{{ riderInfo.levelScore || 0 }}</span><span class="lbl">等级分</span></div>
      </div>
    </div>

    <div v-if="isApproved && !online" class="offline-tip">
      <van-icon name="info-o" />
      <span>您当前已离线，请开启在线状态接收订单。</span>
    </div>

    <van-pull-refresh v-if="isApproved" v-model="loading" @refresh="fetchOrders">
      <div v-if="online" class="order-section">
        <div class="section-header">
          <h4>待接订单</h4>
          <span class="count">共 {{ pendingOrders.length }} 单</span>
        </div>

        <div v-for="order in pendingOrders" :key="order.id" class="order-card">
          <div class="card-top">
            <span class="order-no">#{{ order.orderNo?.substring(order.orderNo.length - 10) }}</span>
            <span class="order-fee">￥{{ (order.deliveryFee || 5).toFixed(2) }}</span>
          </div>

          <div class="card-addr">
            <div class="addr-row">
              <van-icon name="shop-o" size="14" color="#ff6b35" />
              <span>取餐：{{ order.merchantName || '商家' }}</span>
            </div>
            <div class="addr-row">
              <van-icon name="location-o" size="14" color="#409eff" />
              <span>送达：{{ order.address }}</span>
            </div>
          </div>

          <div class="card-footer">
            <span class="card-time">{{ order.createTime?.substring(0, 16) }}</span>
            <van-button type="primary" size="small" round @click="acceptOrder(order)">立即接单</van-button>
          </div>
        </div>

        <van-empty v-if="!loading && pendingOrders.length === 0" description="暂无待接订单" />
      </div>
    </van-pull-refresh>

    <van-tabbar route>
      <van-tabbar-item to="/" icon="home-o">大厅</van-tabbar-item>
      <van-tabbar-item to="/tasks" icon="todo-list-o">任务</van-tabbar-item>
      <van-tabbar-item to="/income" icon="gold-coin-o">收入</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<style scoped>
.home-page { padding-bottom: 50px; background: #f5f5f5; }

.audit-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin: 12px 8px;
  padding: 16px;
  border-radius: 12px;
}

.audit-card.pending { background: #fff8e8; border: 1px solid #ffe0b0; }
.audit-card.rejected { background: #fff0f0; border: 1px solid #ffcccc; }
.audit-text h4 { font-size: 16px; color: #333; margin-bottom: 4px; }
.audit-text p { font-size: 13px; color: #666; line-height: 1.5; }

.rider-card {
  background: linear-gradient(135deg, #07c160, #05a84d);
  color: #fff;
  margin: 8px;
  padding: 16px;
  border-radius: 12px;
}

.rider-top { display: flex; align-items: center; gap: 12px; }
.rider-avatar { position: relative; }

.online-dot {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #999;
  border: 2px solid #fff;
}

.online-dot.on { background: #07c160; }
.rider-info { flex: 1; }
.rider-name { font-size: 16px; font-weight: 600; }
.rider-level { display: flex; align-items: center; gap: 6px; font-size: 12px; margin-top: 2px; }
.score-num { font-size: 11px; opacity: 0.8; }
.online-toggle { text-align: center; }
.toggle-text { display: block; font-size: 10px; margin-top: 2px; opacity: 0.8; }

.rider-stats {
  display: flex;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
}

.stat { flex: 1; text-align: center; }
.num { font-size: 18px; font-weight: 700; display: block; }
.lbl { font-size: 11px; opacity: 0.8; }

.offline-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 8px;
  padding: 12px 16px;
  background: #fff7e8;
  border-radius: 8px;
  font-size: 13px;
  color: #ff976a;
}

.order-section { padding: 0 8px; }
.section-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 4px 8px; }
.section-header h4 { font-size: 15px; }
.count { font-size: 12px; color: #999; }

.order-card { background: #fff; margin-bottom: 8px; padding: 12px; border-radius: 8px; }
.card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.order-no { font-size: 13px; color: #666; }
.order-fee { font-size: 18px; font-weight: 700; color: #ee0a24; }
.card-addr { margin-bottom: 6px; }
.addr-row { display: flex; align-items: center; gap: 6px; padding: 3px 0; font-size: 13px; color: #333; }
.card-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; padding-top: 8px; border-top: 1px solid #f0f0f0; }
.card-time { font-size: 11px; color: #bbb; }
</style>
