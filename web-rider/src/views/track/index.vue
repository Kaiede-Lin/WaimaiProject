<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showSuccessToast } from 'vant'
import request from '@/utils/request'
import { loadAmap } from '@/utils/amap'

const route = useRoute()
const router = useRouter()
const orderNo = route.params.orderNo as string

const lng = ref(116.397428)
const lat = ref(39.90923)
const orderInfo = ref<any>({})
const merchantInfo = ref<any>({})
const activeStep = ref(0)
const reportedCount = ref(0)
const deliveryTime = ref(0)
const mapReady = ref(false)
const routeDistance = ref(0)
const routeDuration = ref(0)

let timer: any = null
let timeCounter: any = null
let mapInstance: any = null
let riderMarker: any = null
let merchantMarker: any = null
let destMarker: any = null
let routePolyline: any = null
let geoWatchId: number | null = null
let lastGpsTime = 0
let routeSeq = 0

function getA() { return (window as any).AMap || window.AMap }

const coords = computed(() => `${lng.value.toFixed(6)}, ${lat.value.toFixed(6)}`)
const elapsedStr = computed(() => {
  const m = Math.floor(deliveryTime.value / 60)
  const s = deliveryTime.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})
const distanceKm = computed(() => (routeDistance.value / 1000).toFixed(1))
const durationMin = computed(() => Math.round(routeDuration.value / 60))

const navPhase = computed(() => activeStep.value === 0 ? '取餐' : '配送')
const navTargetName = computed(() =>
  activeStep.value === 0
    ? (merchantInfo.value.name || '商家')
    : (orderInfo.value.address || '顾客')
)

// ── Data ──

async function fetchOrder() {
  try {
    const res: any = await request.get(`/order/${orderNo}`)
    orderInfo.value = res.data || {}
    if (orderInfo.value.status === 'ACCEPTED') activeStep.value = 0
    else if (orderInfo.value.status === 'DELIVERING') activeStep.value = 1
    else if (orderInfo.value.status === 'COMPLETED') activeStep.value = 2
    if (orderInfo.value.merchantId) fetchMerchant(orderInfo.value.merchantId)
    if (mapReady.value) refreshAll()
  } catch {}
}

async function fetchMerchant(merchantId: number) {
  try {
    const res: any = await request.get(`/merchant/${merchantId}`)
    merchantInfo.value = res.data || {}
    if (mapReady.value) refreshAll()
  } catch {}
}

// ── Map ──

async function initMap(position: { lng: number; lat: number }) {
  try { await loadAmap() } catch { showToast('地图加载失败'); return }
  await nextTick()
  const el = document.getElementById('track-map')
  if (!el) return

  const A = getA()
  mapInstance = new A.Map(el, {
    zoom: 15,
    center: [position.lng, position.lat],
    resizeEnable: true,
    viewMode: '2D'
  })

  riderMarker = new A.Marker({
    position: [position.lng, position.lat],
    content: `<div style="width:22px;height:22px;background:#07c160;border:3px solid #fff;border-radius:50%;box-shadow:0 2px 10px rgba(7,193,96,.5);animation:pulse 2s infinite"></div>`,
    offset: new A.Pixel(-11, -11),
    zIndex: 100
  })
  mapInstance.add(riderMarker)

  mapReady.value = true
  lastGpsTime = Date.now()

  if (orderInfo.value.id || merchantInfo.value.id) refreshAll()
}

function refreshAll() {
  placeMerchantMarker()
  placeDestMarker()
  fitAllMarkers()
  fetchRouteFromBackend()
}

function placeMerchantMarker() {
  if (!mapInstance || !merchantInfo.value) return
  const A = getA()
  if (merchantMarker) mapInstance.remove(merchantMarker)
  const mlng = Number(merchantInfo.value.longitude)
  const mlat = Number(merchantInfo.value.latitude)
  if (isNaN(mlng) || isNaN(mlat) || mlng === 0) return

  merchantMarker = new A.Marker({
    position: [mlng, mlat],
    content: `<div style="display:flex;align-items:center;justify-content:center;width:30px;height:34px"><svg viewBox="0 0 24 24" width="30" height="34"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z" fill="#ff6b35" stroke="#fff" stroke-width="1.5"/><circle cx="12" cy="9" r="3" fill="#fff"/></svg></div>`,
    offset: new A.Pixel(-15, -34),
    title: merchantInfo.value.name || '商家',
    zIndex: 50
  })
  mapInstance.add(merchantMarker)
}

