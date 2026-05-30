<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast, showToast } from 'vant'
import request from '@/utils/request'

const router = useRouter()
const tasks = ref<any[]>([])
const exceptions = ref<any[]>([])
const loading = ref(false)
const tab = ref(0)

const activeTasks = computed(() => tasks.value.filter((t: any) => t.status === 'ACCEPTED' || t.status === 'DELIVERING'))
const completedTasks = computed(() => tasks.value.filter((t: any) => t.status === 'COMPLETED'))
const todayTasks = computed(() => tasks.value.filter((t: any) => {
  return t.createTime?.substring(0, 10) === new Date().toISOString().substring(0, 10)
}))

const todayEarnings = computed(() =>
  todayTasks.value.reduce((sum: number, t: any) => sum + (t.deliveryFee || 5), 0)
)

// Exception reporting
const showException = ref(false)
const selectedExceptionOrder = ref<any>(null)
const exceptionType = ref('')
const exceptionDesc = ref('')
const reporting = ref(false)

const exceptionTypeMap: any = {
  CUSTOMER_UNREACHABLE: '顾客无法联系',
  WRONG_ADDRESS: '地址错误',
  GOODS_DAMAGED: '商品损坏',
  OTHER: '其他'
}

function hasException(orderId: number) {
  return exceptions.value.some((e: any) => e.orderId === orderId)
}

function openExceptionReport(order: any) {
  selectedExceptionOrder.value = order
  exceptionType.value = ''
  exceptionDesc.value = ''
  showException.value = true
}

async function submitException() {
  if (!exceptionType.value) { showToast('请选择异常类型'); return }
  if (!exceptionDesc.value) { showToast('请填写异常描述'); return }
  reporting.value = true
  try {
    await request.post('/rider/exception/report', {
      orderId: selectedExceptionOrder.value.id,
      type: exceptionType.value,
      description: exceptionDesc.value
    })
    showSuccessToast('异常已上报')
    showException.value = false
    fetchExceptions()
  } catch {}
  reporting.value = false
}

async function fetch() {
  loading.value = true
  try {
    const res: any = await request.get('/rider/order/list', { params: { page: 1, size: 50 } })
    tasks.value = res.data || []
  } catch {}
  loading.value = false
}

async function fetchExceptions() {
  try {
    const res: any = await request.get('/rider/exception/list')
    exceptions.value = res.data || []
  } catch {}
}

let pickingUp = false

async function pickUpOrder(order: any) {
  if (pickingUp) return
  pickingUp = true
  try {
    await request.post(`/rider/order/${order.orderNo}/pickup`)
    showSuccessToast('已取餐，开始配送')
    fetch()
  } catch {}
  pickingUp = false
}

let completing = false

async function completeOrder(order: any) {
  if (completing) return
  completing = true
  try {
    await request.post(`/rider/order/${order.orderNo}/complete`)
    showSuccessToast('配送完成')
    fetch()
  } catch {}
  completing = false
}

function goTrack(order: any) {
  router.push(`/track/${order.orderNo}`)
}

function goJoint() {
  router.push('/joint')
}

onMounted(() => {
  fetch()
  fetchExceptions()
})
</script>

