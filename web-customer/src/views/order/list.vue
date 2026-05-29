<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import request from '@/utils/request'

const router = useRouter()
const orders = ref<any[]>([])
const loading = ref(false)

const statusMap: any = {
  PENDING_PAYMENT: '待支付', PAID: '已支付', PREPARING: '备餐中',
  DELIVERING: '配送中', COMPLETED: '已完成', CANCELLED: '已取消'
}

async function fetch() {
  loading.value = true
  try {
    const res: any = await request.get('/order/list', { params: { page: 1, size: 50 } })
    orders.value = res.data?.records || []
  } catch { /* ignore */ }
  loading.value = false
}

function viewDetail(order: any) { router.push(`/order/${order.orderNo}`) }

async function cancelOrder(order: any) {
  await request.post(`/order/${order.id}/cancel`)
  showToast('已取消')
  fetch()
}

async function payOrder(order: any) {
  await request.post(`/order/${order.id}/pay`)
  showToast('支付成功')
  fetch()
}

onMounted(fetch)
</script>

<template>
  <div class="order-list-page">
    <van-nav-bar title="我的订单" fixed placeholder />
    <van-pull-refresh v-model="loading" @refresh="fetch">
      <div v-for="order in orders" :key="order.id" class="order-card" @click="viewDetail(order)">
        <div class="order-header">
          <span class="merchant-name">{{ order.merchantName || '商家' }}</span>
          <van-tag :type="order.status === 'COMPLETED' ? 'success' : order.status === 'CANCELLED' ? 'danger' : 'primary'" size="small">
            {{ statusMap[order.status] || order.status }}
          </van-tag>
        </div>
        <div class="order-body">
          <div v-for="d in order.details?.slice(0, 3)" :key="d.dishId" class="order-item">
            {{ d.dishName }} x{{ d.quantity }}
          </div>
        </div>
        <div class="order-footer">
          <span class="order-total">¥{{ (order.payAmount || 0).toFixed(2) }}</span>
          <span class="order-time">{{ order.createTime?.substring(0, 16) }}</span>
        </div>
        <div class="order-actions" @click.stop>
          <van-button v-if="order.status === 'PENDING_PAYMENT'" size="small" type="danger" @click="cancelOrder(order)">取消</van-button>
          <van-button v-if="order.status === 'PENDING_PAYMENT'" size="small" type="primary" @click="payOrder(order)">支付</van-button>
          <van-button v-if="order.status === 'COMPLETED'" size="small" plain type="primary" @click="viewDetail(order)">评价</van-button>
        </div>
      </div>
      <van-empty v-if="!loading && orders.length === 0" description="暂无订单" />
    </van-pull-refresh>
    <van-tabbar route>
      <van-tabbar-item to="/" icon="home-o">首页</van-tabbar-item>
      <van-tabbar-item to="/order/list" icon="orders-o">订单</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<style scoped>
.order-list-page { padding-bottom: 50px; }
.order-card { background: #fff; margin: 8px; border-radius: 8px; padding: 12px; cursor: pointer; }
.order-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.merchant-name { font-weight: 600; font-size: 14px; }
.order-body { padding: 4px 0; }
.order-item { font-size: 12px; color: #666; padding: 2px 0; }
.order-footer { display: flex; justify-content: space-between; margin-top: 8px; font-size: 12px; }
.order-total { color: #ee0a24; font-weight: 600; font-size: 15px; }
.order-time { color: #999; }
.order-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 8px; }
</style>