function placeDestMarker() {
  if (!mapInstance) return
  const A = getA()
  if (destMarker) mapInstance.remove(destMarker)
  const dlng = Number(orderInfo.value.addressLng)
  const dlat = Number(orderInfo.value.addressLat)
  if (isNaN(dlng) || isNaN(dlat) || dlng === 0) return

  destMarker = new A.Marker({
    position: [dlng, dlat],
    content: `<div style="display:flex;align-items:center;justify-content:center;width:30px;height:34px"><svg viewBox="0 0 24 24" width="30" height="34"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z" fill="#ee0a24" stroke="#fff" stroke-width="1.5"/><circle cx="12" cy="9" r="3" fill="#fff"/></svg></div>`,
    offset: new A.Pixel(-15, -34),
    title: orderInfo.value.address || '送达地址',
    zIndex: 50
  })
  mapInstance.add(destMarker)
}

function fitAllMarkers() {
  const markers = [riderMarker, merchantMarker, destMarker].filter(Boolean)
  if (markers.length >= 2 && mapInstance) {
    mapInstance.setFitView(markers, false, [60, 60, 60, 60])
  }
}

// ── Get target coordinates ──

function getTargetOrigin(): string | null {
  if (!riderMarker) return null
  const pos = riderMarker.getPosition()
  if (!pos) return null
  return `${pos.lng},${pos.lat}`
}

function getTargetDest(): string | null {
  // Phase 0: navigate to merchant
  if (activeStep.value === 0 && merchantMarker) {
    const p = merchantMarker.getPosition()
    return p ? `${p.lng},${p.lat}` : null
  }
  // Phase 1: navigate to customer
  if (activeStep.value >= 1 && destMarker) {
    const p = destMarker.getPosition()
    return p ? `${p.lng},${p.lat}` : null
  }
  if (destMarker) {
    const p = destMarker.getPosition()
    return p ? `${p.lng},${p.lat}` : null
  }
  if (merchantMarker) {
    const p = merchantMarker.getPosition()
    return p ? `${p.lng},${p.lat}` : null
  }
  return null
}

// ── Route: call REST API via backend proxy ──

async function fetchRouteFromBackend() {
  const origin = getTargetOrigin()
  const dest = getTargetDest()
  if (!origin || !dest) return

  // Clear previous route
  clearRoute()

  const seq = ++routeSeq
  try {
    const res: any = await request.get('/rider/direction/driving', {
      params: { origin, destination: dest }
    })
    if (seq !== routeSeq) return // stale response

    // Parse Amap JSON response wrapped in Result.data
    const amapResp = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
    if (amapResp.status !== '1' || !amapResp.route?.paths?.length) {
      drawStraightLine(origin, dest)
      return
    }

    const paths = amapResp.route.paths
    const best = paths[0]
    routeDistance.value = parseInt(best.distance) || 0
    routeDuration.value = parseInt(best.duration) || 0

    // Build path from all step polylines
    const points: [number, number][] = []
    for (const step of best.steps) {
      if (!step.polyline) continue
      for (const pair of step.polyline.split(';')) {
        const [plng, plat] = pair.split(',').map(Number)
        if (!isNaN(plng) && !isNaN(plat)) {
          points.push([plng, plat])
        }
      }
    }

    if (points.length < 2) {
      drawStraightLine(origin, dest)
      return
    }

    drawPolyline(points)
  } catch {
    drawStraightLine(origin, dest)
  }
}

function clearRoute() {
  if (routePolyline && mapInstance) {
    mapInstance.remove(routePolyline)
    routePolyline = null
  }
}

function drawPolyline(points: [number, number][]) {
  if (!mapInstance) return
  clearRoute()
  const A = getA()
  routePolyline = new A.Polyline({
    path: points,
    strokeColor: '#409EFF',
    strokeWeight: 6,
    strokeOpacity: 0.85,
    lineJoin: 'round',
    borderWeight: 2,
    borderColor: '#fff',
    showDir: true
  })
  mapInstance.add(routePolyline)
  routePolyline.setPath(points)
}

