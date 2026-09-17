<template>
  <div class="page">
    <PageLoadingOverlay :visible="loading" />
    <div v-show="!loading" class="page-body">
      <div class="toolbar">
        <h2 class="title">我的订单</h2>
        <el-select v-model="statusFilter" clearable placeholder="全部状态" style="width: 140px" @change="reload">
          <el-option v-for="(label, code) in ORDER_STATUS_TEXT" :key="code" :label="label" :value="Number(code)" />
        </el-select>
      </div>

      <div v-if="orders.length === 0" class="empty">暂无订单</div>
      <div v-else class="order-list">
        <div v-for="o in orders" :key="o.id" class="order-card" @click="goDetail(o.id)">
          <div class="order-head">
            <span>{{ o.orderNo }}</span>
            <span class="status">{{ orderStatusText(o.status) }}</span>
          </div>
          <div v-for="it in o.items || []" :key="it.id" class="item-row">
            <img :src="it.itemImage || placeholder" class="thumb" alt="" />
            <div class="item-info">
              <div>{{ it.itemName }}</div>
              <div class="muted">×{{ it.quantity }}</div>
            </div>
            <div class="money">{{ formatMoney(it.subtotal) }}</div>
          </div>
          <div class="order-foot">
            <span>合计 {{ formatMoney(o.payAmount) }}</span>
            <div class="actions" @click.stop>
              <button v-if="o.status === 1" type="button" class="btn" @click="onPay(o.id)">去支付</button>
              <button v-if="o.status === 1" type="button" class="btn ghost" @click="onCancel(o.id)">取消</button>
              <button v-if="o.status === 6" type="button" class="btn" @click="onConfirm(o.id)">确认收货</button>
              <button v-if="o.status === 3" type="button" class="btn" @click="onUse(o.id)">确认使用</button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="total > pageSize" class="pager">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="pageNo"
          @current-change="onPage"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageLoadingOverlay from '../components/PageLoadingOverlay.vue'
import { cancelOrder, confirmReceipt, listOrders, useOrder } from '../api/order'
import { payOrder } from '../api/wallet'
import { getErrorMessage } from '../api/axios'
import { ORDER_STATUS_TEXT, formatMoney, orderStatusText } from '../utils/order'

const router = useRouter()
const placeholder = 'https://via.placeholder.com/48?text=商品'
const loading = ref(true)
const orders = ref<any[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = 10
const statusFilter = ref<number | undefined>()

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listOrders(pageNo.value, pageSize, statusFilter.value)
    orders.value = res.data?.data || []
    total.value = Number(res.data?.total || 0)
  } catch (e) {
    ElMessage.error(getErrorMessage(e) || '加载订单失败')
  } finally {
    loading.value = false
  }
}

const reload = () => {
  pageNo.value = 1
  fetchList()
}

const onPage = (p: number) => {
  pageNo.value = p
  fetchList()
}

const goDetail = (id: number) => router.push(`/orders/${id}`)

const onPay = async (id: number) => {
  try {
    await payOrder(id)
    ElMessage.success('支付成功')
    await fetchList()
  } catch (e) {
    ElMessage.error(getErrorMessage(e) || '支付失败')
  }
}

const onCancel = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定取消该订单？', '提示', { type: 'warning' })
    await cancelOrder(id)
    ElMessage.success('已取消')
    await fetchList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(getErrorMessage(e) || '取消失败')
  }
}

const onConfirm = async (id: number) => {
  try {
    await confirmReceipt(id)
    ElMessage.success('已确认收货')
    await fetchList()
  } catch (e) {
    ElMessage.error(getErrorMessage(e) || '操作失败')
  }
}

const onUse = async (id: number) => {
  try {
    await useOrder(id)
    ElMessage.success('已确认使用')
    await fetchList()
  } catch (e) {
    ElMessage.error(getErrorMessage(e) || '操作失败')
  }
}

onMounted(fetchList)
</script>

<style scoped>
.page { min-height: 60vh; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.title { margin: 0; font-size: 20px; }
.empty { text-align: center; color: #999; padding: 48px 0; }
.order-list { display: flex; flex-direction: column; gap: 12px; }
.order-card {
  background: #fff; border-radius: 12px; padding: 14px; cursor: pointer;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.order-head { display: flex; justify-content: space-between; margin-bottom: 8px; color: #666; font-size: 13px; }
.status { color: #8BC34A; font-weight: 600; }
.item-row { display: flex; gap: 10px; align-items: center; padding: 6px 0; }
.thumb { width: 48px; height: 48px; border-radius: 6px; object-fit: cover; }
.item-info { flex: 1; }
.muted { color: #999; font-size: 12px; }
.money { color: #e65100; font-weight: 600; }
.order-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; padding-top: 8px; border-top: 1px solid #f0f0f0; }
.actions { display: flex; gap: 8px; }
.btn {
  border: none; background: #8BC34A; color: #fff; padding: 6px 12px; border-radius: 16px; cursor: pointer; font-size: 13px;
}
.btn.ghost { background: #fff; color: #8BC34A; border: 1px solid #8BC34A; }
.pager { display: flex; justify-content: center; margin-top: 16px; }
</style>
