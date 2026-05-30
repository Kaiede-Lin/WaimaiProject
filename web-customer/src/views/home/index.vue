<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { loadAmap, getBrowserLocation } from '@/utils/amap'
import { calcDeliveryFee, calcDeliveryTime, saveLocation, getStoredLocation, getLastAddress } from '@/utils/delivery'

const router = useRouter()
const merchants = ref<any[]>([])
const allMerchants = ref<any[]>([])
const loading = ref(false)
const searchText = ref('')
const sortBy = ref<'default' | 'score' | 'sales' | 'distance'>('default')
const currentLoc = ref({ lng: 116.397, lat: 39.908 })
const currentAddress = ref('正在定位...')

const mapContainer = ref<HTMLDivElement>()
let mapInstance: any = null
let userMarker: any = null
const merchantMarkers = ref<any[]>([])

function getA() {
  return (window as any).AMap || window.AMap
}

const filteredMerchants = computed(() => {
  let list = [...allMerchants.value]

  if (searchText.value) {
    const kw = searchText.value.toLowerCase()
    list = list.filter(m => m.name?.toLowerCase().includes(kw) || m.description?.toLowerCase().includes(kw))
  }

  switch (sortBy.value) {
    case 'score': list.sort((a, b) => (b.score || 0) - (a.score || 0)); break
    case 'sales': list.sort((a, b) => (b.monthlySales || 0) - (a.monthlySales || 0)); break
    case 'distance': list.sort((a, b) => (a.distanceKm || 0) - (b.distanceKm || 0)); break
  }

  return list
})

async function fetch() {
  loading.value = true
  try {
    const res: any = await request.get('/merchant/nearby', {
      params: { lng: currentLoc.value.lng, lat: currentLoc.value.lat, radius: 10 }
    })
    allMerchants.value = res.data || []
    if (mapInstance) addMerchantMarkers()
  } catch { /* ignore */ }
  loading.value = false
}

function addMerchantMarkers() {
  if (!mapInstance) return
  const A = getA()
  merchantMarkers.value.forEach((m: any) => mapInstance.remove(m))
  merchantMarkers.value = []
  const list = allMerchants.value.filter((m: any) => m.longitude && m.latitude)
  if (!list.length) return
  list.forEach((m: any) => {
    const lng = Number(m.longitude)
    const lat = Number(m.latitude)
    const marker = new A.Marker({
      position: [lng, lat],
      title: m.name,
      content: `<div style="background:#fff;border:2px solid #409EFF;border-radius:12px;padding:2px 8px;font-size:11px;white-space:nowrap;box-shadow:0 2px 6px rgba(0,0,0,.15);color:#333">${m.name}</div>`,
      offset: new A.Pixel(-30, -18)
    })
    marker.on('click', () => router.push(`/merchant/${m.id}`))
    mapInstance.add(marker)
    merchantMarkers.value.push(marker)
  })
  mapInstance.setFitView(merchantMarkers.value, false)
}

async function initMap() {
  if (!mapContainer.value) return
  // Use stored location if available (from address selection), otherwise try GPS
  const stored = getStoredLocation()
  if (stored.lng !== 116.397428 || stored.lat !== 39.90923) {
    currentLoc.value = stored
  } else {
    const pos = await getBrowserLocation()
    currentLoc.value = { lng: pos.lng, lat: pos.lat }
    saveLocation(pos.lng, pos.lat)
  }
  currentAddress.value = getLastAddress() || `${currentLoc.value.lat.toFixed(4)}, ${currentLoc.value.lng.toFixed(4)}`
  try {
    await loadAmap()
  } catch {
    return
  }
  await nextTick()
  if (!mapContainer.value) return

  const A = getA()
  mapInstance = new A.Map(mapContainer.value, {
    zoom: 13,
    center: [currentLoc.value.lng, currentLoc.value.lat],
    resizeEnable: true,
    viewMode: '2D'
  })

  userMarker = new A.Marker({
    position: [currentLoc.value.lng, currentLoc.value.lat],
    content: `<div style="width:16px;height:16px;background:#409EFF;border:3px solid #fff;border-radius:50%;box-shadow:0 2px 8px rgba(64,158,255,.5)"></div>`,
    offset: new A.Pixel(-8, -8)
  })
  mapInstance.add(userMarker)

  await fetch()
}

function goMerchant(id: number) {
  if (!id) return
  router.push(`/merchant/${id}`)
}

function getSortLabel() {
  const map: any = { default: '默认', score: '评分最高', sales: '销量最高', distance: '距离最近' }
  return map[sortBy.value]
}

function cycleSort() {
  const order: any[] = ['default', 'score', 'sales', 'distance']
  const idx = order.indexOf(sortBy.value)
  sortBy.value = order[(idx + 1) % order.length]
}

onMounted(async () => {
  await nextTick()
  initMap()
})

onBeforeUnmount(() => {
  if (mapInstance) mapInstance.destroy()
})
</script>

