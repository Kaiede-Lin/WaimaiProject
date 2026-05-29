<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import request from '@/utils/request'

const router = useRouter()
const merchants = ref<any[]>([])
const allMerchants = ref<any[]>([])
const loading = ref(false)
const searchText = ref('')
const sortBy = ref<'default' | 'score' | 'sales' | 'distance'>('default')
const currentLoc = ref({ lng: 116.397, lat: 39.908 })

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
  } catch { /* ignore */ }
  loading.value = false
}

function goMerchant(id: number) {
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

onMounted(fetch)
</script>

<template>
  <div class="home-page">
    <van-nav-bar title="美食外卖" fixed placeholder>
      <template #right>
        <van-icon name="user-o" size="20" @click="router.push('/profile')" />
      </template>
    </van-nav-bar>

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
                {{ m.avgDeliveryTime || 30 }}分钟
              </span>
              <span class="delivery-fee">配送费 ¥{{ m.deliveryFee || 0 }}</span>
              <span class="distance">{{ m.distanceKm?.toFixed(1) || '0' }}km</span>
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
.search-bar { background: #fff; position: sticky; top: 46px; z-index: 10; }
.filter-bar {
  display: flex; gap: 0; padding: 0 16px 8px; font-size: 13px;
}
.filter-item {
  flex: 1; text-align: center; color: #666; cursor: pointer; padding: 4px 0;
}
.filter-item.active { color: #409EFF; font-weight: 600; }
.merchant-list { padding: 8px; }
.merchant-card {
  display: flex; gap: 12px; padding: 12px; background: #fff; border-radius: 8px; margin-bottom: 8px; cursor: pointer;
}
.merchant-info { flex: 1; display: flex; flex-direction: column; gap: 4px; overflow: hidden; }
.merchant-name { font-size: 15px; font-weight: 600; display: flex; align-items: center; gap: 6px; }
.merchant-meta { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #999; }
.score-text { color: #ffc800; font-weight: 600; }
.divider { color: #eee; }
.merchant-footer { display: flex; align-items: center; gap: 12px; font-size: 11px; color: #999; }
.delivery-fee { color: #666; }
.distance { color: #409EFF; font-weight: 500; margin-left: auto; }
.merchant-desc { font-size: 11px; color: #aaa; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
