<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast, showConfirmDialog } from 'vant'
import request from '@/utils/request'
import LocationPicker from '@/components/map/LocationPicker.vue'
import { setLocationFromAddress } from '@/utils/delivery'

const router = useRouter()
const addresses = ref<any[]>([])
const showDialog = ref(false)
const editingAddr = ref<any>({})
const form = ref({
  name: '', phone: '', address: '', detail: '',
  longitude: null as number | null, latitude: null as number | null,
  isDefault: false
})
const loading = ref(false)
const showMapPicker = ref(false)
const mapPickerInitialLng = ref(116.397428)
const mapPickerInitialLat = ref(39.90923)

async function fetchAddresses() {
  loading.value = true
  try {
    const res: any = await request.get('/user/address')
    addresses.value = res.data || []
  } catch { /* ignore */ }
  loading.value = false
}

function openDialog(addr?: any) {
  if (addr) {
    form.value = { ...addr }
    editingAddr.value = addr
  } else {
    form.value = {
      name: '', phone: '', address: '', detail: '',
      longitude: null, latitude: null, isDefault: false
    }
    editingAddr.value = {}
  }
  showDialog.value = true
}

async function openMapPicker() {
  // Try GPS first (triggers permission prompt), fallback to form coords, then Beijing
  if (navigator.geolocation) {
    const gpsPos = await new Promise<{ lng: number; lat: number } | null>((resolve) => {
      navigator.geolocation.getCurrentPosition(
        (p) => resolve({ lng: p.coords.longitude, lat: p.coords.latitude }),
        () => resolve(null),
        { timeout: 8000, maximumAge: 0 }
      )
    })
    if (gpsPos) {
      mapPickerInitialLng.value = form.value.longitude || gpsPos.lng
      mapPickerInitialLat.value = form.value.latitude || gpsPos.lat
    } else {
      mapPickerInitialLng.value = form.value.longitude || 116.397428
      mapPickerInitialLat.value = form.value.latitude || 39.90923
    }
  } else {
    mapPickerInitialLng.value = form.value.longitude || 116.397428
    mapPickerInitialLat.value = form.value.latitude || 39.90923
  }
  showMapPicker.value = true
}

function onLocationPicked(data: { address: string; longitude: number; latitude: number }) {
  form.value.address = data.address
  form.value.longitude = data.longitude
  form.value.latitude = data.latitude
}

async function saveAddress() {
  if (!form.value.name || !form.value.phone || !form.value.address) {
    showToast('请填写完整信息')
    return
  }
  try {
    if (editingAddr.value.id) {
      await request.put(`/user/address/${editingAddr.value.id}`, form.value)
    } else {
      await request.post('/user/address', form.value)
    }
    showSuccessToast('保存成功')
    showDialog.value = false
    fetchAddresses()
  } catch { /* ignore */ }
}

async function deleteAddress(id: number) {
  await showConfirmDialog({ title: '确认删除？' })
  await request.delete(`/user/address/${id}`)
  fetchAddresses()
}

function selectAddress(addr: any) {
  setLocationFromAddress(addr)
  localStorage.setItem('selectedAddressId', String(addr.id))
  router.back()
}

onMounted(fetchAddresses)
</script>

<template>
  <div class="address-page">
    <van-nav-bar title="收货地址" left-arrow @click-left="router.back()" fixed placeholder />

    <div v-for="addr in addresses" :key="addr.id" class="address-card" @click="selectAddress(addr)">
      <div class="addr-header">
        <span class="addr-name">{{ addr.name }}</span>
        <span class="addr-phone">{{ addr.phone }}</span>
        <van-tag v-if="addr.isDefault" type="primary" size="mini">默认</van-tag>
      </div>
      <div class="addr-text">{{ addr.address }} {{ addr.detail }}</div>
      <div class="addr-actions">
        <van-button size="mini" plain @click.stop="openDialog(addr)">编辑</van-button>
        <van-button size="mini" plain type="danger" @click.stop="deleteAddress(addr.id)">删除</van-button>
      </div>
    </div>

    <van-empty v-if="!loading && addresses.length === 0" description="暂无收货地址" />

    <div style="padding: 16px">
      <van-button type="primary" round block @click="openDialog()">添加新地址</van-button>
    </div>

    <van-dialog v-model:show="showDialog" :title="editingAddr.id ? '编辑地址' : '新增地址'" show-cancel-button @confirm="saveAddress">
      <van-field v-model="form.name" label="收货人" placeholder="请输入姓名" />
      <van-field v-model="form.phone" label="手机号" placeholder="请输入手机号" type="tel" />
      <van-field v-model="form.address" label="地址" placeholder="请输入详细地址" />
      <div class="map-pick-cell">
        <van-button size="small" type="primary" plain icon="location-o" @click="openMapPicker">
          在地图上选择位置
        </van-button>
        <span v-if="form.longitude && form.latitude" class="coord-tag">
          {{ form.longitude.toFixed(6) }}, {{ form.latitude.toFixed(6) }}
        </span>
      </div>
      <van-field v-model="form.detail" label="门牌号" placeholder="楼号/房间号（选填）" />
    </van-dialog>

    <LocationPicker
      v-model="showMapPicker"
      :initial-lng="mapPickerInitialLng"
      :initial-lat="mapPickerInitialLat"
      @confirm="onLocationPicked"
    />
  </div>
</template>

<style scoped>
.address-page { padding-bottom: 80px; }
.address-card {
  background: #fff; margin: 8px; padding: 14px; border-radius: 8px;
  cursor: pointer; position: relative;
}
.addr-header { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.addr-name { font-weight: 600; font-size: 15px; }
.addr-phone { color: #666; font-size: 13px; }
.addr-text { font-size: 13px; color: #333; line-height: 1.5; margin-bottom: 8px; }
.addr-actions { display: flex; gap: 8px; }
.map-pick-cell { padding: 4px 16px 0; display: flex; align-items: center; gap: 8px; }
.coord-tag { font-size: 11px; color: #999; }
</style>