<template>
  <div class="home-page">
    <van-nav-bar title="美食外卖" fixed placeholder>
      <template #right>
        <van-icon name="user-o" size="20" @click="router.push('/profile')" />
      </template>
    </van-nav-bar>

    <div class="address-bar" @click="router.push('/address')">
      <van-icon name="location-o" size="16" color="#409EFF" />
      <span class="address-text">{{ currentAddress }}</span>
      <van-icon name="arrow" size="12" color="#999" />
    </div>

    <div class="search-bar">
      <van-search
        v-model="searchText"
        placeholder="搜索商家名称"
        shape="round"
        background="transparent"
      />
      <div class="filter-bar">
        <span :class="['filter-item', { active: sortBy !== 'default' }]" @click="cycleSort">
          {{ getSortLabel() }} <van-icon name="arrow-down" size="10" />
        </span>
        <span class="filter-item">配送费</span>
        <span class="filter-item">筛选</span>
      </div>
    </div>

    <div ref="mapContainer" class="home-map"></div>

    <van-pull-refresh v-model="loading" @refresh="fetch">
      <div class="merchant-list">
        <div v-for="m in filteredMerchants" :key="m.id" class="merchant-card" @click="goMerchant(m.id)">
          <van-image :src="m.logo || 'data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 80 80%22%3E%3Crect fill=%22%23fff0e8%22 width=%2280%22 height=%2280%22/%3E%3Crect x=%2220%22 y=%2220%22 width=%2240%22 height=%2240%22 rx=%228%22 fill=%22%23ff6b35%22/%3E%3Ctext x=%2240%22 y=%2248%22 text-anchor=%22middle%22 fill=%22white%22 font-size=%2228%22%3E店%3C/text%3E%3C/svg%3E'" width="72" height="72" radius="8" />
          <div class="merchant-info">
            <div class="merchant-name">
              {{ m.name }}
              <van-tag v-if="m.score >= 4.5" type="danger" size="mini">优选</van-tag>
            </div>
            <div class="merchant-meta">
              <van-rate v-model="m.score" readonly size="12" allow-half :color="'#ffc800'" />
              <span class="score-text">{{ m.score?.toFixed(1) || '5.0' }}</span>
              <span class="divider">|</span>
              <span class="sales">月售{{ m.monthlySales || 0 }}单</span>
            </div>
            <div class="merchant-footer">
              <span class="delivery">
                <van-icon name="clock-o" size="12" />
                {{ calcDeliveryTime(m.distanceKm || 0) }}分钟
              </span>
              <span class="delivery-fee">配送费 ¥{{ calcDeliveryFee(m.distanceKm || 0) }}</span>
              <span class="distance">{{ (m.distanceKm || 0).toFixed(1) }}km</span>
            </div>
            <div v-if="m.description" class="merchant-desc">{{ m.description }}</div>
          </div>
        </div>
        <van-empty v-if="!loading && filteredMerchants.length === 0" description="暂无商家" />
      </div>
    </van-pull-refresh>

    <van-tabbar route>
      <van-tabbar-item to="/" icon="home-o" name="home">首页</van-tabbar-item>
      <van-tabbar-item to="/order/list" icon="orders-o" name="orders">订单</van-tabbar-item>
      <van-tabbar-item to="/profile" icon="user-o" name="profile">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<style scoped>
.home-page { padding-bottom: 50px; }
.address-bar {
  display: flex; align-items: center; gap: 6px; padding: 10px 16px;
  background: linear-gradient(135deg, #e8f4ff, #f0f8ff); cursor: pointer;
  margin: 8px; border-radius: var(--radius); font-size: 13px;
  box-shadow: var(--shadow-sm);
}
.address-text { flex: 1; color: var(--text); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.search-bar { background: var(--card-bg); position: sticky; top: 46px; z-index: 10; }
.filter-bar { display: flex; gap: 0; padding: 0 16px 8px; font-size: 13px; }
.filter-item { flex: 1; text-align: center; color: var(--text-secondary); cursor: pointer; padding: 4px 0; transition: color 0.2s; }
.filter-item.active { color: var(--info); font-weight: 600; }
.home-map { height: 200px; margin: 0 8px; border-radius: var(--radius); overflow: hidden; }
.merchant-list { padding: 8px; }
.merchant-card {
  display: flex; gap: 12px; padding: 14px; background: var(--card-bg);
  border-radius: var(--radius); margin-bottom: 10px; cursor: pointer;
  box-shadow: var(--shadow-sm); transition: transform 0.15s, box-shadow 0.2s;
}
.merchant-card:active { transform: scale(0.99); box-shadow: var(--shadow); }
.merchant-info { flex: 1; display: flex; flex-direction: column; gap: 4px; overflow: hidden; }
.merchant-name { font-size: 15px; font-weight: 600; display: flex; align-items: center; gap: 6px; color: var(--text); }
.merchant-meta { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--text-secondary); }
.score-text { color: #ffc800; font-weight: 600; }
.divider { color: #eee; }
.merchant-footer { display: flex; align-items: center; gap: 12px; font-size: 11px; color: var(--text-secondary); }
.delivery-fee { color: #666; }
.distance { color: var(--info); font-weight: 500; margin-left: auto; }
.merchant-desc { font-size: 11px; color: #aaa; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
