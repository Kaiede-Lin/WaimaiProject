<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showSuccessToast, showToast } from 'vant'
import request from '@/utils/request'
import { getRiderLevelColor, getRiderLevelLabel, getRiderNextLevelScore } from '@/utils/riderLevel'

const router = useRouter()
const summary = ref<any>({ totalIncome: 0, balance: 0, monthIncome: 0, totalOrders: 0, todayIncome: 0 })
const incomeList = ref<any[]>([])
const withdrawals = ref<any[]>([])
const levelInfo = ref<any>({ level: 'BRONZE', levelScore: 0, totalOrders: 0, score: 5.0 })
const showWithdraw = ref(false)
const withdrawAmount = ref<number | string>(0)

const nextLevelScore = computed(() => getRiderNextLevelScore(levelInfo.value.level))
const levelProgress = computed(() => {
  const score = Number(levelInfo.value.levelScore) || 0
  const target = nextLevelScore.value
  if (!target) return 100
  return Math.min(100, (score / target) * 100)
})
const remainingScore = computed(() => {
  const score = Number(levelInfo.value.levelScore) || 0
  const target = nextLevelScore.value
  if (!target) return 0
  return Math.max(0, target - score)
})

async function fetchAll() {
  try {
    const [summaryRes, incomeRes, withdrawalRes, levelRes] = await Promise.all([
      request.get('/rider/income/summary'),
      request.get('/rider/income/list', { params: { page: 1, size: 20 } }),
      request.get('/rider/withdrawal/list'),
      request.get('/rider/level')
    ])
    summary.value = summaryRes.data || {}
    incomeList.value = incomeRes.data || []
    withdrawals.value = withdrawalRes.data || []
    levelInfo.value = levelRes.data || {}
  } catch {}
}

async function doWithdraw() {
  const amount = Number(withdrawAmount.value)
  if (!amount || amount <= 0) {
    showToast('请输入有效金额')
    return
  }
  if (amount > Number(summary.value.balance || 0)) {
    showToast('余额不足')
    return
  }

  await request.post('/rider/withdrawal', { amount })
  showSuccessToast('提现申请已提交，预计 1-3 个工作日到账')
  showWithdraw.value = false
  withdrawAmount.value = 0
  fetchAll()
}

function handleLogout() {
  showConfirmDialog({ title: '确认退出登录？' })
    .then(() => {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      router.replace('/login')
    })
    .catch(() => {})
}

onMounted(async () => {
  await fetchAll()
})
</script>

