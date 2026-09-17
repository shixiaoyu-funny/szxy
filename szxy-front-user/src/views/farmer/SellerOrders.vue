<template>
  <div class="page">
    <PageLoadingOverlay :visible="loading" />
    <div v-show="!loading" class="page-body">
      <div class="toolbar">
        <h2 class="title">卖家订单</h2>
        <el-select v-model="statusFilter" clearable placeholder="全部状态" style="width: 140px" @change="reload">
          <el-option v-for="(label, code) in ORDER_STATUS_TEXT" :key="code" :label="label" :value="Number(code)" />
        </el-select>
      </div>

      <div v-if="orders.length === 0" class="empty">暂无相关订单</div>
      <div v-else class="order-list">
        <div v-for="o in orders" :key="o.id" class="order-card">
          <div class="order-head">
            <span>{{ o.orderNo }}</span>
            <span class="status">{{ orderStatusText(o.status) }}</span>
          </div>
          <div v-for="it in o.items || []" :key="it.id" class="item-row">
            <div class="item-info">
              <div>{{ it.itemName }} ×{{ it.quantity }}</div>
              <div class="muted">{{ formatMoney(it.subtotal) }}</div>
            </div>
          </div>
          <div class="actions">
            <button v-if="o.status === 4" type="button" class="btn" @click="onShip(o.id)">发货</button>
            <template v-if="o.status === 9">
              <button type="button" class="btn" @click="onRefund(o.id, 1)">同意退款</button>
              <button type="button" class="btn ghost" @click="onRefund(o.id, 2)">驳回</button>
            </template>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import PageLoadingOverlay from '../components/PageLoadingOverlay.vue'
import { listSellerOrders, sellerHandleRefund, sellerShip } from '../api/order'
import { getErrorMessage } from '../api/axios'
import { ORDER_STATUS_TEXT, formatMoney, orderStatusText } from '../utils/order'

const loading = ref(true)
const orders = ref<any[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = 10
const statusFilter = ref<number | undefined>()

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listSellerOrders(pageNo.value, pageSize, statusFilter.value)
    orders.value = res.data?.data || []
    total.value = Number(res.data?.total || 0)
  } catch (e) {
    ElMessage.error(getErrorMessage(e) || '加载失败')
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

const onShip = async (id: number) => {
  try {
    await sellerShip(id)
    ElMessage.success('已发货')
    await fetchList()
  } catch (e) {
    ElMessage.error(getErrorMessage(e) || '发货失败')
  }
}

const onRefund = async (id: number, handle: number) => {
  try {
    let rejectReason: string | undefined
    if (handle === 2) {
      const { value } = await ElMessageBox.prompt('驳回原因（可选）', '驳回退款', {
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      })
      rejectReason = value
    } else {
      await ElMessageBox.confirm('同意后将退款入买家余额并回补库存', '同意退款', { type: 'warning' })
    }
    await sellerHandleRefund(id, handle, rejectReason)
    ElMessage.success(handle === 1 ? '已同意退款' : '已驳回')
    await fetchList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(getErrorMessage(e) || '处理失败')
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
  background: #fff; border-radius: 12px; padding: 14px; box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.order-head { display: flex; justify-content: space-between; margin-bottom: 8px; color: #666; font-size: 13px; }
.status { color: #8BC34A; font-weight: 600; }
.item-row { padding: 4px 0; }
.muted { color: #999; font-size: 12px; }
.actions { display: flex; gap: 8px; margin-top: 10px; }
.btn {
  border: none; background: #8BC34A; color: #fff; padding: 6px 12px; border-radius: 16px; cursor: pointer;
}
.btn.ghost { background: #fff; color: #8BC34A; border: 1px solid #8BC34A; }
.pager { display: flex; justify-content: center; margin-top: 16px; }
</style>
