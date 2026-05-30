<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const reviews = ref<any[]>([])
const loading = ref(false)

async function fetch() {
  loading.value = true
  try {
    const res: any = await request.get('/review/my', { params: { page: 1, size: 50 } })
    const data = res.data || {}
    reviews.value = data.records || []
  } catch {}
  loading.value = false
}

function goOrder(orderId: number) {
  router.push(`/order/${orderId}`)
}

onMounted(fetch)
</script>

<template>
  <div class="reviews-page">
    <van-nav-bar title="我的评价" left-arrow @click-left="router.back()" fixed placeholder />

    <van-pull-refresh v-model="loading" @refresh="fetch">
      <div v-if="reviews.length === 0 && !loading" class="empty-wrap">
        <van-empty description="暂无评价" />
      </div>
      <div v-for="r in reviews" :key="r.id" class="review-card" @click="goOrder(r.orderId)">
        <div class="review-header">
          <van-rate :model-value="r.rating" readonly size="14" :color="'#ffc800'" />
          <span class="review-type">
            <van-tag :type="r.type === 'MERCHANT' ? 'primary' : 'success'" size="mini">{{ r.type === 'MERCHANT' ? '商家' : '骑手' }}</van-tag>
          </span>
        </div>
        <div v-if="r.content" class="review-text">{{ r.content }}</div>
        <div class="review-time">{{ r.createTime?.substring(0, 16) }}</div>
      </div>
    </van-pull-refresh>
  </div>
</template>

<style scoped>
.reviews-page { padding-bottom: 20px; }
.empty-wrap { padding-top: 60px; }
.review-card {
  margin: 8px; padding: 14px; background: #fff; border-radius: 8px; cursor: pointer;
}
.review-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.review-text { font-size: 13px; color: #666; margin-bottom: 4px; }
.review-time { font-size: 11px; color: #bbb; }
</style>
