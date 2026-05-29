<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import request from '@/utils/request'

const lineChartRef = ref<HTMLElement>()
const pieChartRef = ref<HTMLElement>()
const barChartRef = ref<HTMLElement>()
let lineChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null
let barChart: echarts.ECharts | null = null
let timer: ReturnType<typeof setInterval> | null = null

const stats = ref({
  todayOrders: 0,
  todayRevenue: 0,
  totalMerchants: 0,
  totalUsers: 0,
  onlineRiders: 0
})
const last7Days = ref<{ date: string; count: number }[]>([])

const statCards = [
  { label: '今日订单', key: 'todayOrders', suffix: '单', color: '#409EFF', bg: '#ECF5FF', icon: 'Document' },
  { label: '今日营收', key: 'todayRevenue', prefix: '¥', isMoney: true, color: '#67C23A', bg: '#F0F9EB', icon: 'Money' },
  { label: '在线骑手', key: 'onlineRiders', suffix: '人', color: '#E6A23C', bg: '#FDF6EC', icon: 'UserFilled' },
  { label: '商家总数', key: 'totalMerchants', suffix: '家', color: '#F56C6C', bg: '#FEF0F0', icon: 'Shop' },
]

function formatValue(card: any): string {
  const v = (stats.value as any)[card.key] ?? 0
  if (card.isMoney) return '¥' + Number(v).toFixed(2)
  return String(v) + (card.suffix || '')
}

async function fetchData() {
  try {
    const res: any = await request.get('/admin/dashboard')
    if (res.data) {
      stats.value = {
        todayOrders: res.data.todayOrders || 0,
        todayRevenue: res.data.todayRevenue || 0,
        totalMerchants: res.data.totalMerchants || 0,
        totalUsers: res.data.totalUsers || 0,
        onlineRiders: res.data.onlineRiders || 0
      }
      last7Days.value = res.data.last7Days || []
    }
  } catch { /* use defaults */ }
  initCharts()
}

function initCharts() {
  if (lineChartRef.value) {
    if (!lineChart) lineChart = echarts.init(lineChartRef.value)
    lineChart.setOption({
      tooltip: { trigger: 'axis', backgroundColor: '#fff', borderColor: '#eee', textStyle: { color: '#333' } },
      grid: { top: 16, left: 0, right: 8, bottom: 0, containLabel: true },
      xAxis: {
        type: 'category',
        data: last7Days.value.map(d => d.date),
        axisLine: { lineStyle: { color: '#e8e8e8' } },
        axisTick: { show: false },
        axisLabel: { color: '#999' }
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: '#f5f5f5' } },
        axisLabel: { color: '#999' }
      },
      series: [{
        name: '订单量',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        data: last7Days.value.map(d => d.count),
        lineStyle: { color: '#409EFF', width: 3 },
        itemStyle: {
          color: '#409EFF',
          borderColor: '#fff',
          borderWidth: 2
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64,158,255,0.2)' },
            { offset: 1, color: 'rgba(64,158,255,0.02)' }
          ])
        }
      }]
    })
  }

  if (pieChartRef.value) {
    if (!pieChart) pieChart = echarts.init(pieChartRef.value)
    pieChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      series: [{
        type: 'pie',
        radius: ['52%', '80%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 3 },
        label: { show: false },
        emphasis: {
          label: { show: true, fontSize: 16, fontWeight: 'bold' },
          scaleSize: 8
        },
        data: [
          { value: 420, name: '中餐', itemStyle: { color: '#409EFF' } },
          { value: 310, name: '西餐', itemStyle: { color: '#67C23A' } },
          { value: 235, name: '饮品', itemStyle: { color: '#E6A23C' } },
          { value: 180, name: '小吃', itemStyle: { color: '#F56C6C' } },
          { value: 141, name: '其他', itemStyle: { color: '#909399' } }
        ]
      }]
    })
  }

  if (barChartRef.value) {
    if (!barChart) barChart = echarts.init(barChartRef.value)
    barChart.setOption({
      tooltip: { trigger: 'axis', backgroundColor: '#fff', borderColor: '#eee', textStyle: { color: '#333' } },
      grid: { top: 16, left: 0, right: 8, bottom: 0, containLabel: true },
      xAxis: {
        type: 'category',
        data: ['待支付', '已支付', '备餐中', '配送中', '已完成', '已取消'],
        axisLine: { lineStyle: { color: '#e8e8e8' } },
        axisTick: { show: false },
        axisLabel: { color: '#999' }
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: '#f5f5f5' } },
        axisLabel: { color: '#999' }
      },
      series: [{
        type: 'bar',
        barWidth: 24,
        data: [
          { value: 28, itemStyle: { color: '#E6A23C', borderRadius: [6,6,0,0] } },
          { value: 45, itemStyle: { color: '#409EFF', borderRadius: [6,6,0,0] } },
          { value: 32, itemStyle: { color: '#909399', borderRadius: [6,6,0,0] } },
          { value: 18, itemStyle: { color: '#67C23A', borderRadius: [6,6,0,0] } },
          { value: 156, itemStyle: { color: '#67C23A', borderRadius: [6,6,0,0] } },
          { value: 12, itemStyle: { color: '#F56C6C', borderRadius: [6,6,0,0] } }
        ]
      }]
    })
  }
}

onMounted(() => {
  fetchData()
  timer = setInterval(fetchData, 30000)
})

onUnmounted(() => {
  lineChart?.dispose()
  pieChart?.dispose()
  barChart?.dispose()
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="dashboard">
    <!-- Stat Cards -->
    <div class="stat-grid">
      <div v-for="card in statCards" :key="card.label" class="stat-card">
        <div class="stat-icon-wrap" :style="{ background: card.bg }">
          <el-icon :size="22" :color="card.color"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-body">
          <span class="stat-label">{{ card.label }}</span>
          <span class="stat-value" :style="{ color: card.color }">{{ formatValue(card) }}</span>
        </div>
      </div>
    </div>

    <!-- Charts Row -->
    <div class="charts-row">
      <div class="chart-card chart-card--large">
        <div class="chart-header">
          <span class="chart-title">近7日订单趋势</span>
          <span class="chart-subtitle">更新于 {{ new Date().toLocaleTimeString() }}</span>
        </div>
        <div ref="lineChartRef" class="chart-body"></div>
      </div>
      <div class="chart-card">
        <div class="chart-header">
          <span class="chart-title">品类销量占比</span>
        </div>
        <div ref="pieChartRef" class="chart-body"></div>
      </div>
    </div>

    <!-- Bottom Row -->
    <div class="charts-row">
      <div class="chart-card chart-card--full">
        <div class="chart-header">
          <span class="chart-title">各状态订单分布</span>
        </div>
        <div ref="barChartRef" class="chart-body" style="height:300px"></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  max-width: 1400px;
}

/* Stat Cards */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}
.stat-card {
  background: #fff;
  border-radius: 14px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  transition: all 0.25s;
  cursor: default;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0,0,0,0.08);
}
.stat-icon-wrap {
  width: 50px;
  height: 50px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.stat-label {
  font-size: 13px;
  color: #999;
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
}

/* Charts */
.charts-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}
.chart-card {
  background: #fff;
  border-radius: 14px;
  padding: 20px;
  flex: 1;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  min-width: 0;
}
.chart-card--large {
  flex: 1.4;
}
.chart-card--full {
  flex: none;
  width: 100%;
}
.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}
.chart-subtitle {
  font-size: 11px;
  color: #ccc;
}
.chart-body {
  height: 340px;
}

@media (max-width: 1200px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .charts-row {
    flex-direction: column;
  }
}
</style>
