<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const disputes = ref<any[]>([])
const loading = ref(false)

const refundTypeMap: any = {
  WRONG_ITEM: '送错商品', MISSING_ITEM: '漏送商品',
  QUALITY_ISSUE: '质量问题', NOT_DELIVERED: '未送达', OTHER: '其他'
}

const statusMap: any = {
  PENDING: '处理中', INVESTIGATING: '平台调查中', RESOLVED: '已解决', REJECTED: '已驳回'
}

const refundStatusMap: any = {
  REQUESTED: '待商户处理', APPROVED: '已同意退款', REJECTED: '已拒绝退款'
}

async function fetch() {
  loading.value = true
  try {
    const res: any = await request.get('/dispute/my')
    disputes.value = res.data || []
  } catch {}
  loading.value = false
}

function goOrder(orderId: number) {
  router.push(`/order/${orderId}`)
}

onMounted(fetch)
</script>

<template>
  <div class="disputes-page">
    <van-nav-bar title="我的纠纷" left-arrow @click-left="router.back()" fixed placeholder />

    <van-pull-refresh v-model="loading" @refresh="fetch">
      <div v-if="disputes.length === 0 && !loading" class="empty-wrap">
        <van-empty description="暂无纠纷记录" />
      </div>
      <div v-for="d in disputes" :key="d.id" class="dispute-card" @click="goOrder(d.orderId)">
        <div class="dispute-header">
          <span class="dispute-type">{{ refundTypeMap[d.type] || d.type }}</span>
          <van-tag v-if="d.refundStatus" :type="d.refundStatus === 'APPROVED' ? 'success' : d.refundStatus === 'REJECTED' ? 'danger' : 'warning'" size="small">
            {{ refundStatusMap[d.refundStatus] || d.refundStatus }}
          </van-tag>
          <van-tag v-else :type="d.status === 'RESOLVED' ? 'success' : d.status === 'REJECTED' ? 'danger' : 'primary'" size="small">
            {{ statusMap[d.status] || d.status }}
          </van-tag>
        </div>
        <div class="dispute-desc">{{ d.description }}</div>
        <div v-if="d.merchantRemark" class="dispute-remark">商户备注: {{ d.merchantRemark }}</div>
        <div class="dispute-time">{{ d.createTime?.substring(0, 16) }}</div>
      </div>
    </van-pull-refresh>
  </div>
</template>

<style scoped>
.disputes-page { padding-bottom: 20px; }
.empty-wrap { padding-top: 60px; }
.dispute-card {
  margin: 8px; padding: 14px; background: #fff; border-radius: 8px; cursor: pointer;
}
.dispute-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.dispute-type { font-size: 14px; font-weight: 600; color: #333; }
.dispute-desc { font-size: 13px; color: #666; margin-bottom: 4px; }
.dispute-remark { font-size: 12px; color: #409EFF; margin-bottom: 4px; }
.dispute-time { font-size: 11px; color: #bbb; }
</style>
