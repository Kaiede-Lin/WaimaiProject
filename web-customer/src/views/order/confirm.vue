<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast } from 'vant'
import request from '@/utils/request'
import { calcDeliveryFee, calcDeliveryTime, haversineKm, getStoredLocation, saveLocation, setLocationFromAddress } from '@/utils/delivery'

const router = useRouter()
const cart = ref<any>({ items: [], totalAmount: 0, deliveryFee: 0, merchantId: null })
const dynamicDeliveryFee = ref(0)
const dynamicDeliveryTime = ref(30)
const address = ref('')
const remark = ref('')
const loading = ref(false)
const myCoupons = ref<any[]>([])
const availableCoupons = ref<any[]>([])
const selectedCouponId = ref<number | null>(null)
const showCoupons = ref(false)
const receivingCoupon = ref(false)
const savedAddresses = ref<any[]>([])
const showAddressSelect = ref(false)
const selectedAddressId = ref<number | null>(null)
const customAddress = ref('')

const displayDeliveryFee = computed(() => dynamicDeliveryFee.value || cart.value.deliveryFee || 5)

const finalTotal = computed(() => {
  const subtotal = cart.value.totalAmount || 0
  const delivery = displayDeliveryFee.value
  const discount = selectedCouponObj.value?.discountValue || 0
  return Math.max(0, subtotal + delivery - discount)
})

const selectedCouponObj = computed(() => {
  return myCoupons.value.find(c => c.id === selectedCouponId.value) || null
})

async function fetchCart() {
  try {
    const res: any = await request.get('/cart')
    cart.value = res.data || { items: [], totalAmount: 0, deliveryFee: 0 }
    loadAddress()
    fetchSavedAddresses()
    fetchMyCoupons()
    fetchAvailableCoupons()
    calcDynamicDelivery()
  } catch {}
}

async function calcDynamicDelivery() {
  if (!cart.value.merchantId) return
  try {
    const loc = getStoredLocation()
    const mRes: any = await request.get(`/merchant/${cart.value.merchantId}`)
    const m = mRes.data
    if (m?.longitude && m?.latitude) {
      const dist = haversineKm(
        loc.lat, loc.lng,
        Number(m.latitude), Number(m.longitude)
      )
      dynamicDeliveryFee.value = calcDeliveryFee(dist)
      dynamicDeliveryTime.value = calcDeliveryTime(dist)
    }
  } catch {}
}

function loadAddress() {
  const saved = localStorage.getItem('lastAddress')
  if (saved) address.value = saved
}

async function fetchSavedAddresses() {
  try {
    const res: any = await request.get('/user/address')
    savedAddresses.value = res.data || []
  } catch { savedAddresses.value = [] }
}

function selectAddress(addr: any) {
  selectedAddressId.value = addr.id
  address.value = addr.address + (addr.detail ? ' ' + addr.detail : '')
  setLocationFromAddress(addr)
  showAddressSelect.value = false
  calcDynamicDelivery()
}

function useCustomAddress() {
  selectedAddressId.value = null
  address.value = customAddress.value
  showAddressSelect.value = false
}

async function fetchMyCoupons() {
  try {
    const res: any = await request.get('/coupon/my')
    myCoupons.value = (res.data || []).filter((c: any) => c.status === 'UNUSED')
  } catch { myCoupons.value = [] }
}

async function fetchAvailableCoupons() {
  if (!cart.value.merchantId) return
  try {
    const amount = Number(cart.value.totalAmount || 0) + Number(cart.value.deliveryFee || 0)
    const res: any = await request.get('/coupon/available', {
      params: { merchantId: cart.value.merchantId, orderAmount: amount.toFixed(2) }
    })
    availableCoupons.value = (res.data || []).filter((c: any) => !c.received)
  } catch { availableCoupons.value = [] }
}

async function receiveCoupon(couponId: number) {
  receivingCoupon.value = true
  try {
    await request.post(`/coupon/receive/${couponId}`)
    showSuccessToast('领取成功')
    await fetchMyCoupons()
    await fetchAvailableCoupons()
  } catch { /* handled */ }
  receivingCoupon.value = false
}

async function placeOrder() {
  if (!address.value.trim()) { showToast('请输入收货地址'); return }
  loading.value = true
  try {
    const items = cart.value.items.map((i: any) => ({
      dishId: i.dishId, quantity: i.quantity, dishName: i.dishName, price: i.price
    }))
    const loc = getStoredLocation()
    const payload: any = {
      merchantId: cart.value.merchantId,
      address: address.value.trim(),
      items,
      remark: remark.value,
      couponId: selectedCouponId.value || null
    }
    if (loc.lng !== 116.397428 || loc.lat !== 39.90923) {
      payload.addressLng = loc.lng
      payload.addressLat = loc.lat
    }
    const res: any = await request.post('/order/place', payload)
    localStorage.setItem('lastAddress', address.value.trim())
    showSuccessToast('下单成功')
    router.push(`/order/pay/${res.data.id}`)
  } catch {}
  loading.value = false
}

