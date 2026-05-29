<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const report = ref<any>({ totalRevenue: 0, orderCount: 0, avgOrderValue: 0, topDishes: [], revenueDetails: [] })
const date = ref(new Date().toISOString().substring(0, 10))
const type = ref('daily')
const loading = ref(false)

const typeLabel: any = { daily: '日报', weekly: '周报', monthly: '月报' }

async function fetch() {
  loading.value = true
  try {
    let url = `/merchant/report/${type.value}`
    const params: any = {}
    if (type.value === 'daily') params.date = date.value
    else if (type.value === 'weekly') params.startDate = date.value
    else params.month = date.value.substring(0, 7)

    const res: any = await request.get(url, { params })
    report.value = res.data || {}
  } catch {}
  loading.value = false
}

function changeType(t: string) {
  type.value = t
  if (t === 'daily') date.value = new Date().toISOString().substring(0, 10)
  else if (t === 'weekly') date.value = new Date().toISOString().substring(0, 10)
  else date.value = new Date().toISOString().substring(0, 7)
  fetch()
}

onMounted(fetch)
</script>

<template>
  <div class="report-page">
    <van-nav-bar title="销售报表" fixed placeholder />

    <div class="type-switch">
      <span v-for="t in ['daily', 'weekly', 'monthly']" :key="t"
            :class="['type-btn', { active: type === t }]" @click="changeType(t)">
        {{ typeLabel[t] }}
      </span>
    </div>

    <div class="date-picker">
      <van-field
        v-model="date"
        :label="type === 'daily' ? '日期' : type === 'weekly' ? '起始日期' : '月份'"
        :type="type === 'monthly' ? 'month' : 'date'"
        @update:model-value="fetch"
      />
    </div>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon" style="background:#fff0f0"><van-icon name="gold-coin-o" color="#ee0a24" size="24"/></div>
        <div class="stat-body">
          <span class="stat-val">¥{{ (report.totalRevenue || 0).toFixed(2) }}</span>
          <span class="stat-label">总营收</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#f0f9ff"><van-icon name="orders-o" color="#409EFF" size="24"/></div>
        <div class="stat-body">
          <span class="stat-val">{{ report.orderCount || 0 }}</span>
          <span class="stat-label">订单数</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#f0fff0"><van-icon name="chart-trending-o" color="#07c160" size="24"/></div>
        <div class="stat-body">
          <span class="stat-val">¥{{ (report.avgOrderValue || 0).toFixed(2) }}</span>
          <span class="stat-label">平均客单</span>
        </div>
      </div>
      <div class="stat-card" v-if="report.completedOrders !== undefined">
        <div class="stat-icon" style="background:#fff8f0"><van-icon name="checked" color="#ff976a" size="24"/></div>
        <div class="stat-body">
          <span class="stat-val">{{ report.completedOrders || 0 }}</span>
          <span class="stat-label">完成率</span>
        </div>
      </div>
    </div>

    <div class="section">
      <h4>热销菜品 TOP{{ (report.topDishes || []).length }}</h4>
      <div v-for="(d, idx) in report.topDishes" :key="d.name" class="top-dish-row">
        <span class="rank" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</span>
        <span class="dish-name">{{ d.name }}</span>
        <div class="dish-bar">
          <div class="bar-fill" :style="{ width: ((d.count / (report.topDishes[0]?.count || 1)) * 100) + '%' }"></div>
        </div>
        <span class="dish-count">{{ d.count }}单</span>
      </div>
      <van-empty v-if="!report.topDishes || report.topDishes.length === 0" description="暂无数据" />
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
.report-page { padding-bottom: 50px; }
.type-switch { display: flex; padding: 8px; gap: 8px; }
.type-btn {
  flex: 1; text-align: center; padding: 8px; border-radius: 20px; background: #f5f5f5;
  font-size: 13px; cursor: pointer; transition: all 0.3s;
}
.type-btn.active { background: #ff6b35; color: #fff; font-weight: 600; }

.stats-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; padding: 8px; }
.stat-card {
  display: flex; align-items: center; gap: 10px; background: #fff; border-radius: 10px; padding: 14px;
}
.stat-icon { width: 40px; height: 40px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.stat-body { display: flex; flex-direction: column; }
.stat-val { font-size: 18px; font-weight: 700; color: #333; }
.stat-label { font-size: 11px; color: #999; }

.section { padding: 8px 16px; }
.section h4 { font-size: 14px; margin-bottom: 10px; }
.top-dish-row { display: flex; align-items: center; gap: 8px; padding: 6px 0; }
.rank {
  width: 20px; height: 20px; border-radius: 4px; text-align: center; line-height: 20px;
  font-size: 11px; font-weight: 700; background: #f0f0f0; color: #999; flex-shrink: 0;
}
.rank-1 { background: #ee0a24; color: #fff; }
.rank-2 { background: #ff976a; color: #fff; }
.rank-3 { background: #ffc800; color: #fff; }
.dish-name { width: 60px; font-size: 12px; flex-shrink: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dish-bar { flex: 1; height: 6px; background: #f0f0f0; border-radius: 3px; overflow: hidden; }
.bar-fill { height: 100%; background: linear-gradient(90deg, #ff6b35, #ff8c5a); border-radius: 3px; min-width: 4px; }
.dish-count { font-size: 12px; color: #999; width: 36px; text-align: right; flex-shrink: 0; }
</style>
