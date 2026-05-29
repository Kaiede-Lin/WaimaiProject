<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const orderId = ref('')
const progress = ref<any>(null)
const subTasks = ref<any[]>([])
const loading = ref(false)
const splitVisible = ref(false)
const splitForm = ref({ orderId: 0, subTasks: [{ address: '', itemsJson: '[]' }] })

async function queryProgress() {
  if (!orderId.value) return
  loading.value = true
  try {
    const res: any = await request.get(`/delivery/batch/progress/${orderId.value}`)
    progress.value = res.data
    if (res.data?.hasBatch) {
      const subRes: any = await request.get(`/delivery/batch/${res.data.batch.id}/subtasks`)
      subTasks.value = subRes.data || []
    }
  } catch { progress.value = null }
  loading.value = false
}

function openSplit() {
  splitForm.value = { orderId: Number(orderId.value), subTasks: [{ address: '', itemsJson: '[]' }] }
  splitVisible.value = true
}
function addSubTask() { splitForm.value.subTasks.push({ address: '', itemsJson: '[]' }) }
function removeSubTask(i: number) { splitForm.value.subTasks.splice(i, 1) }

async function doSplit() {
  await request.post('/delivery/batch/split', splitForm.value)
  ElMessage.success('订单已拆分')
  splitVisible.value = false
  queryProgress()
}

const statusTag: any = { PENDING: 'info', ASSIGNED: 'warning', PICKED_UP: '', DELIVERING: 'primary', COMPLETED: 'success' }
const statusLabel: any = { PENDING: '待分配', ASSIGNED: '已分配', PICKED_UP: '已取餐', DELIVERING: '配送中', COMPLETED: '已完成' }
</script>

<template>
  <div class="batch-page">
    <div class="page-header">
      <h2>多骑手协同配送</h2>
      <div class="search-bar">
        <el-input v-model="orderId" placeholder="输入订单ID" style="width:200px" />
        <el-button type="primary" @click="queryProgress">查询</el-button>
        <el-button v-if="progress && !progress.hasBatch" type="success" @click="openSplit">拆分配送</el-button>
      </div>
    </div>

    <div v-if="progress?.hasBatch" class="batch-info">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="批次号">{{ progress.batch.batchNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ progress.batch.status }}</el-descriptions-item>
        <el-descriptions-item label="进度">{{ progress.progress }}</el-descriptions-item>
        <el-descriptions-item label="子任务">{{ progress.batch.completedSubCount }} / {{ progress.batch.totalSubCount }}</el-descriptions-item>
      </el-descriptions>

      <el-table :data="subTasks" style="margin-top:16px" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="subAddress" label="地址" />
        <el-table-column prop="riderId" label="骑手ID" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTag[row.status]">{{ statusLabel[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="estimatedMinutes" label="预计(分钟)" width="90" />
      </el-table>
    </div>

    <el-dialog v-model="splitVisible" title="拆分订单为子任务" width="560px">
      <div v-for="(st, i) in splitForm.subTasks" :key="i" class="subtask-row">
        <el-input v-model="st.address" placeholder="子配送地址" style="flex:1" />
        <el-input v-model="st.itemsJson" placeholder="菜品JSON" style="flex:1; margin-left:8px" />
        <el-button type="danger" :icon="'Delete'" circle size="small" @click="removeSubTask(i)" style="margin-left:4px" />
      </div>
      <el-button @click="addSubTask" style="margin-top:10px">+ 添加子任务</el-button>
      <template #footer>
        <el-button @click="splitVisible = false">取消</el-button>
        <el-button type="primary" @click="doSplit">确认拆分</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.batch-page { max-width: 1200px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 18px; }
.search-bar { display: flex; gap: 8px; }
.batch-info { margin-top: 12px; }
.subtask-row { display: flex; align-items: center; margin-bottom: 8px; }
</style>
