<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { showToast, showConfirmDialog, showSuccessToast } from 'vant'
import request from '@/utils/request'

const categories = ref<any[]>([])
const dishes = ref<any[]>([])
const activeCat = ref<number>(0)
const showCatDialog = ref(false)
const showDishPopup = ref(false)
const catForm = ref({ name: '', sort: 0 })
const dishForm = ref<any>({ name: '', price: '', originalPrice: '', stock: '', categoryId: 0, summary: '', image: '' })
const editingDish = ref<any>(null)
const saving = ref(false)

const realCategories = computed(() => categories.value.filter(c => c.id !== 0))

async function fetchCategories() {
  const res: any = await request.get('/merchant/category/list')
  categories.value = [{ id: 0, name: '全部' }, ...(res.data || [])]
}

async function fetchDishes() {
  const res: any = await request.get('/merchant/dish/list')
  dishes.value = res.data || []
}

function filteredDishes() {
  return activeCat.value === 0 ? dishes.value : dishes.value.filter((d: any) => d.categoryId === activeCat.value)
}

async function addCategory() {
  if (!catForm.value.name) { showToast('请输入分类名称'); return }
  try {
    await request.post('/merchant/category', catForm.value)
    showSuccessToast('分类已添加')
    showCatDialog.value = false
    catForm.value = { name: '', sort: 0 }
    fetchCategories()
  } catch { /* handled */ }
}

function openDishForm(dish?: any) {
  if (dish) {
    dishForm.value = {
      name: dish.name, price: String(dish.price || ''), originalPrice: String(dish.originalPrice || ''),
      stock: String(dish.stock || 0), categoryId: dish.categoryId, summary: dish.summary || '',
      image: dish.image || '', status: dish.status
    }
    editingDish.value = dish
  } else {
    const defaultCat = activeCat.value > 0 ? activeCat.value : (realCategories.value[0]?.id || 0)
    dishForm.value = {
      name: '', price: '', originalPrice: '', stock: '0',
      categoryId: defaultCat, summary: '', image: ''
    }
    editingDish.value = null
  }
  showDishPopup.value = true
}

async function saveDish() {
  if (!dishForm.value.name) { showToast('请输入菜品名称'); return }
  const price = Number(dishForm.value.price)
  if (!price || price <= 0) { showToast('请输入有效价格'); return }
  const catId = Number(dishForm.value.categoryId)
  if (!catId || catId <= 0) { showToast('请选择分类'); return }

  saving.value = true
  const payload = {
    name: dishForm.value.name,
    price: price,
    originalPrice: Number(dishForm.value.originalPrice || 0),
    stock: Number(dishForm.value.stock || 0),
    categoryId: catId,
    summary: dishForm.value.summary || '',
    image: dishForm.value.image || ''
  }

  try {
    if (editingDish.value) {
      await request.put(`/merchant/dish/${editingDish.value.id}`, payload)
    } else {
      await request.post('/merchant/dish', payload)
    }
    showSuccessToast('保存成功')
    showDishPopup.value = false
    fetchDishes()
  } catch { /* handled */ }
  saving.value = false
}

async function toggleDishStatus(dish: any) {
  const newStatus = dish.status === 1 ? 0 : 1
  try {
    await request.put(`/merchant/dish/${dish.id}`, { ...dish, status: newStatus })
    showToast(newStatus === 1 ? '已上架' : '已下架')
    fetchDishes()
  } catch { /* handled */ }
}

async function deleteDish(id: number) {
  await showConfirmDialog({ title: '确认删除该菜品？' })
  await request.delete(`/merchant/dish/${id}`)
  showSuccessToast('已删除')
  fetchDishes()
}

onMounted(() => { fetchCategories(); fetchDishes() })
</script>

