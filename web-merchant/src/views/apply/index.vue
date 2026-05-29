<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast } from 'vant'
import request from '@/utils/request'

const router = useRouter()
const form = ref({ name: '', phone: '', address: '', description: '', businessHours: '09:00-22:00' })
const loading = ref(false)

async function submit() {
  if (!form.value.name) { showToast('请输入店铺名称'); return }
  if (!form.value.phone) { showToast('请输入手机号'); return }
  if (!form.value.address) { showToast('请输入地址'); return }

  loading.value = true
  try {
    await request.post('/merchant/apply', {
      name: form.value.name,
      phone: form.value.phone,
      address: form.value.address,
      description: form.value.description,
      businessHours: form.value.businessHours,
      code: form.value.phone
    })
    showSuccessToast('申请已提交，请等待管理员审核')
    router.push('/login')
  } catch { /* error handled by interceptor */ }
  loading.value = false
}
</script>

<template>
  <div class="apply-page">
    <van-nav-bar title="商家入驻" left-arrow @click-left="router.back()" fixed placeholder />
    <div class="apply-tip">
      <van-icon name="info-o" size="16" color="#ff976a" />
      <span>提交申请后，请等待管理员审核通过再登录</span>
    </div>
    <van-form @submit="submit" style="margin-top:8px">
      <van-cell-group inset>
        <van-field v-model="form.name" label="店铺名称" placeholder="请输入店铺名称" required />
        <van-field v-model="form.phone" label="手机号" placeholder="请输入手机号（即登录账号）" type="tel" maxlength="11" required />
        <van-field v-model="form.address" label="地址" placeholder="请输入店铺地址" required />
        <van-field v-model="form.description" label="简介" placeholder="介绍一下您的店铺（选填）" type="textarea" rows="2" />
        <van-field v-model="form.businessHours" label="营业时间" placeholder="09:00-22:00" />
      </van-cell-group>
      <div style="padding: 16px">
        <van-button round block type="primary" native-type="submit" :loading="loading" color="#ff6b35">
          提交申请
        </van-button>
      </div>
    </van-form>
    <div class="apply-note">
      <p>提交后请在商家端登录页输入相同手机号查看审核状态</p>
    </div>
  </div>
</template>

<style scoped>
.apply-page { padding-bottom: 40px; }
.apply-tip {
  display: flex; align-items: center; gap: 8px; padding: 10px 16px;
  background: #fff8e8; color: #b87a14; font-size: 13px; margin: 8px 16px 0;
  border-radius: 8px;
}
.apply-note { text-align: center; padding: 16px; }
.apply-note p { font-size: 12px; color: #bbb; }
</style>
