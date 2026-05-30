<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast } from 'vant'
import request from '@/utils/request'

const router = useRouter()
const form = ref({ phone: '' })
const loading = ref(false)
const applyStatus = ref<any>(null)

async function checkApplyStatus() {
  if (!form.value.phone || form.value.phone.length < 11) return
  try {
    const res: any = await request.get('/merchant/apply/status', {
      params: { code: form.value.phone }
    })
    applyStatus.value = res.data
  } catch { /* ignore */ }
}

async function handleLogin() {
  if (!form.value.phone || form.value.phone.length < 11) {
    showToast('请输入正确的11位手机号')
    return
  }
  loading.value = true
  try {
    const res: any = await request.post('/auth/login/merchant/wechat', {
      code: form.value.phone,
      nickname: '',
      avatar: ''
    })
    localStorage.setItem('accessToken', res.data.accessToken)
    localStorage.setItem('refreshToken', res.data.refreshToken)
    showSuccessToast('登录成功')
    router.replace('/')
  } catch { /* error handled by interceptor */ }
  loading.value = false
}

onMounted(() => {
  const token = localStorage.getItem('accessToken')
  if (token) router.replace('/')
})
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="logo-area">
        <div class="logo-circle">
          <van-icon name="shop-o" size="40" color="#fff" />
        </div>
        <h2>商家中心</h2>
        <p>管理店铺 · 高效经营</p>
      </div>

      <van-form @submit="handleLogin" class="login-form">
        <div class="field-wrapper">
          <div class="field-icon">
            <van-icon name="phone-o" size="18" color="#999" />
          </div>
          <input
            v-model="form.phone"
            type="tel"
            maxlength="11"
            placeholder="请输入注册手机号"
            class="text-input"
            autocomplete="tel"
            @blur="checkApplyStatus"
          />
        </div>

        <div v-if="applyStatus" class="status-block">
          <div v-if="applyStatus.status === 0" class="status-notice warning">
            <van-icon name="clock-o" size="16" />
            <span>「{{ applyStatus.name }}」审核中，请耐心等待</span>
          </div>
          <div v-if="applyStatus.status === 2" class="status-notice error">
            <van-icon name="close" size="16" />
            <span>申请被驳回：{{ applyStatus.rejectionReason || '请联系平台' }}</span>
          </div>
          <div v-if="applyStatus.status === 3" class="status-notice error">
            <van-icon name="warning-o" size="16" />
            <span>店铺已被停用，请联系平台</span>
          </div>
          <div v-if="applyStatus.status === -1" class="not-applied-box">
            <van-icon name="info-o" size="20" color="#ff976a" />
            <p>您还未申请入驻，请先提交申请</p>
            <button type="button" class="apply-link" @click="router.push('/apply')">前往申请入驻</button>
          </div>
        </div>

        <button type="submit" class="submit-btn" :disabled="loading">
          <van-loading v-if="loading" size="18" color="#fff" style="margin-right:8px" />
          立即登录
        </button>
      </van-form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(160deg, var(--primary) 0%, #ff8c5a 30%, var(--primary-light) 60%, #fff 100%);
  display: flex; align-items: center; justify-content: center; padding: 20px;
}
.login-card {
  width: 100%; max-width: 380px; background: var(--card-bg);
  border-radius: var(--radius-lg); box-shadow: var(--shadow-lg); padding: 36px 28px 28px;
  animation: slideUp 0.5s ease;
}
@keyframes slideUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}
.logo-area { text-align: center; margin-bottom: 24px; }
.logo-circle {
  width: 76px; height: 76px; border-radius: 50%;
  background: var(--primary-gradient);
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 14px;
  box-shadow: 0 8px 24px rgba(255,107,53,0.25);
  animation: pulse 2s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { box-shadow: 0 8px 24px rgba(255,107,53,0.25); }
  50% { box-shadow: 0 8px 32px rgba(255,107,53,0.40); }
}
.logo-area h2 { font-size: 22px; font-weight: 700; color: var(--text); margin: 0 0 4px; }
.logo-area p { font-size: 13px; color: var(--text-secondary); margin: 0; letter-spacing: 1px; }
.login-form { margin-top: 24px; }
.field-wrapper {
  display: flex; align-items: center; background: #f8f8f8; border-radius: var(--radius);
  margin-bottom: 14px; padding: 0 14px; border: 2px solid transparent;
  transition: border-color 0.25s, background 0.25s, box-shadow 0.25s;
}
.field-wrapper:focus-within {
  border-color: var(--primary); background: #fff;
  box-shadow: 0 0 0 4px rgba(255,107,53,0.08);
}
.field-icon { width: 32px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.text-input {
  flex: 1; height: 50px; border: none; outline: none; background: transparent;
  font-size: 15px; color: var(--text);
}
.text-input::placeholder { color: #bbb; }

.status-block { margin-bottom: 14px; }
.status-notice {
  display: flex; align-items: flex-start; gap: 8px; padding: 12px; border-radius: var(--radius-sm);
  font-size: 13px; margin-bottom: 8px;
}
.status-notice.warning { background: #fff8e8; color: #b87a14; }
.status-notice.error { background: #fff0f0; color: #c0392b; }
.not-applied-box {
  text-align: center; padding: 20px; background: var(--primary-light); border-radius: var(--radius);
}
.not-applied-box p { font-size: 13px; color: #b87a14; margin: 8px 0; }
.apply-link {
  background: none; border: none; color: var(--primary); font-size: 14px; font-weight: 600;
  cursor: pointer; text-decoration: none; padding: 6px 16px;
  border-radius: var(--radius-round); background: #fff;
  box-shadow: var(--shadow-sm); transition: transform 0.15s;
}
.apply-link:active { transform: scale(0.96); }

.submit-btn {
  width: 100%; height: 50px; border: none; border-radius: var(--radius-round);
  background: var(--primary-gradient); color: #fff; font-size: 16px; font-weight: 600;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  margin-top: 12px; transition: opacity 0.2s, transform 0.15s, box-shadow 0.2s;
  box-shadow: 0 4px 16px rgba(255,107,53,0.30);
}
.submit-btn:hover { box-shadow: 0 6px 20px rgba(255,107,53,0.40); }
.submit-btn:active { transform: scale(0.98); }
.submit-btn:disabled { opacity: 0.6; box-shadow: none; }
</style>
