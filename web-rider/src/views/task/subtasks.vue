<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { showSuccessToast } from 'vant'
import request from '@/utils/request'

const riderId = ref(0)
const subTasks = ref<any[]>([])
const loading = ref(false)

async function fetch() {
  try {
    const info: any = await request.get('/rider/info')
    riderId.value = info?.data?.id || 0
    const res: any = await request.get('/delivery/subtask/my', { params: { riderId: riderId.value } })
    subTasks.value = res.data || []
  } catch {}
}

async function completeSubTask(id: number) {
  await request.post(`/delivery/subtask/${id}/complete`, { riderId: riderId.value })
  showSuccessToast('子任务完成')
  fetch()
}

onMounted(fetch)
</script>

<template>
  <div class="subtasks-page">
    <van-nav-bar title="协同配送任务" fixed placeholder />
    <van-pull-refresh v-model="loading" @refresh="fetch">
      <div v-for="t in subTasks" :key="t.id" class="task-card">
        <div class="task-header">
          <van-tag :type="t.status === 'COMPLETED' ? 'success' : 'primary'" size="small">
            {{ t.status === 'ASSIGNED' ? '已分配' : t.status === 'DELIVERING' ? '配送中' : t.status === 'COMPLETED' ? '已完成' : t.status }}
          </van-tag>
          <span class="batch-id">批次 #{{ t.batchId }}</span>
        </div>
        <div class="task-addr">送: {{ t.subAddress }}</div>
        <div class="task-items" v-if="t.itemsJson">{{ t.itemsJson }}</div>
        <van-button v-if="t.status !== 'COMPLETED' && t.status !== 'PENDING'" type="success" size="small" round @click="completeSubTask(t.id)">完成配送</van-button>
      </div>
      <van-empty v-if="!loading && subTasks.length === 0" description="暂无协同配送任务" />
    </van-pull-refresh>
  </div>
</template>

<style scoped>
.subtasks-page { padding-bottom: 20px; }
.task-card { background: #fff; margin: 8px; padding: 12px; border-radius: 8px; }
.task-header { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.task-addr { font-size: 14px; } .task-items { font-size: 11px; color: #999; margin: 4px 0; }
</style>
