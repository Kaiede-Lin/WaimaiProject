<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { showToast, showConfirmDialog } from 'vant'
import request from '@/utils/request'

const orders = ref<any[]>([])
const loading = ref(false)
const tab = ref(0)

const statusMap: any = {
  PAID: '待接单', PREPARING: '备餐中', DELIVERING: '配送中',
  COMPLETED: '已完成', CANCELLED: '已取消'
}
const statusColor: any = {
  PAID: 'danger', PREPARING: 'warning', DELIVERING: 'primary',
  COMPLETED: 'success', CANCELLED: 'default'
}

async function fetch() {
  loading.value = true
  try {
    const res: any = await request.get('/merchant/order/list', { params: { page: 1, size: 100 } })
    orders.value = res.data || []
  } catch {}
  loading.value = false
}

const filtered = computed(() => {
  if (tab.value === 0) return orders.value.filter((o: any) => o.status === 'PAID')
  if (tab.value === 1) return orders.value.filter((o: any) => o.status === 'PREPARING' || o.status === 'DELIVERING')
  return orders.value.filter((o: any) => o.status === 'COMPLETED' || o.status === 'CANCELLED')
})

const pendingCount = computed(() => orders.value.filter((o: any) => o.status === 'PAID').length)
const activeCount = computed(() => orders.value.filter((o: any) => o.status === 'PREPARING' || o.status === 'DELIVERING').length)

async function acceptOrder(id: number) {
  await request.post(`/merchant/order/${id}/accept`)
  showToast('已接单')
  fetch()
}

async function rejectOrder(id: number) {
  await showConfirmDialog({ title: '确认拒绝此订单？', message: '拒绝后订单将被取消' })
  await request.post(`/merchant/order/${id}/cancel`)
  showToast('已拒绝')
  fetch()
}

async function completeOrder(id: number) {
  await request.post(`/merchant/order/${id}/complete`)
  showToast('已出餐，等待骑手取餐')
  fetch()
}

onMounted(fetch)
</script>

<template>
  <div class="orders-page">
    <van-nav-bar title="订单管理" fixed placeholder />

    <van-tabs v-model:active="tab" sticky>
      <van-tab :title="'新订单 (' + pendingCount + ')'">
        <template #title>
          <van-badge :content="pendingCount" :max="99">
            <span>新订单</span>
          </van-badge>
        </template>
      </van-tab>
      <van-tab :title="'进行中 (' + activeCount + ')'" />
      <van-tab title="已完成" />
    </van-tabs>

    <van-pull-refresh v-model="loading" @refresh="fetch">
      <div v-for="o in filtered" :key="o.id" class="order-card">
        <div class="order-head">
          <span class="order-no">#{{ o.orderNo?.substring(o.orderNo.length - 8) }}</span>
          <van-tag :type="statusColor[o.status]" size="small">{{ statusMap[o.status] || o.status }}</van-tag>
        </div>
        <div class="order-details" v-if="o.details">
          <span v-for="d in o.details?.slice(0, 4)" :key="d.dishId" class="dish-tag">{{ d.dishName }} x{{ d.quantity }}</span>
        </div>
        <div class="order-row">
          <span class="order-addr"><van-icon name="location-o" size="12" /> {{ o.address }}</span>
          <span class="order-price">¥{{ (o.payAmount || 0).toFixed(2) }}</span>
        </div>
        <div class="order-time" v-if="o.createTime">{{ o.createTime?.substring(0, 16) }}</div>
        <div class="order-actions">
          <template v-if="o.status === 'PAID'">
            <van-button size="small" type="danger" plain @click="rejectOrder(o.id)">拒绝</van-button>
            <van-button size="small" type="primary" @click="acceptOrder(o.id)">接单</van-button>
          </template>
          <van-button v-if="o.status === 'PREPARING'" size="small" type="success" @click="completeOrder(o.id)">出餐完成</van-button>
          <van-tag v-if="o.status === 'DELIVERING'" type="primary" size="small">骑手配送中</van-tag>
        </div>
      </div>
      <van-empty v-if="!loading && filtered.length === 0" description="暂无订单" />
    </van-pull-refresh>

    <van-tabbar route>
      <van-tabbar-item to="/" icon="home-o">首页</van-tabbar-item>
      <van-tabbar-item to="/orders" icon="orders-o">订单</van-tabbar-item>
      <van-tabbar-item to="/menu" icon="wap-home-o">菜单</van-tabbar-item>
      <van-tabbar-item to="/reports" icon="chart-trending-o">报表</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<style scoped>
.orders-page { padding-bottom: 50px; }
.order-card { background: #fff; margin: 8px; padding: 12px; border-radius: 8px; }
.order-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.order-no { font-weight: 600; font-size: 13px; color: #666; }
.order-details { display: flex; flex-wrap: wrap; gap: 4px; margin: 6px 0; }
.dish-tag { font-size: 11px; background: #f5f5f5; padding: 2px 6px; border-radius: 4px; color: #666; }
.order-row { display: flex; justify-content: space-between; align-items: center; margin: 4px 0; }
.order-addr { font-size: 12px; color: #999; display: flex; align-items: center; gap: 4px; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.order-price { font-size: 16px; font-weight: 700; color: #ee0a24; }
.order-time { font-size: 11px; color: #bbb; margin-top: 4px; }
.order-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 8px; padding-top: 8px; border-top: 1px solid #f5f5f5; }
</style>
