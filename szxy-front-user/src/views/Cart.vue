<template>
  <div class="page">
    <PageLoadingOverlay :visible="loading" />
    <div v-show="!loading" class="page-body">
      <div class="toolbar">
        <h2 class="title">购物车</h2>
        <div class="toolbar-right">
          <button v-if="list.length" type="button" class="link danger" @click="onClear">清空</button>
          <button type="button" class="btn-primary" :disabled="!selectedCount" @click="goCheckout">
            结算（{{ selectedCount }}）
          </button>
        </div>
      </div>

      <div v-if="list.length === 0" class="empty">购物车是空的</div>
      <div v-else class="cart-list">
        <div v-for="item in list" :key="item.id" class="cart-card">
          <el-checkbox
            :model-value="item.selected === 1"
            @change="(v: boolean | string | number) => onToggleSelect(item, !!v)"
          />
          <img class="thumb" :src="item.productImage || placeholder" :alt="item.productName" />
          <div class="info">
            <h3 class="name">{{ item.productName || '商品' }}</h3>
            <p class="meta">
              {{ PRODUCT_TYPE_TEXT[item.productType as number] || '' }}
              · {{ FULFILLMENT_TEXT[item.fulfillmentType as number] || '' }}
            </p>
            <p class="price">{{ formatMoney(item.price) }}</p>
            <div class="qty-row">
              <el-input-number
                v-model="item.quantity"
                :min="1"
                :max="item.stock || 999"
                size="small"
                @change="(v: number | undefined) => onQty(item, v)"
              />
              <button type="button" class="link danger" @click="onDel(item.id)">删除</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageLoadingOverlay from '../components/PageLoadingOverlay.vue'
import { alterCart, clearCart, delCart, listCart } from '../api/cart'
import { getErrorMessage } from '../api/axios'
import { FULFILLMENT_TEXT, PRODUCT_TYPE_TEXT, formatMoney } from '../utils/order'

interface CartItem {
  id: number
  productId: number
  productName?: string
  productImage?: string
  price?: number
  quantity: number
  selected: number
  stock?: number
  productType?: number
  fulfillmentType?: number
  productStatus?: number
  sellerUserId?: number
}

const router = useRouter()
const loading = ref(true)
const list = ref<CartItem[]>([])
const placeholder = 'https://via.placeholder.com/80?text=商品'

const selectedCount = computed(() => list.value.filter((i) => i.selected === 1).length)

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listCart()
    list.value = (res.data || []) as CartItem[]
  } catch (e) {
    ElMessage.error(getErrorMessage(e) || '加载购物车失败')
  } finally {
    loading.value = false
  }
}

const onToggleSelect = async (item: CartItem, selected: boolean) => {
  try {
    await alterCart(item.id, { selected: selected ? 1 : 0 })
    item.selected = selected ? 1 : 0
  } catch (e) {
    ElMessage.error(getErrorMessage(e) || '更新失败')
  }
}

const onQty = async (item: CartItem, qty: number | undefined) => {
  if (!qty || qty < 1) return
  try {
    await alterCart(item.id, { quantity: qty })
  } catch (e) {
    ElMessage.error(getErrorMessage(e) || '修改数量失败')
    await fetchList()
  }
}

const onDel = async (id: number) => {
  try {
    await delCart(id)
    list.value = list.value.filter((i) => i.id !== id)
    ElMessage.success('已删除')
  } catch (e) {
    ElMessage.error(getErrorMessage(e) || '删除失败')
  }
}

const onClear = async () => {
  try {
    await ElMessageBox.confirm('确定清空购物车？', '提示', { type: 'warning' })
    await clearCart()
    list.value = []
    ElMessage.success('已清空')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(getErrorMessage(e) || '清空失败')
  }
}

const goCheckout = () => {
  const selected = list.value.filter((i) => i.selected === 1)
  if (!selected.length) {
    ElMessage.warning('请先勾选商品')
    return
  }
  const types = new Set(selected.map((i) => i.fulfillmentType))
  if (types.size > 1) {
    ElMessage.warning('虚拟与实物不可同单结算，请分开勾选')
    return
  }
  if (types.has(1)) {
    ElMessage.warning('门票/核销票请直接下单，不可从购物车结算')
    return
  }
  const sellers = new Set(selected.map((i) => i.sellerUserId).filter((id) => id != null))
  if (sellers.size > 1) {
    ElMessage.warning('不同商家的商品请分开结算')
    return
  }
  router.push({ path: '/order/confirm', query: { fromCart: '1' } })
}

onMounted(fetchList)
</script>

<style scoped>
.page { min-height: 60vh; }
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; gap: 12px; }
.toolbar-right { display: flex; align-items: center; gap: 12px; }
.title { margin: 0; font-size: 20px; }
.btn-primary {
  border: none; background: #8BC34A; color: #fff; padding: 8px 16px; border-radius: 20px; cursor: pointer;
}
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.link { border: none; background: none; color: #8BC34A; cursor: pointer; }
.link.danger { color: #ef5350; }
.empty { text-align: center; color: #999; padding: 48px 0; }
.cart-list { display: flex; flex-direction: column; gap: 12px; }
.cart-card {
  display: flex; align-items: flex-start; gap: 12px; background: #fff; border-radius: 12px;
  padding: 14px; box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.thumb { width: 80px; height: 80px; object-fit: cover; border-radius: 8px; }
.info { flex: 1; min-width: 0; }
.name { margin: 0 0 4px; font-size: 16px; }
.meta { margin: 0 0 4px; color: #888; font-size: 13px; }
.price { margin: 0 0 8px; color: #e65100; font-weight: 600; }
.qty-row { display: flex; align-items: center; justify-content: space-between; }
</style>
