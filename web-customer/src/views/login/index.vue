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

      <van-tabs v-model:active="mode" color="#ff6b35" title-active-color="#ff6b35" sticky>
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

      <div class="login-tip">
        <van-divider>体验说明</van-divider>
        <p>输入任意11位手机号即可模拟登录，无需真实验证码</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(160deg, #ff6b35 0%, #ff8c5a 30%, #fff0e8 60%, #fff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}
.login-card {
  width: 100%;
  max-width: 380px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(255, 107, 53, 0.15);
  padding: 32px 24px 24px;
  overflow: hidden;
}
.logo-area {
  text-align: center;
  margin-bottom: 20px;
}
.logo-circle {
  width: 72px; height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff6b35, #ff8c5a);
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 12px;
}
.logo-area h2 {
  font-size: 22px; font-weight: 700; color: #1a1a1a; margin: 0 0 4px;
}
.logo-area p {
  font-size: 13px; color: #999; margin: 0;
}
.login-form {
  margin-top: 20px;
}
.field-wrapper {
  display: flex;
  align-items: center;
  background: #f8f8f8;
  border-radius: 12px;
  margin-bottom: 14px;
  padding: 0 14px;
  border: 2px solid transparent;
  transition: border-color 0.25s, background 0.25s;
}
.field-wrapper:focus-within {
  border-color: #ff6b35;
  background: #fff;
}
.field-icon {
  width: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.text-input {
  flex: 1;
  height: 50px;
  border: none;
  outline: none;
  background: transparent;
  font-size: 15px;
  color: #333;
}
.text-input::placeholder {
  color: #bbb;
}
.submit-btn {
  width: 100%;
  height: 50px;
  border: none;
  border-radius: 25px;
  background: linear-gradient(135deg, #ff6b35, #ff8c5a);
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 8px;
  transition: opacity 0.2s, transform 0.15s;
}
.submit-btn:active {
  transform: scale(0.98);
}
.submit-btn:disabled {
  opacity: 0.7;
}
.login-tip {
  text-align: center;
  margin-top: 8px;
}
.login-tip p {
  font-size: 11px; color: #bbb; margin: 0;
}
</style>
