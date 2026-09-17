<template>
  <div class="page">
    <PageLoadingOverlay :visible="loading" />
    <div v-if="!loading && order" class="page-body">
      <div class="card">
        <div class="row between">
          <span>订单号 {{ order.orderNo }}</span>
          <span class="status">{{ orderStatusText(order.status) }}</span>
        </div>
        <p class="muted">{{ FULFILLMENT_TEXT[order.fulfillmentType] || '' }} · {{ order.createTime }}</p>
        <p v-if="order.addressSnapshot" class="addr">收货：{{ addrText }}</p>
        <p v-if="order.remark">备注：{{ order.remark }}</p>
      </div>

      <div class="card">
        <h3>商品</h3>
        <div v-for="it in order.items || []" :key="it.id" class="item-row">
          <img :src="it.itemImage || placeholder" class="thumb" alt="" />
          <div class="item-info">
            <div>{{ it.itemName }}</div>
            <div class="muted">×{{ it.quantity }} · {{ formatMoney(it.unitPrice) }}</div>
            <div v-if="it.verifyCode" class="code">核销码：{{ it.verifyCode }}</div>
          </div>
          <div class="money">{{ formatMoney(it.subtotal) }}</div>
        </div>
        <div class="total">实付 {{ formatMoney(order.payAmount) }}</div>
      </div>

      <div class="actions">
        <button v-if="order.status === 1" type="button" class="btn" :disabled="acting" @click="onPay">去支付</button>
        <button v-if="order.status === 1" type="button" class="btn ghost" :disabled="acting" @click="onCancel">取消订单</button>
        <button v-if="order.status === 6" type="button" class="btn" :disabled="acting" @click="onConfirm">确认收货</button>
        <button v-if="order.status === 3" type="button" class="btn" :disabled="acting" @click="onUse">确认使用</button>
        <button
          v-if="canRefund"
          type="button"
          class="btn ghost"
          :disabled="acting"
          @click="onRefund"
        >申请退款</button>
        <button
          v-if="canDelete"
          type="button"
          class="btn ghost danger"
          :disabled="acting"
          @click="onDelete"
        >删除订单</button>
        <button type="button" class="btn ghost" @click="router.push('/wallet')">我的钱包</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageLoadingOverlay from '../components/PageLoadingOverlay.vue'
import {
  applyRefund,
  cancelOrder,
  confirmReceipt,
  getOrderDetail,
  softDeleteOrder,
  useOrder
} from '../api/order'
import { payOrder } from '../api/wallet'
import { getErrorMessage } from '../api/axios'
import { FULFILLMENT_TEXT, formatMoney, orderStatusText } from '../utils/order'

const route = useRoute()
const router = useRouter()
const placeholder = 'https://via.placeholder.com/56?text=商品'
const loading = ref(true)
const acting = ref(false)
const order = ref<any>(null)

const orderId = computed(() => Number(route.params.id))

const canRefund = computed(() => [3, 4, 5, 6, 7].includes(Number(order.value?.status)))
const canDelete = computed(() => [2, 7, 8, 10].includes(Number(order.value?.status)))

const addrText = computed(() => {
  const raw = order.value?.addressSnapshot
  if (!raw) return ''
  try {
    const a = typeof raw === 'string' ? JSON.parse(raw) : raw
    return `${a.receiverName || ''} ${a.phone || ''} ${a.province || ''}${a.city || ''}${a.county || ''}${a.detail || ''}`
  } catch {
    return String(raw)
  }
})

const fetchDetail = async () => {
  loading.value = true
  try {
    const res = await getOrderDetail(orderId.value)
    order.value = res.data
  } catch (e) {
    ElMessage.error(getErrorMessage(e) || '加载失败')
  } finally {
    loading.value = false
  }
}

const wrap = async (fn: () => Promise<void>, okMsg: string) => {
  acting.value = true
  try {
    await fn()
    ElMessage.success(okMsg)
    await fetchDetail()
  } catch (e) {
    ElMessage.error(getErrorMessage(e) || '操作失败')
  } finally {
    acting.value = false
  }
}

const onPay = () => wrap(() => payOrder(orderId.value).then(() => undefined), '支付成功')
const onCancel = async () => {
  try {
    await ElMessageBox.confirm('确定取消？', '提示', { type: 'warning' })
    await wrap(() => cancelOrder(orderId.value).then(() => undefined), '已取消')
  } catch { /* cancel */ }
}
const onConfirm = () => wrap(() => confirmReceipt(orderId.value).then(() => undefined), '已确认收货')
const onUse = () => wrap(() => useOrder(orderId.value).then(() => undefined), '已确认使用')
const onRefund = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请填写退款原因', '申请退款', {
      confirmButtonText: '提交',
      cancelButtonText: '取消',
      inputPattern: /\S+/,
      inputErrorMessage: '原因不能为空'
    })
    await wrap(() => applyRefund(orderId.value, value).then(() => undefined), '已申请退款')
  } catch { /* cancel */ }
}
const onDelete = async () => {
  try {
    await ElMessageBox.confirm('确定删除该订单？', '提示', { type: 'warning' })
    await softDeleteOrder(orderId.value)
    ElMessage.success('已删除')
    router.replace('/orders')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(getErrorMessage(e) || '删除失败')
  }
}

onMounted(fetchDetail)
</script>

<style scoped>
.page { min-height: 60vh; }
.card {
  background: #fff; border-radius: 12px; padding: 16px; margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.row.between { display: flex; justify-content: space-between; }
.status { color: #8BC34A; font-weight: 600; }
.muted { color: #888; font-size: 13px; margin: 6px 0; }
.addr { color: #555; }
.item-row { display: flex; gap: 10px; align-items: center; padding: 8px 0; border-bottom: 1px solid #f5f5f5; }
.thumb { width: 56px; height: 56px; border-radius: 8px; object-fit: cover; }
.item-info { flex: 1; }
.code { margin-top: 4px; color: #2e7d32; font-size: 13px; }
.money { color: #e65100; font-weight: 600; }
.total { text-align: right; margin-top: 10px; font-weight: 600; }
.actions { display: flex; flex-wrap: wrap; gap: 10px; }
.btn {
  border: none; background: #8BC34A; color: #fff; padding: 8px 16px; border-radius: 20px; cursor: pointer;
}
.btn:disabled { opacity: 0.6; }
.btn.ghost { background: #fff; color: #8BC34A; border: 1px solid #8BC34A; }
.btn.ghost.danger { color: #ef5350; border-color: #ef5350; }
</style>