<template>
  <div class="menu-page">
    <van-nav-bar title="菜单管理" fixed placeholder />

    <div class="menu-header">
      <van-button size="small" plain type="primary" @click="showCatDialog = true">+ 分类</van-button>
      <van-button size="small" type="primary" @click="openDishForm()">+ 菜品</van-button>
      <span class="dish-count">共 {{ dishes.length }} 个菜品</span>
    </div>

    <van-tabs v-model:active="activeCat" sticky swipeable>
      <van-tab v-for="cat in categories" :key="cat.id" :title="cat.name" />
    </van-tabs>

    <div class="dish-list">
      <div v-for="dish in filteredDishes()" :key="dish.id" :class="['dish-row', { disabled: dish.status !== 1 }]">
        <div class="dish-img-pl">
          <van-icon name="photo-o" size="24" color="#ccc" />
        </div>
        <div class="dish-info">
          <div class="dish-name">
            {{ dish.name }}
            <van-tag v-if="dish.status !== 1" type="danger" size="mini">下架</van-tag>
            <van-tag v-if="dish.status === 1" type="success" size="mini">在售</van-tag>
          </div>
          <div class="dish-meta">
            <span class="price">¥{{ dish.price }}</span>
            <span v-if="dish.originalPrice" class="orig">¥{{ dish.originalPrice }}</span>
            <span class="stock">库存: {{ dish.stock }}</span>
            <span class="cat-label">{{ categories.find(c => c.id === dish.categoryId)?.name || '' }}</span>
          </div>
        </div>
        <div class="dish-actions">
          <van-switch :model-value="dish.status === 1" @update:model-value="toggleDishStatus(dish)" size="18" />
          <van-button size="mini" plain @click="openDishForm(dish)">编辑</van-button>
          <van-button size="mini" plain type="danger" @click="deleteDish(dish.id)">删</van-button>
        </div>
      </div>
      <van-empty v-if="filteredDishes().length === 0" description="暂无菜品，请先添加分类再添加菜品" />
    </div>

    <!-- Category Dialog -->
    <van-dialog v-model:show="showCatDialog" title="添加分类" show-cancel-button @confirm="addCategory">
      <van-field v-model="catForm.name" label="名称" placeholder="如：热销、主食、饮品" />
      <van-field v-model="catForm.sort" label="排序" type="digit" placeholder="数字越小越靠前" />
    </van-dialog>

    <!-- Dish Popup Form -->
    <van-popup v-model:show="showDishPopup" position="bottom" round :style="{ height: '85%' }" closeable>
      <div class="dish-form">
        <h3>{{ editingDish ? '编辑菜品' : '新增菜品' }}</h3>
        <van-form @submit="saveDish">
          <van-cell-group inset>
            <van-field v-model="dishForm.name" label="名称" placeholder="菜品名称" required />
            <van-field v-model="dishForm.price" label="价格" type="digit" placeholder="售价（元）" required />
            <van-field v-model="dishForm.originalPrice" label="原价" type="digit" placeholder="划线价，选填" />
            <van-field v-model="dishForm.stock" label="库存" type="digit" placeholder="库存数量" />
            <van-field v-model="dishForm.summary" label="简介" placeholder="简单描述菜品" />
            <van-field v-model="dishForm.image" label="图片" placeholder="图片URL，选填" />
          </van-cell-group>

          <div class="cat-select">
            <span class="cat-label">选择分类：</span>
            <div class="cat-chips">
              <span
                v-for="cat in realCategories" :key="cat.id"
                :class="['cat-chip', { active: dishForm.categoryId === cat.id }]"
                @click="dishForm.categoryId = cat.id"
              >{{ cat.name }}</span>
              <span v-if="realCategories.length === 0" class="no-cat">请先添加分类</span>
            </div>
          </div>

          <div class="form-submit">
            <van-button round block type="primary" native-type="submit" :loading="saving" color="#ff6b35">
              {{ editingDish ? '保存修改' : '确认添加' }}
            </van-button>
          </div>
        </van-form>
      </div>
    </van-popup>

    <van-tabbar route>
      <van-tabbar-item to="/" icon="home-o">首页</van-tabbar-item>
      <van-tabbar-item to="/orders" icon="orders-o">订单</van-tabbar-item>
      <van-tabbar-item to="/menu" icon="wap-home-o">菜单</van-tabbar-item>
      <van-tabbar-item to="/reports" icon="chart-trending-o">报表</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<style scoped>
.menu-page { padding-bottom: 50px; background: #f5f5f5; }
.menu-header {
  display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #fff;
}
.dish-count { margin-left: auto; font-size: 12px; color: #999; }

.dish-list { padding: 8px; }
.dish-row {
  display: flex; gap: 10px; padding: 10px; background: #fff; border-radius: 8px;
  margin-bottom: 8px; align-items: center;
}
.dish-row.disabled { opacity: 0.6; }
.dish-img-pl {
  width: 48px; height: 48px; border-radius: 6px; background: #f5f5f5;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.dish-info { flex: 1; min-width: 0; }
.dish-name { font-size: 14px; font-weight: 500; display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.dish-meta { display: flex; align-items: center; gap: 8px; font-size: 12px; margin-top: 2px; }
.price { color: #ee0a24; font-weight: 600; font-size: 14px; }
.orig { color: #bbb; text-decoration: line-through; font-size: 11px; }
.stock { color: #666; }
.cat-label { color: #409EFF; font-size: 11px; }
.dish-actions { display: flex; flex-direction: column; gap: 4px; align-items: flex-end; flex-shrink: 0; }

.dish-form { padding: 20px 0 40px; }
.dish-form h3 { text-align: center; font-size: 17px; margin-bottom: 16px; }

.cat-select { padding: 12px 16px; }
.cat-label { font-size: 13px; color: #666; margin-bottom: 8px; display: block; }
.cat-chips { display: flex; flex-wrap: wrap; gap: 8px; }
.cat-chip {
  padding: 6px 16px; border-radius: 20px; background: #f5f5f5; font-size: 13px;
  cursor: pointer; border: 2px solid transparent;
}
.cat-chip.active { background: #fff0e8; border-color: #ff6b35; color: #ff6b35; font-weight: 600; }
.no-cat { font-size: 12px; color: #ee0a24; }

.form-submit { padding: 16px; }
</style>
