<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast } from 'vant'
import request from '@/utils/request'

const router = useRouter()
const form = ref({ realName: '', idCard: '', phone: '' })
const loading = ref(false)

async function submit() {
  if (!form.value.realName) { showToast('请输入真实姓名'); return }
  if (!form.value.idCard) { showToast('请输入身份证号'); return }
  if (form.value.idCard.length !== 18) { showToast('请输入18位身份证号'); return }
  if (!form.value.phone) { showToast('请输入手机号'); return }
  if (form.value.phone.length < 11) { showToast('请输入正确的11位手机号'); return }

  loading.value = true
  try {
    const res: any = await request.post('/auth/register/rider', {
      code: form.value.phone,
      realName: form.value.realName,
      idCard: form.value.idCard,
      phone: form.value.phone
    })
    // Auto-login after registration
    localStorage.setItem('accessToken', res.data.accessToken)
    localStorage.setItem('refreshToken', res.data.refreshToken)
    showSuccessToast('注册成功，请等待管理员审核')
    router.replace('/')
  } catch { /* error handled by interceptor */ }
  loading.value = false
}
</script>

<template>
  <div class="register-page">
    <van-nav-bar title="骑手注册" left-arrow @click-left="router.back()" fixed placeholder />
    <div class="register-tip">
      <van-icon name="info-o" size="16" color="#ff976a" />
      <span>提交认证后，请等待管理员审核通过再登录接单</span>
    </div>
    <van-form @submit="submit" style="margin-top:8px">
      <van-cell-group inset>
        <van-field v-model="form.realName" label="真实姓名" placeholder="请输入真实姓名" required />
        <van-field v-model="form.idCard" label="身份证号" placeholder="请输入18位身份证号" maxlength="18" required />
        <van-field v-model="form.phone" label="手机号" placeholder="请输入手机号（即登录账号）" type="tel" maxlength="11" required />
      </van-cell-group>
      <div style="padding: 16px">
        <van-button round block type="primary" native-type="submit" :loading="loading" color="#07c160">
          提交认证
        </van-button>
      </div>
    </van-form>
    <div class="register-note">
      <p>注册后请在骑手端登录页输入相同手机号查看审核状态</p>
    </div>
  </div>
</template>

<style scoped>
.register-page { padding-bottom: 40px; }
.register-tip {
  display: flex; align-items: center; gap: 8px; padding: 10px 16px;
  background: #fff8e8; color: #b87a14; font-size: 13px; margin: 8px 16px 0;
  border-radius: 8px;
}
.register-note { text-align: center; padding: 16px; }
.register-note p { font-size: 12px; color: #bbb; }
</style>
