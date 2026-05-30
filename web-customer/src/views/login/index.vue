<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast } from 'vant'
import request from '@/utils/request'

const router = useRouter()
const mode = ref<'login' | 'register'>('login')
const form = ref({ phone: '', nickname: '' })
const loading = ref(false)

async function handleSubmit() {
  if (!form.value.phone || form.value.phone.length < 11) {
    showToast('请输入正确的11位手机号')
    return
  }
  loading.value = true
  try {
    const res: any = await request.post('/auth/login/wechat', {
      code: form.value.phone,
      nickname: form.value.nickname || '用户' + form.value.phone.slice(-4),
      avatar: ''
    })
    localStorage.setItem('accessToken', res.data.accessToken)
    localStorage.setItem('refreshToken', res.data.refreshToken)
    showSuccessToast(mode.value === 'login' ? '登录成功' : '注册成功')
    router.replace('/')
  } catch { /* error handled by interceptor */ }
  loading.value = false
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="logo-area">
        <div class="logo-circle">
          <van-icon name="shop-o" size="40" color="#fff" />
        </div>
        <h2>美食外卖</h2>
        <p>品质美食 · 即时送达</p>
      </div>

      <van-tabs v-model:active="mode" color="var(--primary)" title-active-color="var(--primary)" sticky>
        <van-tab title="登录" name="login" />
        <van-tab title="注册" name="register" />
      </van-tabs>

      <van-form @submit="handleSubmit" class="login-form">
        <div class="field-wrapper">
          <div class="field-icon">
            <van-icon name="phone-o" size="18" color="#999" />
          </div>
          <input
            v-model="form.phone"
            type="tel"
            maxlength="11"
            placeholder="请输入手机号"
            class="text-input"
            autocomplete="tel"
          />
        </div>

        <div v-if="mode === 'register'" class="field-wrapper">
          <div class="field-icon">
            <van-icon name="user-o" size="18" color="#999" />
          </div>
          <input
            v-model="form.nickname"
            type="text"
            placeholder="请输入昵称（选填）"
            class="text-input"
            autocomplete="nickname"
          />
        </div>

        <button type="submit" class="submit-btn" :disabled="loading">
          <van-loading v-if="loading" size="18" color="#fff" style="margin-right:8px" />
          {{ mode === 'login' ? '立即登录' : '立即注册' }}
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