<template>
  <div class="tasks-page">
    <van-nav-bar title="我的任务" fixed placeholder />

    <div class="stats-bar">
      <div class="stat">
        <span class="num">{{ todayTasks.length }}</span>
        <span class="lbl">今日完成</span>
      </div>
      <div class="stat">
        <span class="num">¥{{ todayEarnings.toFixed(2) }}</span>
        <span class="lbl">今日收入</span>
      </div>
      <div class="stat">
        <span class="num">{{ activeTasks.length }}</span>
        <span class="lbl">进行中</span>
      </div>
    </div>

    <div class="quick-links">
      <van-button size="small" plain type="primary" @click="goJoint">联合配送任务</van-button>
    </div>

    <van-tabs v-model:active="tab">
      <van-tab :title="'进行中 (' + activeTasks.length + ')'">
        <div v-if="activeTasks.length === 0" style="text-align:center;padding:40px;color:#999">暂无进行中的任务</div>
        <div v-for="t in activeTasks" :key="t.id" class="task-card">
          <div class="task-status">
            <van-tag :type="t.status === 'ACCEPTED' ? 'warning' : 'primary'" size="small">
              {{ t.status === 'ACCEPTED' ? '待取餐' : '配送中' }}
            </van-tag>
            <span class="order-no">#{{ t.orderNo?.substring(t.orderNo.length - 10) }}</span>
          </div>
          <div class="task-addr">
            <van-icon name="shop-o" size="14" color="#ff6b35" />
            <span>取餐：{{ t.merchantName || '商家' }}</span>
          </div>
          <div class="task-addr">
            <van-icon name="location-o" size="14" color="#409EFF" />
            {{ t.address }}
          </div>
          <div class="task-meta">
            <span>配送费 ¥{{ (t.deliveryFee || 5).toFixed(2) }}</span>
            <span v-if="t.estimatedMinutes">预计 {{ t.estimatedMinutes }} 分钟</span>
          </div>
          <div class="task-actions">
            <van-button size="small" plain @click="goTrack(t)">导航</van-button>
            <van-button v-if="t.status === 'ACCEPTED'" size="small" type="warning" :loading="pickingUp" @click="pickUpOrder(t)">
              确认取餐
            </van-button>
            <van-button v-if="t.status === 'DELIVERING'" size="small" type="success" :loading="completing" @click="completeOrder(t)">
              确认送达
            </van-button>
            <van-button v-if="!hasException(t.id)" size="small" plain type="danger" @click.stop="openExceptionReport(t)">
              异常上报
            </van-button>
            <van-tag v-else type="warning" size="small">已上报异常</van-tag>
          </div>
        </div>
      </van-tab>
      <van-tab :title="'异常 (' + exceptions.length + ')'">
        <div v-if="exceptions.length === 0" style="text-align:center;padding:40px;color:#999">暂无异常记录</div>
        <div v-for="e in exceptions" :key="e.id" class="task-card">
          <div class="task-status">
            <van-tag :type="e.status === 'RESOLVED' ? 'success' : 'warning'" size="small">
              {{ e.status === 'RESOLVED' ? '已解决' : '已上报' }}
            </van-tag>
            <span>{{ exceptionTypeMap[e.type] || e.type }}</span>
          </div>
          <div class="task-addr">{{ e.description }}</div>
          <div class="task-meta" style="color:#999;font-weight:400">{{ e.createTime?.substring(0, 16) }}</div>
        </div>
      </van-tab>
      <van-tab :title="'已完成 (' + completedTasks.length + ')'">
        <div v-if="completedTasks.length === 0" style="text-align:center;padding:40px;color:#999">暂无完成的订单</div>
        <div v-for="t in completedTasks" :key="t.id" class="task-card done">
          <div class="task-status">
            <van-tag type="success" size="small">已完成</van-tag>
            <span class="order-no">#{{ t.orderNo?.substring(t.orderNo.length - 10) }}</span>
          </div>
          <div class="task-addr">{{ t.address }}</div>
          <div class="task-meta">配送费 ¥{{ (t.deliveryFee || 5).toFixed(2) }}</div>
        </div>
      </van-tab>
    </van-tabs>

    <!-- Exception Report Dialog -->
    <van-dialog v-model:show="showException" title="上报异常" show-cancel-button @confirm="submitException" :confirm-loading="reporting">
      <div style="padding:16px">
        <div class="ex-type-label">异常类型</div>
        <div class="ex-types">
          <span v-for="(label, key) in exceptionTypeMap" :key="key"
                :class="['type-chip', { active: exceptionType === key }]"
                @click="exceptionType = key">{{ label }}</span>
        </div>
        <van-field v-model="exceptionDesc" label="详细描述" placeholder="请描述异常情况" type="textarea" rows="2" />
      </div>
    </van-dialog>

    <van-tabbar route>
      <van-tabbar-item to="/" icon="home-o">大厅</van-tabbar-item>
      <van-tabbar-item to="/tasks" icon="todo-list-o">任务</van-tabbar-item>
      <van-tabbar-item to="/income" icon="gold-coin-o">收入</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<style scoped>
.tasks-page { padding-bottom: 50px; }
.stats-bar { display: flex; gap: 8px; padding: 8px; }
.stat {
  flex: 1; background: #fff; border-radius: 8px; padding: 12px; text-align: center;
}
.num { font-size: 18px; font-weight: 700; color: #333; display: block; }
.lbl { font-size: 11px; color: #999; margin-top: 2px; display: block; }
.quick-links { padding: 0 8px 8px; }

.task-card { background: #fff; margin: 8px; padding: 12px; border-radius: 8px; }
.task-card.done { opacity: 0.7; }
.task-status { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.order-no { font-size: 12px; color: #666; }
.task-addr { font-size: 13px; color: #333; padding: 4px 0; display: flex; align-items: center; gap: 6px; }
.task-meta { font-size: 12px; color: #ee0a24; font-weight: 500; margin-top: 4px; display: flex; gap: 12px; }
.task-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 8px; padding-top: 8px; border-top: 1px solid #f5f5f5; flex-wrap: wrap; }

.ex-type-label { font-size: 14px; font-weight: 500; margin-bottom: 8px; }
.ex-types { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.type-chip {
  padding: 6px 14px; border-radius: 16px; background: #f5f5f5; font-size: 12px;
  cursor: pointer; border: 1px solid transparent; transition: all 0.2s;
}
.type-chip.active { background: #fff0f0; border-color: #ee0a24; color: #ee0a24; font-weight: 500; }
</style>
