<template>
  <div class="page">
    <PageLoadingOverlay :visible="loading" />

    <div v-if="!loading && loadError" class="state-box">
      <p>{{ loadError }}</p>
      <button type="button" class="btn-primary" @click="load">重试</button>
      <button type="button" class="link" @click="router.push('/cart')">返回购物车</button>
    </div>

    <!-- 必须用 v-if：v-show 在 preview 为空时仍会渲染子树，访问 preview.xxx 会白屏且不发请求 -->
    <div v-if="!loading && preview" class="page-body">
      <h2 class="title">确认订单</h2>

      <section v-if="preview.needAddress" class="block">
        <div class="block-head">
          <h3>收货地址</h3>
          <button type="button" class="link" @click="router.push('/address')">管理地址</button>
        </div>
        <div v-if="addresses.length === 0" class="hint">
          暂无地址，请先
          <button type="button" class="link" @click="router.push('/address')">新增</button>
        </div>
        <el-radio-group v-else v-model="addressId" class="addr-group">
          <el-radio
            v-for="a in addresses"
            :key="a.id"
            :value="a.id"
            class="addr-radio"
          >
            <div>
              <strong>{{ a.receiverName }}</strong> {{ a.phone }}
              <span v-if="a.isDefault === 1" class="tag">默认</span>
              <p>{{ a.province }}{{ a.city }}{{ a.county }}{{ a.detail }}</p>
            </div>
          </el-radio>
        </el-radio-group>
      </section>

      <section class="block">
        <h3>商品明细</h3>
        <div v-if="!preview.items?.length" class="hint">暂无待结算商品</div>
        <div v-for="(it, idx) in preview.items || []" :key="idx" class="line">
          <img :src="it.productImage || placeholder" class="thumb" alt="" />
          <div class="line-info">
            <div class="name">{{ it.productName }}</div>
            <div class="sub">×{{ it.quantity }} · {{ formatMoney(it.unitPrice) }}</div>
          </div>
          <div class="subtotal">{{ formatMoney(it.subtotal) }}</div>
        </div>
      </section>

      <section class="block">
        <el-input v-model="remark" type="textarea" :rows="2" maxlength="200" placeholder="买家备注（可选）" />
      </section>

      <section class="block summary">
        <p>履约方式：{{ FULFILLMENT_TEXT[preview.fulfillmentType as number] || '-' }}</p>
        <p>应付总额：<strong class="money">{{ formatMoney(preview.totalAmount) }}</strong></p>
        <p>当前余额：{{ formatMoney(preview.balance) }}</p>
        <p v-if="Number(preview.shortage) > 0" class="warn">
          余额不足，还差 {{ formatMoney(preview.shortage) }}
          <button type="button" class="link" @click="router.push('/wallet')">去充值</button>
        </p>
      </section>

      <div class="footer-bar">
        <button
          type="button"
          class="btn-primary"
          :disabled="submitting || !preview.items?.length"
          @click="onSubmit"
        >
          {{ submitting ? '提交中…' : '提交订单' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageLoadingOverlay from '../components/PageLoadingOverlay.vue'
import { listAddress } from '../api/address'
import { createOrder, previewOrder, type OrderSubmitBody } from '../api/order'
import { payOrder } from '../api/wallet'
import { getErrorMessage } from '../api/axios'
import { FULFILLMENT_TEXT, formatMoney } from '../utils/order'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const placeholder = 'https://via.placeholder.com/64?text=商品'

const loading = ref(true)
const loadError = ref('')
const submitting = ref(false)
const preview = ref<any>(null)
const addresses = ref<any[]>([])
const addressId = ref<number | undefined>()
const remark = ref('')

const submitBody = computed<OrderSubmitBody>(() => {
  const fromCart = route.query.fromCart === '1'
  if (fromCart) {
    return {
      fromCart: true,
      addressId: addressId.value ?? null,
      remark: remark.value || undefined
    }
  }
  const productId = Number(route.query.productId)
  return {
    productId: Number.isFinite(productId) ? productId : undefined,
    quantity: Number(route.query.quantity || 1) || 1,
    addressId: addressId.value ?? null,
    remark: remark.value || undefined
  }
})

const load = async () => {
  if (!userStore.isLoggedIn) {
    loading.value = false
    router.push('/login')
    return
  }
  const fromCart = route.query.fromCart === '1'
  const productId = Number(route.query.productId)
  if (!fromCart && !Number.isFinite(productId)) {
    loading.value = false
    loadError.value = '缺少商品信息，请从购物车或商品页重新进入'
    preview.value = null
    return
  }

  loading.value = true
  loadError.value = ''
  preview.value = null
  try {
    const body: OrderSubmitBody = { ...submitBody.value }
    if (!body.addressId) delete body.addressId
    const [pv, addrRes] = await Promise.all([previewOrder(body), listAddress()])
    preview.value = pv.data
    addresses.value = addrRes.data || []
    const def = addresses.value.find((a: any) => a.isDefault === 1)
    addressId.value = def?.id ?? addresses.value[0]?.id
  } catch (e) {
    loadError.value = getErrorMessage(e) || '加载确认页失败'
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
}

const onSubmit = async () => {
  if (preview.value?.needAddress && !addressId.value) {
    ElMessage.warning('请选择收货地址')
    return
  }
  if (!preview.value?.items?.length) {
    ElMessage.warning('没有可结算的商品')
    return
  }
  submitting.value = true
  try {
    const res = await createOrder({
      ...submitBody.value,
      addressId: addressId.value ?? null,
      remark: remark.value || undefined
    })
    const order = res.data as { id: number; payAmount?: number }
    ElMessage.success('下单成功')
    if (Number(preview.value?.shortage || 0) <= 0) {
      try {
        await payOrder(order.id)
        ElMessage.success('支付成功')
      } catch (e) {
        ElMessage.warning(getErrorMessage(e) || '支付未完成，请在订单详情继续支付')
      }
    }
    router.replace(`/orders/${order.id}`)
  } catch (e) {
    ElMessage.error(getErrorMessage(e) || '下单失败')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
watch(
  () => [route.query.fromCart, route.query.productId, route.query.quantity],
  () => {
    load()
  }
)
</script>

<style scoped>
.page { min-height: 60vh; padding-bottom: 80px; }
.title { margin: 0 0 16px; font-size: 20px; }
.state-box {
  text-align: center; padding: 48px 16px; color: #666;
  display: flex; flex-direction: column; align-items: center; gap: 12px;
}
.block {
  background: #fff; border-radius: 12px; padding: 16px; margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.block-head { display: flex; justify-content: space-between; align-items: center; }
.block h3 { margin: 0 0 12px; font-size: 16px; }
.link { border: none; background: none; color: #8BC34A; cursor: pointer; padding: 0; }
.hint { color: #888; }
.addr-group { display: flex; flex-direction: column; align-items: stretch; gap: 8px; width: 100%; }
.addr-radio { height: auto; margin: 0; align-items: flex-start; white-space: normal; }
.addr-radio p { margin: 4px 0 0; color: #666; font-size: 13px; }
.tag { margin-left: 6px; font-size: 12px; color: #8BC34A; }
.line { display: flex; gap: 10px; align-items: center; padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
.line:last-child { border-bottom: none; }
.thumb { width: 56px; height: 56px; border-radius: 8px; object-fit: cover; }
.line-info { flex: 1; min-width: 0; }
.name { font-weight: 500; }
.sub { color: #888; font-size: 13px; margin-top: 4px; }
.subtotal { color: #e65100; font-weight: 600; }
.summary p { margin: 6px 0; }
.money { color: #e65100; font-size: 18px; }
.warn { color: #ef6c00; }
.footer-bar {
  position: sticky; bottom: 12px; display: flex; justify-content: flex-end;
}
.btn-primary {
  border: none; background: #8BC34A; color: #fff; padding: 12px 28px; border-radius: 24px;
  cursor: pointer; font-size: 15px;
}
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