const couponLabel = computed(() => {
  if (selectedCouponObj.value) return `-¥${selectedCouponObj.value.discountValue}`
  const total = myCoupons.value.length + availableCoupons.value.length
  return total > 0 ? `${total}张可用` : '暂无'
})

onMounted(fetchCart)
</script>

<template>
  <div class="confirm-page">
    <van-nav-bar title="确认订单" left-arrow @click-left="router.back()" fixed placeholder />

    <van-cell-group inset title="收货地址">
      <div class="address-select-row" @click="showAddressSelect = true">
        <van-icon name="location-o" size="18" color="#409EFF" />
        <span class="addr-display">{{ address || '点击选择收货地址' }}</span>
        <van-icon name="arrow" size="14" color="#999" />
      </div>
    </van-cell-group>

    <div class="items-section">
      <div class="section-title">商品明细</div>
      <div v-for="item in cart.items" :key="item.dishId" class="cart-item">
        <div class="item-info">
          <span class="item-name">{{ item.dishName }}</span>
          <span class="item-qty">x{{ item.quantity }}</span>
        </div>
        <span class="price">¥{{ (item.price * item.quantity).toFixed(2) }}</span>
      </div>
      <van-empty v-if="!cart.items || cart.items.length === 0" description="购物车为空" />
    </div>

    <van-cell-group inset style="margin-top:8px">
      <van-cell title="优惠券" is-link :value="couponLabel" @click="showCoupons = true" />
      <van-field v-model="remark" label="备注" placeholder="订单备注（选填）" />
    </van-cell-group>

    <div class="price-breakdown">
      <div class="row"><span>商品总额</span><span>¥{{ (cart.totalAmount || 0).toFixed(2) }}</span></div>
      <div class="row"><span>配送费</span><span>¥{{ displayDeliveryFee.toFixed(2) }}</span></div>
      <div class="row delivery-time"><span>预计送达</span><span>约{{ dynamicDeliveryTime }}分钟</span></div>
      <div v-if="selectedCouponObj" class="row discount">
        <span>优惠 ({{ selectedCouponObj.name }})</span><span>-¥{{ selectedCouponObj.discountValue?.toFixed(2) }}</span>
      </div>
      <div class="row total"><span>实付</span><span class="final">¥{{ finalTotal.toFixed(2) }}</span></div>
    </div>

    <div class="submit-bar">
      <div class="submit-info">
        <span class="total-label">实付</span>
        <span class="total-price">¥{{ finalTotal.toFixed(2) }}</span>
      </div>
      <van-button type="primary" round :loading="loading" @click="placeOrder" color="#ff6b35">提交订单</van-button>
    </div>

    <van-action-sheet v-model:show="showAddressSelect" title="选择收货地址">
      <div class="address-sheet">
        <div v-if="savedAddresses.length > 0" class="section-label">已保存的地址</div>
        <div v-for="addr in savedAddresses" :key="addr.id"
          :class="['addr-item', { active: selectedAddressId === addr.id }]"
          @click="selectAddress(addr)"
        >
          <div class="addr-main">
            <span class="addr-name">{{ addr.name }}</span>
            <span class="addr-phone">{{ addr.phone }}</span>
          </div>
          <div class="addr-detail">{{ addr.address }} {{ addr.detail }}</div>
          <van-icon v-if="selectedAddressId === addr.id" name="success" color="#ee0a24" size="18" />
        </div>
        <div class="section-label" style="margin-top:8px">或手动输入</div>
        <div class="custom-addr-row">
          <van-field v-model="customAddress" placeholder="输入新地址" />
          <van-button size="small" type="primary" @click="useCustomAddress">使用</van-button>
        </div>
      </div>
    </van-action-sheet>

    <van-action-sheet v-model:show="showCoupons" title="优惠券">
      <div class="coupon-section">
        <!-- Already received, unused coupons -->
        <div v-if="myCoupons.length > 0" class="section-label">已领取 · 点击使用</div>
        <div v-for="c in myCoupons" :key="'my-'+c.id"
          :class="['coupon-card', { active: selectedCouponId === c.id }]"
          @click="selectedCouponId = selectedCouponId === c.id ? null : c.id; showCoupons = false"
        >
          <div class="c-amount">¥{{ c.discountValue }}</div>
          <div class="c-info">
            <div class="c-name">{{ c.name }}</div>
            <div class="c-cond" v-if="c.threshold">满¥{{ c.threshold }}可用</div>
          </div>
          <van-icon v-if="selectedCouponId === c.id" name="success" color="#ee0a24" size="20" />
        </div>

        <!-- Available coupons to receive -->
        <div v-if="availableCoupons.length > 0" class="section-label">可领取</div>
        <div v-for="c in availableCoupons" :key="'av-'+c.id" class="coupon-card receive"
          @click="receiveCoupon(c.id)"
        >
          <div class="c-amount">¥{{ c.discountValue }}</div>
          <div class="c-info">
            <div class="c-name">{{ c.name }}</div>
            <div class="c-cond" v-if="c.threshold">满¥{{ c.threshold }}可用</div>
          </div>
          <van-button size="mini" type="danger" plain round :loading="receivingCoupon">领取</van-button>
        </div>

        <div v-if="myCoupons.length === 0 && availableCoupons.length === 0" class="empty-coupons">
          <van-icon name="coupon-o" size="40" color="#ccc" />
          <p>暂无可用优惠券</p>
        </div>
      </div>
    </van-action-sheet>
  </div>
