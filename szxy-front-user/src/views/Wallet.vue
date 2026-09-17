<template>
  <div class="page">
    <PageLoadingOverlay :visible="loading" />
    <div v-show="!loading" class="page-body">
      <div class="balance-card">
        <p class="label">账户余额</p>
        <p class="amount">{{ formatMoney(balance) }}</p>
        <div class="recharge-row">
          <el-input-number v-model="rechargeAmount" :min="1" :max="99999" :precision="2" />
          <button type="button" class="btn" :disabled="recharging" @click="onRecharge">模拟充值</button>
        </div>
      </div>

      <div class="tabs">
        <button type="button" :class="{ active: tab === 'recharge' }" @click="tab = 'recharge'">充值记录</button>
        <button type="button" :class="{ active: tab === 'consume' }" @click="tab = 'consume'">消费/退款</button>
      </div>

      <div class="record-list">
        <div v-if="currentRecords.length === 0" class="empty">暂无流水</div>
        <div v-for="r in currentRecords" :key="r.id" class="record-item">
          <div>
            <div class="rt">{{ typeText(r.type) }}</div>
            <div class="muted">{{ r.createTime }} · {{ r.remark || '' }}</div>
          </div>
          <div class="rm">
            <div :class="r.type === 2 ? 'out' : 'in'">
              {{ r.type === 2 ? '-' : '+' }}{{ formatMoney(r.amount).replace('¥', '') }}
            </div>
            <div class="muted">余额 {{ formatMoney(r.balanceAfter) }}</div>
          </div>
        </div>
      </div>

      <div class="pager">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="currentTotal"
          :page-size="pageSize"
          :current-page="pageNo"
          @current-change="onPage"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageLoadingOverlay from '../components/PageLoadingOverlay.vue'
import { getBalance, getWalletRecords, recharge } from '../api/wallet'
import { getErrorMessage } from '../api/axios'
import { formatMoney } from '../utils/order'

const loading = ref(true)
const recharging = ref(false)
const balance = ref(0)
const rechargeAmount = ref(100)
const tab = ref<'recharge' | 'consume'>('recharge')
const pageNo = ref(1)
const pageSize = 10
const rechargePage = ref<{ total: number; data: any[] }>({ total: 0, data: [] })
const consumePage = ref<{ total: number; data: any[] }>({ total: 0, data: [] })

const currentRecords = computed(() =>
  tab.value === 'recharge' ? rechargePage.value.data : consumePage.value.data
)
const currentTotal = computed(() =>
  tab.value === 'recharge' ? Number(rechargePage.value.total || 0) : Number(consumePage.value.total || 0)
)

const typeText = (t: number) => ({ 1: '充值', 2: '支付', 3: '退款' }[t] || '流水')

const fetchAll = async () => {
  loading.value = true
  try {
    const [b, rec] = await Promise.all([getBalance(), getWalletRecords(pageNo.value, pageSize)])
    balance.value = Number(b.data || 0)
    rechargePage.value = rec.data?.recharge || { total: 0, data: [] }
    consumePage.value = rec.data?.consume || { total: 0, data: [] }
  } catch (e) {
    ElMessage.error(getErrorMessage(e) || '加载钱包失败')
  } finally {
    loading.value = false
  }
}

const onPage = (p: number) => {
  pageNo.value = p
  fetchAll()
}

const onRecharge = async () => {
  if (!rechargeAmount.value || rechargeAmount.value <= 0) {
    ElMessage.warning('请输入充值金额')
    return
  }
  recharging.value = true
  try {
    await recharge(rechargeAmount.value)
    ElMessage.success('充值成功')
    await fetchAll()
  } catch (e) {
    ElMessage.error(getErrorMessage(e) || '充值失败')
  } finally {
    recharging.value = false
  }
}

onMounted(fetchAll)
</script>

<style scoped>
.page { min-height: 60vh; }
.balance-card {
  background: linear-gradient(135deg, #8BC34A, #4CAF50); color: #fff;
  border-radius: 14px; padding: 20px; margin-bottom: 16px;
}
.label { margin: 0; opacity: 0.9; }
.amount { margin: 8px 0 16px; font-size: 32px; font-weight: 700; }
.recharge-row { display: flex; gap: 10px; align-items: center; }
.btn {
  border: none; background: #fff; color: #4CAF50; padding: 8px 16px; border-radius: 20px;
  cursor: pointer; font-weight: 600;
}
.btn:disabled { opacity: 0.6; }
.tabs { display: flex; gap: 8px; margin-bottom: 12px; }
.tabs button {
  border: 1px solid #e0e0e0; background: #fff; border-radius: 16px; padding: 6px 14px; cursor: pointer;
}
.tabs button.active { border-color: #8BC34A; color: #8BC34A; background: #f1f8e9; }
.record-list { background: #fff; border-radius: 12px; padding: 8px 14px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.record-item {
  display: flex; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #f0f0f0;
}
.record-item:last-child { border-bottom: none; }
.rt { font-weight: 500; }
.muted { color: #999; font-size: 12px; margin-top: 4px; }
.rm { text-align: right; }
.in { color: #2e7d32; font-weight: 600; }
.out { color: #e65100; font-weight: 600; }
.empty { text-align: center; color: #999; padding: 24px; }
.pager { display: flex; justify-content: center; margin-top: 16px; }
</style>