function drawStraightLine(originStr: string, destStr: string) {
  if (!mapInstance) return
  clearRoute()
  const [olng, olat] = originStr.split(',').map(Number)
  const [dlng, dlat] = destStr.split(',').map(Number)
  const A = getA()
  routePolyline = new A.Polyline({
    path: [[olng, olat], [dlng, dlat]],
    strokeColor: '#409EFF',
    strokeWeight: 5,
    strokeStyle: 'dashed',
    strokeOpacity: 0.7,
    lineJoin: 'round'
  })
  mapInstance.add(routePolyline)
}

// ── GPS ──

function reportLocation(pos?: { lng: number; lat: number }) {
  const p = pos || { lng: lng.value, lat: lat.value }
  lng.value = p.lng
  lat.value = p.lat
  if (riderMarker) riderMarker.setPosition([p.lng, p.lat])
  request.post('/rider/location', { longitude: p.lng, latitude: p.lat }).catch(() => {})
  if (orderInfo.value.id) {
    request.post('/rider/track/report', {
      orderId: orderInfo.value.id,
      longitude: p.lng,
      latitude: p.lat
    }).then(() => { reportedCount.value++ }).catch(() => {})
  }
  lastGpsTime = Date.now()
}

function startGps() {
  if (!navigator.geolocation) {
    initMap({ lng: 116.397428, lat: 39.90923 })
    return
  }
  geoWatchId = navigator.geolocation.watchPosition(
    (pos) => {
      const p = { lng: pos.coords.longitude, lat: pos.coords.latitude }
      reportLocation(p)
      if (!mapReady.value) initMap(p)
    },
    () => {
      if (!mapReady.value) initMap({ lng: 116.397428, lat: 39.90923 })
    },
    { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 }
  )
}

// ── Actions ──

async function confirmPickup() {
  try {
    await request.post(`/rider/order/${orderNo}/pickup`)
    activeStep.value = 1
    orderInfo.value.status = 'DELIVERING'
    // Re-route to customer
    fetchRouteFromBackend()
    showSuccessToast('已取餐，开始配送！')
  } catch {}
}

async function completeDelivery() {
  try {
    await request.post(`/rider/order/${orderNo}/complete`)
    activeStep.value = 2
    showSuccessToast('配送完成！')
    setTimeout(() => router.push('/tasks'), 1500)
  } catch {}
}

// ── Lifecycle ──

onMounted(() => {
  fetchOrder()
  startGps()
  timer = setInterval(() => {
    if (!mapReady.value) return
    if (Date.now() - lastGpsTime > 20000) reportLocation()
  }, 15000)
  timeCounter = setInterval(() => { deliveryTime.value++ }, 1000)
})

onBeforeUnmount(() => {
  clearInterval(timer)
  clearInterval(timeCounter)
  if (geoWatchId !== null) navigator.geolocation.clearWatch(geoWatchId)
  clearRoute()
  if (mapInstance) mapInstance.destroy()
  mapInstance = null
})
</script>

