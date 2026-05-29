<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const merchant = ref<any>({})
const categories = ref<any[]>([])
const dishes = ref<any[]>([])
const activeCategory = ref<number>(0)
const cartQuantities = ref<Record<number, number>>({})
const cartTotal = computed(() => Object.values(cartQuantities.value).reduce((a: number, b: number) => a + b, 0))

async function fetchMerchant() {
  const res: any = await request.get(`/merchant/${route.params.id}`)
  merchant.value = res.data || {}
}
const mid = Number(route.params.id)

async function fetchCategories() {
  try {
    const res: any = await request.get(`/merchant/${mid}/categories`)
    categories.value = [{ id: 0, name: '全部' }, ...(res.data || [])]
  } catch { categories.value = [{ id: 0, name: '全部' }] }
}
async function fetchDishes(catId?: number) {
  try {
    let res: any
    if (catId && catId > 0) {
      res = await request.get(`/merchant/${mid}/dishes/category/${catId}`)
      dishes.value = (res.data || []).filter((d: any) => d.status === 1)
    } else {
      res = await request.get(`/merchant/${mid}/dishes`, { params: { page: 1, size: 200 } })
      dishes.value = ((res.data?.records || []) as any[]).filter((d: any) => d.status === 1)
    }
  } catch { dishes.value = [] }
}

function selectCategory(cat: any) {
  activeCategory.value = cat.id
  fetchDishes(cat.id)
}

function addToCart(dish: any) {
  if (dish.stock <= 0) { showToast('库存不足'); return }
  const qty = (cartQuantities.value[dish.id] || 0) + 1
  if (qty > dish.stock) { showToast('库存不足'); return }
  cartQuantities.value[dish.id] = qty
  request.post('/cart/add', { dishId: dish.id, quantity: 1, dishName: dish.name, dishImage: dish.image, price: dish.price }).catch(() => {
    cartQuantities.value[dish.id]--
  })
}

function removeFromCart(dish: any) {
  const qty = cartQuantities.value[dish.id] || 0
  if (qty <= 0) return
  cartQuantities.value[dish.id] = qty - 1
  request.post('/cart/remove', { dishId: dish.id })
}

async function fetchCart() {
  try {
    const res: any = await request.get('/cart')
    const items = res.data?.items || []
    items.forEach((i: any) => { cartQuantities.value[i.dishId] = i.quantity })
  } catch { /* ignore */ }
}

function goCheckout() {
  if (cartTotal.value === 0) { showToast('请先添加商品'); return }
  router.push('/order/confirm')
}

onMounted(async () => {
  await fetchMerchant()
  await fetchCategories()
  await fetchDishes()
  await fetchCart()
})
</script>

<template>
  <div class="merchant-page">
    <van-nav-bar :title="merchant.name || '商家详情'" left-arrow @click-left="router.back()" fixed placeholder />

    <div class="merchant-banner">
      <van-image :src="merchant.banner || 'data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 375 140%22%3E%3Crect fill=%22%23f5f5f5%22 width=%22375%22 height=%22140%22/%3E%3Ctext x=%22187%22 y=%2275%22 text-anchor=%22middle%22 fill=%22%23ccc%22 font-size=%2232%22%3E暂无封面%3C/text%3E%3C/svg%3E'" height="140" />
      <div class="merchant-head">
        <van-image :src="merchant.logo || 'data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 60 60%22%3E%3Crect fill=%22%23fff0e8%22 width=%2260%22 height=%2260%22/%3E%3Crect x=%2215%22 y=%2215%22 width=%2230%22 height=%2230%22 rx=%226%22 fill=%22%23ff6b35%22/%3E%3Ctext x=%2230%22 y=%2236%22 text-anchor=%22middle%22 fill=%22white%22 font-size=%2220%22%3E店%3C/text%3E%3C/svg%3E'" width="60" height="60" radius="8" />
        <div class="merchant-head-info">
          <h3>{{ merchant.name }}</h3>
          <p>{{ merchant.description }}</p>
        </div>
      </div>
    </div>

    <div class="menu-area">
      <div class="category-sidebar">
        <div v-for="cat in categories" :key="cat.id"
             :class="['cat-item', { active: activeCategory === cat.id }]"
             @click="selectCategory(cat)">{{ cat.name }}</div>
      </div>
      <div class="dish-list">
        <div v-for="dish in dishes" :key="dish.id" class="dish-item">
          <div class="dish-info">
            <div class="dish-name">{{ dish.name }}</div>
            <div class="dish-desc">{{ dish.summary }}</div>
            <div class="dish-price">¥{{ dish.price }}
              <span v-if="dish.originalPrice" class="orig-price">¥{{ dish.originalPrice }}</span>
            </div>
          </div>
          <div class="dish-actions">
            <van-stepper
              :model-value="cartQuantities[dish.id] || 0"
              :max="dish.stock"
              @plus="addToCart(dish)"
              @minus="removeFromCart(dish)"
            />
          </div>
        </div>
      </div>
    </div>

    <div class="cart-bar" v-if="cartTotal > 0">
      <div class="cart-info">
        <van-icon name="cart-o" size="24" />
        <span>{{ cartTotal }} 件商品</span>
      </div>
      <van-button type="primary" size="small" round @click="goCheckout">去结算</van-button>
    </div>
  </div>
</template>

<style scoped>
.merchant-page { padding-bottom: 60px; }
.merchant-banner { position: relative; }
.merchant-head { display: flex; gap: 10px; padding: 12px; background: #fff; align-items: center; }
.merchant-head-info h3 { font-size: 16px; margin-bottom: 4px; }
.merchant-head-info p { font-size: 12px; color: #999; }
.menu-area { display: flex; }
.category-sidebar { width: 80px; background: #f8f8f8; min-height: calc(100vh - 320px); }
.cat-item { padding: 14px 8px; font-size: 13px; text-align: center; border-left: 3px solid transparent; cursor: pointer; }
.cat-item.active { background: #fff; border-left-color: #409EFF; color: #409EFF; font-weight: 600; }
.dish-list { flex: 1; padding: 8px; background: #fff; }
.dish-item { display: flex; padding: 10px 0; border-bottom: 1px solid #f0f0f0; gap: 10px; }
.dish-info { flex: 1; }
.dish-name { font-size: 14px; font-weight: 500; }
.dish-desc { font-size: 11px; color: #999; margin: 4px 0; }
.dish-price { color: #ee0a24; font-size: 15px; font-weight: 600; }
.orig-price { color: #999; font-size: 12px; text-decoration: line-through; margin-left: 6px; font-weight: 400; }
.dish-actions { display: flex; align-items: flex-end; }
.cart-bar {
  position: fixed; bottom: 50px; left: 0; right: 0; background: #fff; padding: 10px 16px;
  display: flex; justify-content: space-between; align-items: center; box-shadow: 0 -2px 8px rgba(0,0,0,0.06); z-index: 99;
}
.cart-info { display: flex; align-items: center; gap: 6px; font-size: 14px; }
</style>
