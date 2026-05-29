<script setup lang="ts">
import { ref, watch, nextTick, onBeforeUnmount } from 'vue'
import { showToast } from 'vant'
import { loadAmap, regeo } from '@/utils/amap'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    initialLng?: number
    initialLat?: number
  }>(),
  { initialLng: 116.397428, initialLat: 39.90923 }
)

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'confirm', data: { address: string; longitude: number; latitude: number }): void
}>()

let mapInstance: any = null
const mapContainer = ref<HTMLDivElement>()
const searchText = ref('')
const addressText = ref('正在获取地址...')
const pickerVisible = ref(false)

function getA() {
  return (window as any).AMap || window.AMap
}

watch(
  () => props.modelValue,
  async (val) => {
    if (val) {
      pickerVisible.value = true
      await nextTick()
      initMap()
    } else {
      destroyMap()
      pickerVisible.value = false
    }
  }
)

async function initMap() {
  try {
    await loadAmap()
  } catch {
    showToast('地图加载失败')
    return
  }
  await nextTick()
  if (!mapContainer.value) return

  const A = getA()
  mapInstance = new A.Map(mapContainer.value, {
    zoom: 16,
    center: [props.initialLng, props.initialLat],
    resizeEnable: true,
    viewMode: '2D'
  })

  mapInstance.on('moveend', () => {
    const center = mapInstance.getCenter()
    updateAddress(center.lng, center.lat)
  })
}

function destroyMap() {
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
  }
}

let regeoSeq = 0
async function updateAddress(lng: number, lat: number) {
  const seq = ++regeoSeq
  try {
    const result = await regeo(lng, lat)
    if (seq === regeoSeq) {
      addressText.value = result.address
    }
  } catch {
    if (seq === regeoSeq) {
      addressText.value = `${lng.toFixed(6)}, ${lat.toFixed(6)}`
    }
  }
}

function onConfirm() {
  if (!mapInstance) return
  const center = mapInstance.getCenter()
  emit('confirm', {
    address: addressText.value,
    longitude: center.lng,
    latitude: center.lat
  })
  emit('update:modelValue', false)
}

function onCancel() {
  emit('update:modelValue', false)
}

async function onSearch() {
  const keyword = searchText.value.trim()
  if (!keyword || !mapInstance) return
  try {
    await loadAmap()
    const A = getA()
    // Load AutoComplete plugin dynamically — JS API 2.0 requires this
    await new Promise<void>((resolve, reject) => {
      A.plugin(['AMap.AutoComplete'], () => resolve())
    })
    const auto = new A.AutoComplete({ citylimit: false })
    auto.search(keyword, (status: string, result: any) => {
      if (status === 'complete' && result.tips && result.tips.length > 0) {
        const tip = result.tips[0]
        if (tip.location) {
          mapInstance.setCenter([tip.location.getLng(), tip.location.getLat()])
          addressText.value = tip.name
        }
      }
    })
  } catch (e) {
    console.error(e)
    showToast('搜索失败')
  }
}

onBeforeUnmount(() => {
  destroyMap()
})
</script>

<template>
  <van-popup
    v-model:show="pickerVisible"
    position="top"
    :style="{ width: '100%', height: '100%' }"
    teleport="body"
    @closed="destroyMap"
  >
    <div class="picker-wrap">
      <div class="picker-header">
        <span class="picker-btn cancel" @click="onCancel">取消</span>
        <span class="picker-title">选择位置</span>
        <span class="picker-btn confirm" @click="onConfirm">确认</span>
      </div>

      <div class="picker-search">
        <van-search
          v-model="searchText"
          placeholder="搜索地址"
          shape="round"
          @search="onSearch"
        />
      </div>

      <div class="picker-map-wrap">
        <div ref="mapContainer" class="picker-map"></div>
        <div class="crosshair">
          <van-icon name="location-o" size="36" color="#ee0a24" />
        </div>
      </div>

      <div class="picker-address-bar">
        <van-icon name="location-o" size="16" color="#409EFF" />
        <span class="address-text">{{ addressText }}</span>
      </div>
    </div>
  </van-popup>
</template>

<style scoped>
.picker-wrap {
  display: flex; flex-direction: column; height: 100%; background: #fff;
}
.picker-header {
  display: flex; align-items: center; justify-content: space-between;
  height: 44px; padding: 0 16px; border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}
.picker-btn { font-size: 15px; cursor: pointer; }
.picker-btn.cancel { color: #666; }
.picker-btn.confirm { color: #409EFF; font-weight: 600; }
.picker-title { font-size: 16px; font-weight: 600; }

.picker-search { flex-shrink: 0; }

.picker-map-wrap {
  flex: 1; position: relative; overflow: hidden; min-height: 0;
}
.picker-map { width: 100%; height: 100%; }

.crosshair {
  position: absolute; top: 50%; left: 50%;
  transform: translate(-50%, -100%);
  pointer-events: none; z-index: 10;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,.3));
}

.picker-address-bar {
  display: flex; align-items: center; gap: 8px; padding: 10px 16px;
  background: #f7f8fa; border-top: 1px solid #eee; flex-shrink: 0; min-height: 44px;
}
.address-text {
  font-size: 13px; color: #333; flex: 1;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
</style>