<template>
  <div class="track-page">
    <van-nav-bar title="配送导航" left-arrow @click-left="router.back()" fixed placeholder />

    <!-- Map -->
    <div class="map-area">
      <div id="track-map" class="real-map" :class="{ ready: mapReady }"></div>

      <!-- Navigation banner -->
      <div v-if="routeDistance > 0 && mapReady" class="nav-banner" :class="activeStep === 0 ? 'to-merchant' : 'to-customer'">
        <span>{{ navPhase }}：{{ navTargetName }}</span>
        <span class="nav-stats">{{ distanceKm }}km / {{ durationMin }}分钟</span>
      </div>

      <!-- Bottom overlay -->
      <div class="map-overlay-info">
        <span>{{ coords }} · 第{{ reportedCount }}次上报 · {{ elapsedStr }}</span>
      </div>

      <div v-if="!mapReady" class="map-loading">
        <van-loading size="24" color="#409EFF" />
        <span>正在获取位置...</span>
      </div>
    </div>

    <!-- Info card -->
    <div class="delivery-info">
      <div class="info-card">
        <div class="info-row">
          <van-icon name="shop-o" color="#ff6b35" size="16" />
          <span class="info-label">取餐</span>
          <span class="info-val">{{ orderInfo.merchantName || merchantInfo.name || '商家' }}</span>
        </div>
        <div v-if="merchantInfo.address" class="info-row">
          <span class="info-label"></span>
          <span class="info-val sub">{{ merchantInfo.address }}</span>
        </div>
        <div class="info-row">
          <van-icon name="location-o" color="#409EFF" size="16" />
          <span class="info-label">送达</span>
          <span class="info-val">{{ orderInfo.address || '未知' }}</span>
        </div>
        <div class="info-row" v-if="orderInfo.estimatedMinutes">
          <van-icon name="clock-o" color="#ff976a" size="16" />
          <span class="info-label">预计</span>
          <span class="info-val">{{ orderInfo.estimatedMinutes }} 分钟</span>
        </div>
      </div>
    </div>

    <!-- Progress -->
    <div class="track-progress">
      <van-steps direction="vertical" :active="activeStep">
        <van-step>
          <template #active-icon><van-icon name="shop-o" color="#ff6b35"/></template>
          <template #inactive-icon><van-icon name="shop-o"/></template>
          <h4>前往取餐</h4>
          <p>导航至 {{ merchantInfo.name || '商家' }}，到达后确认取餐</p>
        </van-step>
        <van-step>
          <template #active-icon><van-icon name="logistics" color="#409EFF"/></template>
          <template #inactive-icon><van-icon name="location-o"/></template>
          <h4>配送中</h4>
          <p>导航至 {{ orderInfo.address || '顾客' }}，到达后确认送达</p>
        </van-step>
        <van-step>
          <template #inactive-icon><van-icon name="checked"/></template>
          <h4>完成</h4>
          <p>配送完成</p>
        </van-step>
      </van-steps>
    </div>

    <!-- Actions -->
    <div class="bottom-action" v-if="activeStep === 0">
      <van-button type="warning" round block size="large" @click="confirmPickup">
        确认取餐，开始配送
      </van-button>
    </div>
    <div class="bottom-action" v-if="activeStep === 1">
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

.map-area {
  margin: 8px; position: relative; border-radius: 12px; overflow: hidden;
  background: #e8f4ff; min-height: 300px;
}
.real-map { width: 100%; height: 320px; opacity: 0; transition: opacity .3s; }
.real-map.ready { opacity: 1; }

.map-loading {
  position: absolute; inset: 0; display: flex; align-items: center;
  justify-content: center; gap: 8px; font-size: 13px; color: #666;
  background: #e8f4ff; z-index: 5;
}

.nav-banner {
  position: absolute; top: 8px; left: 50%; transform: translateX(-50%); z-index: 10;
  display: flex; flex-direction: column; align-items: center; gap: 2px;
  padding: 6px 16px; border-radius: 20px; font-size: 12px; font-weight: 600;
  white-space: nowrap; box-shadow: 0 2px 8px rgba(0,0,0,.15);
}
.nav-banner.to-merchant { background: #fff7e8; color: #ff6b35; }
.nav-banner.to-customer { background: #e8f4ff; color: #409EFF; }
.nav-stats { font-size: 11px; font-weight: 400; opacity: 0.8; }

.map-overlay-info {
  position: absolute; bottom: 0; left: 0; right: 0; z-index: 5;
  background: rgba(0,0,0,0.6); color: #fff; padding: 8px 12px; font-size: 11px;
}

.delivery-info { padding: 0 8px; }
.info-card { background: #fff; border-radius: 8px; padding: 12px; }
.info-row { display: flex; align-items: center; gap: 8px; padding: 4px 0; }
.info-label { font-size: 12px; color: #999; width: 40px; }
.info-val { font-size: 13px; color: #333; flex: 1; }
.info-val.sub { font-size: 11px; color: #999; }

.track-progress { padding: 16px; background: #fff; margin: 8px; border-radius: 8px; }
.track-progress h4 { font-size: 14px; margin-bottom: 2px; }
.track-progress p { font-size: 12px; color: #999; }

.bottom-action { padding: 12px 16px; }
.completed-banner { text-align: center; padding: 40px 16px; }
.completed-banner h3 { margin: 12px 0 4px; font-size: 18px; }
.completed-banner p { color: #999; margin-bottom: 20px; }
</style>

<style>
@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(7,193,96,.4); }
  100% { box-shadow: 0 0 0 12px rgba(7,193,96,0); }
}
</style>
