<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showSuccessToast } from 'vant'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const paying = ref(false)

async function doPay() {
  paying.value = true
  try {
    await request.post(`/order/${route.params.id}/pay`)
    showSuccessToast('支付成功')
    setTimeout(() => router.replace('/order/list'), 1500)
  } catch { /* ignore */ }
  paying.value = false
}
</script>

<template>
  <div class="pay-page">
    <van-nav-bar title="支付" left-arrow @click-left="router.back()" fixed placeholder />
    <div class="pay-content">
      <van-icon name="checked" size="60" color="#07c160" />
      <h3>订单已创建</h3>
      <p>请选择支付方式</p>
      <van-radio-group :model-value="'wechat'" style="width:100%">
        <van-cell-group>
          <van-cell title="微信支付" clickable @click="doPay"><template #icon><van-icon name="wechat" size="24" color="#07c160" style="margin-right:8px" /></template></van-cell>
          <van-cell title="支付宝" clickable><template #icon><van-icon name="alipay" size="24" color="#1677ff" style="margin-right:8px" /></template></van-cell>
        </van-cell-group>
      </van-radio-group>
      <van-button type="primary" round block :loading="paying" @click="doPay" style="margin-top:24px">确认支付</van-button>
    </div>
  </div>
</template>

<style scoped>
.pay-content { text-align: center; padding: 40px 16px; }
.pay-content h3 { margin: 12px 0 4px; }
.pay-content p { color: #999; margin-bottom: 20px; font-size: 14px; }
</style>
