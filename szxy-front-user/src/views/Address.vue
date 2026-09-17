<template>
  <div class="page">
    <PageLoadingOverlay :visible="loading" />
    <div v-show="!loading" class="page-body">
      <div class="toolbar">
        <h2 class="title">收货地址</h2>
        <button type="button" class="btn-primary" @click="openForm()">新增地址</button>
      </div>

      <div v-if="list.length === 0" class="empty">暂无地址，请新增</div>
      <div v-else class="addr-list">
        <div v-for="item in list" :key="item.id" class="addr-card">
          <div class="addr-main">
            <div class="addr-top">
              <strong>{{ item.receiverName }}</strong>
              <span class="phone">{{ item.phone }}</span>
              <span v-if="item.isDefault === 1" class="tag-default">默认</span>
            </div>
            <p class="addr-text">{{ item.province }}{{ item.city }}{{ item.county }}{{ item.detail }}</p>
          </div>
          <div class="addr-actions">
            <button type="button" class="link" @click="openForm(item)">编辑</button>
            <button
              v-if="item.isDefault !== 1"
              type="button"
              class="link"
              @click="onSetDefault(item.id)"
            >设为默认</button>
            <button
              v-if="item.isDefault !== 1"
              type="button"
              class="link danger"
              @click="onDel(item.id)"
            >删除</button>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑地址' : '新增地址'" width="480px" destroy-on-close>
      <el-form label-width="88px">
        <el-form-item label="收件人">
          <el-input v-model="form.receiverName" maxlength="32" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" maxlength="20" />
        </el-form-item>
        <el-form-item label="省">
          <el-input v-model="form.province" maxlength="32" />
        </el-form-item>
        <el-form-item label="市">
          <el-input v-model="form.city" maxlength="32" />
        </el-form-item>
        <el-form-item label="区/县">
          <el-input v-model="form.county" maxlength="32" />
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input v-model="form.detail" type="textarea" :rows="2" maxlength="200" />
        </el-form-item>
        <el-form-item label="默认地址">
          <el-checkbox v-model="form.isDefaultChecked">设为默认</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageLoadingOverlay from '../components/PageLoadingOverlay.vue'
import { addAddress, alterAddress, delAddress, listAddress, setDefaultAddress } from '../api/address'
import { getErrorMessage } from '../api/axios'

interface AddressItem {
  id: number
  receiverName: string
  phone: string
  province: string
  city: string
  county: string
  detail: string
  isDefault: number
}

const loading = ref(true)
const list = ref<AddressItem[]>([])
const dialogVisible = ref(false)
const saving = ref(false)

const form = reactive({
  id: 0 as number,
  receiverName: '',
  phone: '',
  province: '',
  city: '',
  county: '',
  detail: '',
  isDefaultChecked: false
})

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listAddress()
    list.value = (res.data || []) as AddressItem[]
  } catch (e) {
    ElMessage.error(getErrorMessage(e) || '加载地址失败')
  } finally {
    loading.value = false
  }
}

const openForm = (item?: AddressItem) => {
  if (item) {
    form.id = item.id
    form.receiverName = item.receiverName
    form.phone = item.phone
    form.province = item.province
    form.city = item.city
    form.county = item.county
    form.detail = item.detail
    form.isDefaultChecked = item.isDefault === 1
  } else {
    form.id = 0
    form.receiverName = ''
    form.phone = ''
    form.province = ''
    form.city = ''
    form.county = ''
    form.detail = ''
    form.isDefaultChecked = list.value.length === 0
  }
  dialogVisible.value = true
}

const onSave = async () => {
  if (!form.receiverName.trim() || !form.phone.trim()) {
    ElMessage.warning('请填写收件人与手机号')
    return
  }
  if (!form.province.trim() || !form.city.trim() || !form.county.trim() || !form.detail.trim()) {
    ElMessage.warning('请填写完整省市区与详细地址')
    return
  }
  saving.value = true
  try {
    const payload = {
      receiverName: form.receiverName.trim(),
      phone: form.phone.trim(),
      province: form.province.trim(),
      city: form.city.trim(),
      county: form.county.trim(),
      detail: form.detail.trim(),
      isDefault: form.isDefaultChecked ? 1 : 0
    }
    if (form.id) {
      await alterAddress({ id: form.id, ...payload })
    } else {
      await addAddress(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await fetchList()
  } catch (e) {
    ElMessage.error(getErrorMessage(e) || '保存失败')
  } finally {
    saving.value = false
  }
}

const onSetDefault = async (id: number) => {
  try {
    await setDefaultAddress(id)
    ElMessage.success('已设为默认')
    await fetchList()
  } catch (e) {
    ElMessage.error(getErrorMessage(e) || '设置失败')
  }
}

const onDel = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定删除该地址？', '提示', { type: 'warning' })
    await delAddress(id)
    ElMessage.success('已删除')
    await fetchList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(getErrorMessage(e) || '删除失败')
  }
}

onMounted(fetchList)
</script>

<style scoped>
.page { min-height: 60vh; }
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.title { margin: 0; font-size: 20px; color: #333; }
.btn-primary {
  border: none; background: #8BC34A; color: #fff; padding: 8px 16px; border-radius: 20px; cursor: pointer;
}
.btn-primary:hover { background: #7CB342; }
.empty { text-align: center; color: #999; padding: 48px 0; }
.addr-list { display: flex; flex-direction: column; gap: 12px; }
.addr-card {
  background: #fff; border-radius: 12px; padding: 16px; border-left: 4px solid #8BC34A;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06); display: flex; justify-content: space-between; gap: 12px;
}
.addr-top { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.phone { color: #666; }
.tag-default {
  font-size: 12px; color: #8BC34A; border: 1px solid #8BC34A; border-radius: 10px; padding: 0 8px;
}
.addr-text { margin: 0; color: #555; line-height: 1.5; }
.addr-actions { display: flex; flex-direction: column; gap: 6px; flex-shrink: 0; }
.link { border: none; background: none; color: #8BC34A; cursor: pointer; padding: 0; text-align: right; }
.link.danger { color: #ef5350; }
</style>