<template>
  <div class="income-page">
    <van-nav-bar title="我的收入" fixed placeholder>
      <template #right>
        <van-icon name="setting-o" size="20" @click="handleLogout" />
      </template>
    </van-nav-bar>

    <div
      class="level-card"
      :style="{ background: `linear-gradient(135deg, ${getRiderLevelColor(levelInfo.level)}, ${getRiderLevelColor(levelInfo.level)}dd)` }"
    >
      <div class="level-badge">
        <span class="level-icon">&#9733;</span>
        <span class="level-name">{{ getRiderLevelLabel(levelInfo.level) }}</span>
      </div>

      <div class="level-stats">
        <div class="ls-item">
          <span class="ls-val">{{ levelInfo.totalOrders || 0 }}</span>
          <span class="ls-lbl">累计订单</span>
        </div>
        <div class="ls-item">
          <span class="ls-val">{{ levelInfo.score?.toFixed(1) || '5.0' }}</span>
          <span class="ls-lbl">用户评分</span>
        </div>
        <div class="ls-item">
          <span class="ls-val">{{ levelInfo.levelScore || 0 }}</span>
          <span class="ls-lbl">等级分</span>
        </div>
      </div>

      <div class="level-progress">
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: `${levelProgress}%` }"></div>
        </div>
        <span class="progress-text">{{ nextLevelScore ? `距下一级还需 ${remainingScore} 分` : '已达到最高等级' }}</span>
      </div>
    </div>

    <div class="summary-grid">
      <div class="card"><div class="label">累计收入</div><div class="val">￥{{ (summary.totalIncome || 0).toFixed(2) }}</div></div>
      <div class="card"><div class="label">可提现余额</div><div class="val primary">￥{{ (summary.balance || 0).toFixed(2) }}</div></div>
      <div class="card"><div class="label">本月收入</div><div class="val">￥{{ (summary.monthIncome || 0).toFixed(2) }}</div></div>
      <div class="card"><div class="label">今日收入</div><div class="val">￥{{ (summary.todayIncome || 0).toFixed(2) }}</div></div>
    </div>

    <div class="action-bar">
      <van-button type="primary" round block color="#07c160" @click="showWithdraw = true">申请提现</van-button>
    </div>

    <van-tabs>
      <van-tab title="收入明细">
        <div v-if="incomeList.length === 0" class="empty-state">暂无收入记录</div>
        <div v-for="item in incomeList" :key="item.id" class="income-row">
          <div>
            <div class="income-desc">订单收入</div>
            <div class="income-time">{{ item.createTime?.substring(0, 16) || '' }}</div>
          </div>
          <span class="amount">+￥{{ (item.amount || 0).toFixed(2) }}</span>
        </div>
      </van-tab>

      <van-tab title="提现记录">
        <div v-if="withdrawals.length === 0" class="empty-state">暂无提现记录</div>
        <div v-for="item in withdrawals" :key="item.id" class="income-row">
          <div>
            <div class="income-desc">提现</div>
            <div class="income-time">{{ item.createTime?.substring(0, 16) || '' }}</div>
          </div>
          <span :class="['amount', item.status === 1 ? 'done' : 'pending']">
            -￥{{ (item.amount || 0).toFixed(2) }}
            <van-tag size="mini">{{ item.status === 1 ? '已到账' : item.status === 0 ? '处理中' : '已拒绝' }}</van-tag>
          </span>
        </div>
      </van-tab>
    </van-tabs>

    <van-dialog v-model:show="showWithdraw" title="申请提现" show-cancel-button @confirm="doWithdraw">
      <van-field v-model="withdrawAmount" type="number" label="金额" placeholder="请输入提现金额" />
      <div class="withdraw-tip">可提现余额：￥{{ (summary.balance || 0).toFixed(2) }}</div>
    </van-dialog>

    <van-tabbar route>
      <van-tabbar-item to="/" icon="home-o">大厅</van-tabbar-item>
      <van-tabbar-item to="/tasks" icon="todo-list-o">任务</van-tabbar-item>
      <van-tabbar-item to="/income" icon="gold-coin-o">收入</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<style scoped>
.income-page { padding-bottom: 50px; }

.level-card {
  margin: 8px;
  padding: 16px;
  border-radius: 12px;
  color: #fff;
}

.level-badge { display: flex; align-items: center; gap: 6px; margin-bottom: 10px; }
.level-icon { font-size: 20px; }
.level-name { font-size: 16px; font-weight: 700; }
.level-stats { display: flex; gap: 12px; margin-bottom: 10px; }
.ls-item { flex: 1; text-align: center; }
.ls-val { font-size: 16px; font-weight: 700; display: block; }
.ls-lbl { font-size: 10px; opacity: 0.8; }
.progress-bar { height: 4px; background: rgba(255, 255, 255, 0.3); border-radius: 2px; overflow: hidden; margin-bottom: 4px; }
.progress-fill { height: 100%; background: #fff; border-radius: 2px; transition: width 0.5s; }
.progress-text { font-size: 10px; opacity: 0.8; }

.summary-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; padding: 8px; }
.card { background: #fff; border-radius: 8px; padding: 14px; text-align: center; }
.label { font-size: 11px; color: #999; }
.val { font-size: 20px; font-weight: 700; color: #333; margin-top: 4px; }
.val.primary { color: #ee0a24; }

.action-bar { padding: 0 8px 12px; }
.empty-state { text-align: center; padding: 40px; color: #999; }

.income-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #f5f5f5;
}

.income-desc { font-size: 14px; }
.income-time { font-size: 11px; color: #bbb; margin-top: 2px; }
.amount { font-size: 16px; font-weight: 600; color: #07c160; display: flex; align-items: center; gap: 6px; }
.amount.pending { color: #ff976a; }
.amount.done { color: #999; }
.withdraw-tip { padding: 4px 16px 12px; font-size: 11px; color: #999; }
</style>
