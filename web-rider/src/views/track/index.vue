<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showSuccessToast } from 'vant'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const orderNo = route.params.orderNo as string
const lng = ref(116.397)
const lat = ref(39.908)
const orderInfo = ref<any>({})
const activeStep = ref(0)
const reportedCount = ref(0)
const deliveryTime = ref(0)
let timer: any = null
let timeCounter: any = null

const coords = computed(() => `${lng.value.toFixed(6)}, ${lat.value.toFixed(6)}`)

async function fetchOrder() {
  try {
    const res: any = await request.get(`/order/${orderNo}`)
    orderInfo.value = res.data || {}
    if (orderInfo.value.status === 'DELIVERING') activeStep.value = 1
    else if (orderInfo.value.status === 'COMPLETED') activeStep.value = 2
  } catch {}
}

function reportLocation() {
  const jitter = () => (Math.random() - 0.5) * 0.003
  const newLng = lng.value + jitter()
  const newLat = lat.value + jitter()
  lng.value = newLng
  lat.value = newLat
  request.post('/rider/location', { longitude: newLng, latitude: newLat }).catch(() => {})
  if (orderInfo.value.id) {
    request.post('/rider/track/report', {
      orderId: orderInfo.value.id,
      longitude: newLng,
      latitude: newLat
    }).then(() => { reportedCount.value++ }).catch(() => {})
  }
}

async function completeDelivery() {
  try {
    await request.post(`/rider/order/${orderNo}/complete`)
    activeStep.value = 2
    showSuccessToast('配送完成！')
    setTimeout(() => router.push('/tasks'), 1500)
  } catch {}
}

onMounted(() => {
  fetchOrder()
  reportLocation()
  timer = setInterval(reportLocation, 15000)
  timeCounter = setInterval(() => { deliveryTime.value++ }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (timeCounter) clearInterval(timeCounter)
})

const elapsedStr = computed(() => {
  const m = Math.floor(deliveryTime.value / 60)
  const s = deliveryTime.value % 60
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
})
</script>

<template>
  <div class="track-page">
    <van-nav-bar title="配送导航" left-arrow @click-left="router.back()" fixed placeholder />

    <div class="map-area">
      <div class="fake-map">
        <div class="map-grid"></div>
        <div class="rider-marker">
          <van-icon name="logistics" size="32" color="#07c160" />
          <div class="pulse-ring"></div>
        </div>
        <div class="dest-marker">
          <van-icon name="location-o" size="28" color="#ee0a24" />
        </div>
        <div class="map-info">
          <div class="coord-label">当前位置</div>
          <div class="coord-value">{{ coords }}</div>
          <div class="reported-info">已上报 {{ reportedCount }} 次 | 配送中 {{ elapsedStr }}</div>
        </div>
      </div>
    </div>

    <div class="delivery-info">
      <div class="info-card">
        <div class="info-row">
          <van-icon name="shop-o" color="#ff6b35" size="16" />
          <span class="info-label">取餐地址</span>
          <span class="info-val">{{ orderInfo.merchantName || '商家' }}</span>
        </div>
        <div class="info-row">
          <van-icon name="location-o" color="#409EFF" size="16" />
          <span class="info-label">送达地址</span>
          <span class="info-val">{{ orderInfo.address || '未知' }}</span>
        </div>
        <div class="info-row" v-if="orderInfo.estimatedMinutes">
          <van-icon name="clock-o" color="#ff976a" size="16" />
          <span class="info-label">预计送达</span>
          <span class="info-val">{{ orderInfo.estimatedMinutes }} 分钟</span>
        </div>
      </div>
    </div>

    <div class="track-progress">
      <van-steps direction="vertical" :active="activeStep">
        <van-step>
          <template #active-icon><van-icon name="checked" color="#07c160"/></template>
          <template #inactive-icon><van-icon name="shop-o"/></template>
          <h4>取餐</h4>
          <p>到店取餐，确认餐品</p>
        </van-step>
        <van-step>
          <template #active-icon><van-icon name="logistics" color="#409EFF"/></template>
          <template #inactive-icon><van-icon name="location-o"/></template>
          <h4>配送中</h4>
          <p>正在为您配送，位置实时上报中</p>
        </van-step>
        <van-step>
          <template #inactive-icon><van-icon name="checked"/></template>
          <h4>完成</h4>
          <p>送达确认，配送完成</p>
        </van-step>
      </van-steps>
    </div>

    <div class="bottom-action" v-if="activeStep < 2">
      <van-button type="success" round block size="large" @click="completeDelivery">
        确认送达
      </van-button>
    </div>

    <div v-if="activeStep >= 2" class="completed-banner">
      <van-icon name="checked" size="48" color="#07c160" />
      <h3>配送完成</h3>
      <p>感谢您的辛勤付出</p>
      <van-button round type="primary" @click="router.push('/tasks')">返回任务列表</van-button>
    </div>
  </div>
</template>

<style scoped>
.track-page { padding-bottom: 80px; }
.map-area { margin: 8px; }
.fake-map {
  position: relative; height: 200px; background: #e8f4ff; border-radius: 12px; overflow: hidden;
}
.map-grid {
  position: absolute; inset: 0;
  background-image:
    linear-gradient(rgba(64,158,255,0.1) 1px, transparent 1px),
    linear-gradient(90deg, rgba(64,158,255,0.1) 1px, transparent 1px);
  background-size: 30px 30px;
}
.rider-marker {
  position: absolute; top: 40%; left: 40%; text-align: center; z-index: 2;
}
.pulse-ring {
  width: 30px; height: 30px; border-radius: 50%; background: rgba(7,193,96,0.2);
  position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
  animation: pulse 2s infinite;
}
@keyframes pulse {
  0% { transform: translate(-50%, -50%) scale(1); opacity: 1; }
  100% { transform: translate(-50%, -50%) scale(2.5); opacity: 0; }
}
.dest-marker { position: absolute; bottom: 20%; right: 20%; z-index: 2; }
.map-info {
  position: absolute; bottom: 0; left: 0; right: 0;
  background: rgba(0,0,0,0.6); color: #fff; padding: 8px 12px; font-size: 11px;
}
.coord-label { font-size: 10px; opacity: 0.7; }
.coord-value { font-weight: 600; font-size: 12px; }
.reported-info { margin-top: 2px; opacity: 0.7; }

.delivery-info { padding: 0 8px; }
.info-card { background: #fff; border-radius: 8px; padding: 12px; }
.info-row { display: flex; align-items: center; gap: 8px; padding: 6px 0; }
.info-label { font-size: 12px; color: #999; width: 60px; }
.info-val { font-size: 13px; color: #333; flex: 1; }

.track-progress { padding: 16px; background: #fff; margin: 8px; border-radius: 8px; }
.track-progress h4 { font-size: 14px; margin-bottom: 2px; }
.track-progress p { font-size: 12px; color: #999; }

.bottom-action { padding: 12px 16px; }
.completed-banner { text-align: center; padding: 40px 16px; }
.completed-banner h3 { margin: 12px 0 4px; font-size: 18px; }
.completed-banner p { color: #999; margin-bottom: 20px; }
</style>
