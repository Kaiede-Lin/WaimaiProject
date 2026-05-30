<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const order = ref<any>({})
const dispute = ref<any>(null)
const loading = ref(false)
const handling = ref(false)
const rejectRemark = ref('')
const showRejectDialog = ref(false)

const statusMap: any = {
  PENDING_PAYMENT: '待支付', PAID: '待接单', PREPARING: '备餐中',
  ACCEPTED: '已接单', DELIVERING: '配送中', COMPLETED: '已完成',
  CANCELLED: '已取消', REFUNDING: '退款中', REFUNDED: '已退款'
}

const refundTypeMap: any = {
  WRONG_ITEM: '送错商品', MISSING_ITEM: '漏送商品',
  QUALITY_ISSUE: '质量问题', NOT_DELIVERED: '未送达', OTHER: '其他'
}

async function fetch() {
  loading.value = true
  try {
    const res: any = await request.get(`/merchant/order/${route.params.id}/detail`)
    order.value = res.data || {}
  } catch {}
  loading.value = false
}

async function fetchDispute() {
  try {
    const res: any = await request.get('/merchant/dispute/list')
    const list = res.data || []
    dispute.value = list.find((d: any) => d.orderId === order.value.id) || null
  } catch {}
}

async function acceptOrder() {
  try {
    await request.post(`/merchant/order/${order.value.id}/accept`)
    showToast('已接单')
    fetch()
  } catch {}
}

async function markReady() {
  await showConfirmDialog({ title: '确认出餐完成？' })
  try {
    await request.post(`/merchant/order/${order.value.id}/complete`)
    showToast('已出餐，等待骑手取餐')
    fetch()
  } catch {}
}

async function approveRefund() {
  if (!dispute.value) return
  await showConfirmDialog({ title: '确认同意退款？' })
  handling.value = true
  try {
    await request.put(`/merchant/dispute/${dispute.value.id}/handle`, {
      approved: true,
      remark: ''
    })
    showToast('已同意退款')
    fetch()
    fetchDispute()
  } catch {}
  handling.value = false
}

function openRejectDialog() {
  rejectRemark.value = ''
  showRejectDialog.value = true
}

async function rejectRefund() {
  if (!dispute.value) return
  handling.value = true
  try {
    await request.put(`/merchant/dispute/${dispute.value.id}/handle`, {
      approved: false,
      remark: rejectRemark.value
    })
    showToast('已拒绝退款')
    showRejectDialog.value = false
    fetch()
    fetchDispute()
  } catch {}
  handling.value = false
}

async function cancelOrder() {
  await showConfirmDialog({ title: '确认拒绝该订单？' })
  try {
    await request.post(`/merchant/order/${order.value.id}/cancel`)
    showToast('已拒绝订单')
    fetch()
  } catch {}
}

const refundStatusMap: any = {
  REQUESTED: '待商户处理', APPROVED: '已同意退款', REJECTED: '已拒绝退款'
}

onMounted(async () => {
  await fetch()
  await fetchDispute()
})
</script>

<template>
  <div class="detail-page">
    <van-nav-bar title="订单详情" left-arrow @click-left="router.back()" fixed placeholder />

    <van-loading v-if="loading" class="loading-center" />

    <template v-if="order.id">
      <div class="status-bar">
        <van-tag :type="order.status === 'COMPLETED' ? 'success' : order.status === 'CANCELLED' || order.status === 'REFUNDED' ? 'danger' : order.status === 'REFUNDING' ? 'warning' : 'primary'" size="large">
          {{ statusMap[order.status] || order.status }}
        </van-tag>
      </div>

      <!-- Dispute / Refund Card -->
      <div v-if="dispute" class="dispute-section">
        <van-cell-group inset title="退款申请">
          <van-cell title="退款类型" :value="refundTypeMap[dispute.type] || dispute.type" />
          <van-cell title="退款状态">
            <template #value>
              <van-tag :type="dispute.refundStatus === 'APPROVED' ? 'success' : dispute.refundStatus === 'REJECTED' ? 'danger' : 'warning'" size="small">
                {{ refundStatusMap[dispute.refundStatus] || dispute.refundStatus }}
              </van-tag>
            </template>
          </van-cell>
          <van-cell title="客户原因" :value="dispute.description" />
        </van-cell-group>

        <div v-if="dispute.refundStatus === 'REQUESTED'" class="refund-actions">
          <van-button type="success" round :loading="handling" @click="approveRefund">同意退款</van-button>
          <van-button type="danger" round plain :loading="handling" @click="openRejectDialog">拒绝退款</van-button>
        </div>
      </div>

      <van-cell-group inset title="订单信息">
        <van-cell title="订单编号" :value="order.orderNo" />
        <van-cell title="收货地址" :value="order.address" />
        <van-cell v-if="order.remark" title="备注" :value="order.remark" />
      </van-cell-group>

      <van-cell-group inset title="费用">
        <van-cell title="商品总额" :value="'¥' + ((order.totalAmount || 0)).toFixed(2)" />
        <van-cell title="配送费" :value="'¥' + ((order.deliveryFee || 0)).toFixed(2)" />
        <van-cell v-if="order.discountAmount" title="优惠" :value="'-¥' + ((order.discountAmount || 0)).toFixed(2)" />
        <van-cell title="实付">
          <template #value>
            <span class="pay-amount">¥{{ (order.payAmount || 0).toFixed(2) }}</span>
          </template>
        </van-cell>
      </van-cell-group>

      <van-cell-group v-if="order.payTime" inset title="时间">
        <van-cell title="下单时间" :value="order.createTime?.substring(0, 16)" />
        <van-cell title="支付时间" :value="order.payTime?.substring(0, 16)" />
      </van-cell-group>

      <div class="actions">
        <van-button v-if="order.status === 'PAID'" type="primary" round @click="acceptOrder">接单</van-button>
        <van-button v-if="order.status === 'PAID'" type="danger" round plain @click="cancelOrder">拒绝</van-button>
        <van-button v-if="order.status === 'PREPARING'" type="success" round @click="markReady">出餐完成</van-button>
      </div>
    </template>

    <!-- Reject Refund Dialog -->
    <van-dialog v-model:show="showRejectDialog" title="拒绝退款" show-cancel-button @confirm="rejectRefund" :confirm-loading="handling">
      <div style="padding: 16px">
        <van-field v-model="rejectRemark" label="拒绝原因" placeholder="请填写拒绝退款的原因" type="textarea" rows="2" />
      </div>
    </van-dialog>
  </div>
</template>

<style scoped>
.detail-page { padding-bottom: 80px; }
.loading-center { display: flex; justify-content: center; padding: 60px 0; }
.status-bar { padding: 16px; text-align: center; background: #fff; margin-bottom: 8px; }
.pay-amount { color: #ee0a24; font-weight: 700; font-size: 16px; }

.dispute-section { margin-top: 8px; }
.refund-actions { display: flex; gap: 12px; justify-content: center; padding: 16px; }

.actions { display: flex; gap: 10px; justify-content: center; padding: 16px; }
</style>