</template>

<style scoped>
.confirm-page { padding-bottom: 90px; }
.items-section { padding: 12px 16px; background: #fff; margin: 8px 0; }
.section-title { font-size: 14px; font-weight: 600; margin-bottom: 8px; }
.cart-item { display: flex; align-items: center; gap: 10px; padding: 8px 0; border-bottom: 1px solid #f5f5f5; }
.item-info { flex: 1; }
.item-name { font-size: 14px; display: block; }
.item-qty { font-size: 12px; color: #999; }
.price { color: #ee0a24; font-weight: 500; font-size: 14px; }
.price-breakdown { padding: 12px 16px; background: #fff; margin-top: 8px; }
.row { display: flex; justify-content: space-between; padding: 4px 0; font-size: 13px; }
.row.total { font-weight: 600; font-size: 15px; margin-top: 6px; border-top: 1px solid #f0f0f0; padding-top: 8px; }
.row.delivery-time { color: #666; }
.row.discount { color: #ee0a24; }
.final { color: #ee0a24; font-size: 16px; }
.submit-bar {
  position: fixed; bottom: 0; left: 0; right: 0; background: #fff; padding: 10px 16px;
  display: flex; justify-content: space-between; align-items: center;
  box-shadow: 0 -2px 8px rgba(0,0,0,0.06); z-index: 99;
}
.submit-info { display: flex; align-items: baseline; gap: 4px; }
.total-label { font-size: 13px; color: #666; }
.total-price { font-size: 20px; font-weight: 700; color: #ee0a24; }

.coupon-section { padding: 8px 16px 30px; }
.section-label { font-size: 12px; color: #999; padding: 8px 0 4px; }
.coupon-card {
  display: flex; align-items: center; gap: 12px; padding: 12px;
  background: #fff8f0; border-radius: 10px; margin-bottom: 8px; cursor: pointer;
  border: 2px solid transparent;
}
.coupon-card.active { border-color: #ee0a24; background: #fff0f0; }
.coupon-card.receive { cursor: default; }
.c-amount { font-size: 22px; font-weight: 700; color: #ee0a24; min-width: 55px; }
.c-info { flex: 1; }
.c-name { font-size: 13px; font-weight: 500; }
.c-cond { font-size: 11px; color: #999; margin-top: 2px; }
.empty-coupons { text-align: center; padding: 40px 16px; }
.empty-coupons p { color: #999; margin: 8px 0 4px; }

.address-select-row {
  display: flex; align-items: center; gap: 8px; padding: 12px 16px; cursor: pointer;
}
.addr-display { flex: 1; font-size: 14px; color: #333; }
.address-sheet { padding: 8px 16px 30px; }
.section-label { font-size: 12px; color: #999; padding: 8px 0 4px; }
.addr-item {
  display: flex; align-items: flex-start; gap: 8px; padding: 12px;
  background: #f8f8f8; border-radius: 8px; margin-bottom: 8px; cursor: pointer;
  border: 2px solid transparent; flex-wrap: wrap;
}
.addr-item.active { border-color: #ee0a24; background: #fff0f0; }
.addr-main { display: flex; gap: 8px; align-items: center; width: 100%; }
.addr-name { font-weight: 600; font-size: 14px; }
.addr-phone { color: #666; font-size: 13px; }
.addr-detail { font-size: 12px; color: #666; flex: 1; min-width: 0; }
.custom-addr-row { display: flex; align-items: center; gap: 8px; padding: 8px 0; }
</style>
