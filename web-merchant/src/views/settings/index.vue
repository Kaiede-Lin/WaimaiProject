<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast, showConfirmDialog } from 'vant'
import request from '@/utils/request'
import LocationPicker from '@/components/map/LocationPicker.vue'

const router = useRouter()
const info = ref<any>({})
const hours = ref('')
const saving = ref(false)
const showMapPicker = ref(false)
const mapPickerInitialLng = ref(116.397428)
const mapPickerInitialLat = ref(39.90923)

async function fetch() {
  try {
    const res: any = await request.get('/merchant/info')
    info.value = res.data || {}
    hours.value = info.value.businessHours || '09:00-22:00'
  } catch {}
}

async function saveHours() {
  if (!hours.value) { showToast('请输入营业时间'); return }
  saving.value = true
  try {
    await request.put('/merchant/business-hours', { businessHours: hours.value })
    showSuccessToast('营业时间已保存')
  } catch {}
  saving.value = false
}

async function saveInfo() {
  saving.value = true
  try {
    await request.put('/merchant/info', {
      name: info.value.name,
      phone: info.value.phone,
      address: info.value.address,
      description: info.value.description,
      longitude: info.value.longitude,
      latitude: info.value.latitude
    })
    showSuccessToast('店铺信息已保存')
  } catch {}
  saving.value = false
}

async function openMapPicker() {
  // Try GPS first (triggers permission prompt), then stored coords, then Beijing default
  let gpsPos: { lng: number; lat: number } | null = null
  if (navigator.geolocation) {
    gpsPos = await new Promise((resolve) => {
      navigator.geolocation.getCurrentPosition(
        (p) => resolve({ lng: p.coords.longitude, lat: p.coords.latitude }),
        () => resolve(null),
        { timeout: 5000, maximumAge: 0 }
      )
    })
  }
  mapPickerInitialLng.value = gpsPos?.lng
    ?? (info.value.longitude ? Number(info.value.longitude) : 116.397428)
  mapPickerInitialLat.value = gpsPos?.lat
    ?? (info.value.latitude ? Number(info.value.latitude) : 39.90923)
  showMapPicker.value = true
}

function onLocationPicked(data: { address: string; longitude: number; latitude: number }) {
  info.value.address = data.address
  info.value.longitude = data.longitude
  info.value.latitude = data.latitude
}

function handleLogout() {
  showConfirmDialog({ title: '确认退出登录？' }).then(() => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    router.replace('/login')
  }).catch(() => {})
}

onMounted(fetch)
</script>

<template>
  <div class="settings-page">
    <van-nav-bar title="店铺设置" fixed placeholder>
      <template #right>
        <van-icon name="user-o" size="20" @click="handleLogout" />
      </template>
    </van-nav-bar>

    <van-cell-group inset title="营业状态">
      <van-cell title="当前状态">
        <template #value>
          <van-tag :type="info.status === 1 ? 'success' : 'default'" size="medium">
            {{ info.status === 1 ? '营业中' : '已休息' }}
          </van-tag>
        </template>
      </van-cell>
    </van-cell-group>

    <van-cell-group inset title="营业时间">
      <van-field v-model="hours" label="时间段" placeholder="如 09:00-22:00" />
      <div class="cell-footer">
        <van-button type="primary" size="small" :loading="saving" @click="saveHours">保存营业时间</van-button>
      </div>
    </van-cell-group>

    <van-cell-group inset title="基本信息">
      <van-field v-model="info.name" label="店铺名称" placeholder="输入店铺名称" />
      <van-field v-model="info.phone" label="联系电话" placeholder="输入联系电话" type="tel" />
      <van-field v-model="info.address" label="店铺地址" placeholder="输入店铺地址" />
      <div class="cell-footer map-pick-row">
        <van-button size="small" type="primary" plain icon="location-o" @click="openMapPicker">
          在地图上定位
        </van-button>
        <span v-if="info.longitude && info.latitude" class="coord-tag">
          已定位: {{ Number(info.longitude).toFixed(6) }}, {{ Number(info.latitude).toFixed(6) }}
        </span>
      </div>
      <van-field v-model="info.description" label="店铺简介" placeholder="介绍一下您的店铺" type="textarea" rows="2" />
      <div class="cell-footer">
        <van-button type="primary" size="small" :loading="saving" @click="saveInfo">保存信息</van-button>
      </div>
    </van-cell-group>

    <van-cell-group inset title="更多信息" style="margin-bottom: 70px">
      <van-cell title="评分" :value="(info.score || 5.0).toFixed(1)" />
      <van-cell title="月销量" :value="(info.monthlySales || 0) + ' 单'" />
      <van-cell title="入驻时间" :value="(info.createTime || '').substring(0, 10)" />
    </van-cell-group>

    <van-tabbar route>
      <van-tabbar-item to="/" icon="home-o">首页</van-tabbar-item>
      <van-tabbar-item to="/orders" icon="orders-o">订单</van-tabbar-item>
      <van-tabbar-item to="/menu" icon="wap-home-o">菜单</van-tabbar-item>
      <van-tabbar-item to="/reports" icon="chart-trending-o">报表</van-tabbar-item>
    </van-tabbar>

    <LocationPicker
      v-model="showMapPicker"
      :initial-lng="mapPickerInitialLng"
      :initial-lat="mapPickerInitialLat"
      @confirm="onLocationPicked"
    />
  </div>
</template>

<style scoped>
.settings-page { padding-bottom: 50px; }
.cell-footer { padding: 8px 16px; text-align: right; }
.map-pick-row { display: flex; align-items: center; gap: 8px; text-align: left; }
.coord-tag { font-size: 11px; color: #999; }
</style>
