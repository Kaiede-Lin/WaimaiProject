<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast } from 'vant'
import request from '@/utils/request'

const router = useRouter()
const form = ref({ phone: '' })
const loading = ref(false)
const auditStatus = ref<any>(null)

async function checkStatus() {
  if (!form.value.phone || form.value.phone.length < 11) return
  try {
    const res: any = await request.get('/auth/register/rider/status', {
      params: { code: form.value.phone }
    })
    auditStatus.value = res.data
  } catch { /* ignore */ }
}

async function handleLogin() {
  if (!form.value.phone || form.value.phone.length < 11) {
    showToast('请输入正确的11位手机号')
    return
  }
  loading.value = true
  try {
    const res: any = await request.post('/auth/login/rider/wechat', {
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
          <van-icon name="logistics" size="40" color="#fff" />
        </div>
        <h2>骑手中心</h2>
        <p>高效配送 · 收入可观</p>
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
            @blur="checkStatus"
          />
        </div>

        <div v-if="auditStatus" class="status-block">
          <div v-if="auditStatus.auditStatus === 0" class="status-notice warning">
            <van-icon name="clock-o" size="16" />
            <span>您的认证正在审核中，请耐心等待</span>
          </div>
          <div v-if="auditStatus.auditStatus === 2" class="status-notice error">
            <van-icon name="close" size="16" />
            <span>注册被驳回：{{ auditStatus.rejectionReason || '请联系平台' }}</span>
          </div>
          <div v-if="!auditStatus.registered" class="not-registered-box">
            <van-icon name="info-o" size="20" color="#ff976a" />
            <p>您还未注册成为骑手</p>
            <button type="button" class="register-link" @click="router.push('/register')">前往注册认证</button>
          </div>
        </div>

        <button type="submit" class="submit-btn" :disabled="loading">
          <van-loading v-if="loading" size="18" color="#fff" style="margin-right:8px" />
          立即登录
        </button>
      </van-form>

      <div class="login-tip">
        <van-divider>体验说明</van-divider>
        <p>输入任意11位手机号即可模拟登录，首次使用请先注册</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(160deg, #07c160 0%, #05a84d 30%, #e0ffe8 60%, #fff 100%);
  display: flex; align-items: center; justify-content: center; padding: 20px;
}
.login-card {
  width: 100%; max-width: 380px; background: #fff; border-radius: 20px;
  box-shadow: 0 20px 60px rgba(7, 193, 96, 0.12); padding: 32px 24px 24px; overflow: hidden;
}
.logo-area { text-align: center; margin-bottom: 20px; }
.logo-circle {
  width: 72px; height: 72px; border-radius: 50%;
  background: linear-gradient(135deg, #07c160, #05a84d);
  display: flex; align-items: center; justify-content: center; margin: 0 auto 12px;
}
.logo-area h2 { font-size: 22px; font-weight: 700; color: #1a1a1a; margin: 0 0 4px; }
.logo-area p { font-size: 13px; color: #999; margin: 0; }
.login-form { margin-top: 20px; }
.field-wrapper {
  display: flex; align-items: center; background: #f8f8f8; border-radius: 12px;
  margin-bottom: 14px; padding: 0 14px; border: 2px solid transparent;
  transition: border-color 0.25s, background 0.25s;
}
.field-wrapper:focus-within { border-color: #07c160; background: #fff; }
.field-icon { width: 32px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.text-input {
  flex: 1; height: 50px; border: none; outline: none; background: transparent;
  font-size: 15px; color: #333;
}
.text-input::placeholder { color: #bbb; }

.status-block { margin-bottom: 14px; }
.status-notice {
  display: flex; align-items: flex-start; gap: 8px; padding: 12px; border-radius: 10px;
  font-size: 13px; margin-bottom: 8px;
}
.status-notice.warning { background: #fff8e8; color: #b87a14; }
.status-notice.error { background: #fff0f0; color: #c0392b; }
.not-registered-box {
  text-align: center; padding: 20px; background: #e8fff0; border-radius: 12px;
}
.not-registered-box p { font-size: 13px; color: #1a7a3a; margin: 8px 0; }
.register-link {
  background: none; border: none; color: #07c160; font-size: 14px; font-weight: 600;
  cursor: pointer; text-decoration: underline;
}

.submit-btn {
  width: 100%; height: 50px; border: none; border-radius: 25px;
  background: linear-gradient(135deg, #07c160, #05a84d); color: #fff;
  font-size: 16px; font-weight: 600; cursor: pointer;
  display: flex; align-items: center; justify-content: center; margin-top: 8px;
  transition: opacity 0.2s, transform 0.15s;
}
.submit-btn:active { transform: scale(0.98); }
.submit-btn:disabled { opacity: 0.7; }

.login-tip { text-align: center; margin-top: 8px; }
.login-tip p { font-size: 11px; color: #bbb; margin: 0; }
</style>
