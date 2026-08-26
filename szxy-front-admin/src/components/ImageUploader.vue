<template>
  <div class="image-uploader">
    <el-upload
      v-model:file-list="fileList"
      list-type="picture-card"
      accept="image/*"
      :disabled="disabled || fileList.length >= maxCount"
      :limit="maxCount"
      :http-request="doUpload"
      :before-upload="beforeUpload"
      :on-preview="onPreview"
      :on-remove="onRemove"
      :on-exceed="onExceed"
    >
      <el-icon><Plus /></el-icon>
    </el-upload>
    <p v-if="!disabled" class="tip">支持 JPG / PNG / GIF / WebP，单张不超过 {{ maxMb }}MB，最多 {{ maxCount }} 张</p>
    <el-dialog v-model="previewVisible" title="预览" width="520px" append-to-body>
      <img v-if="previewUrl" :src="previewUrl" class="preview-img" alt="preview" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { Plus } from '@element-plus/icons-vue';
import { ElMessage, type UploadFile, type UploadRawFile, type UploadRequestOptions, type UploadUserFile } from 'element-plus';
import { uploadApi } from '../api';

const props = withDefaults(
  defineProps<{
    modelValue?: string;
    disabled?: boolean;
    maxCount?: number;
    maxMb?: number;
  }>(),
  {
    modelValue: '',
    disabled: false,
    maxCount: 5,
    maxMb: 5
  }
);

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();

const fileList = ref<UploadUserFile[]>([]);
const previewVisible = ref(false);
const previewUrl = ref('');
let syncingFromModel = false;

function parseUploadUrl(res: unknown): string {
  if (typeof res === 'string') return res.trim();
  if (res && typeof res === 'object') {
    const o = res as Record<string, unknown>;
    const u = o.url ?? o.data;
    if (typeof u === 'string') return u.trim();
  }
  return '';
}

function urlsFromModel(value: string): string[] {
  return value
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);
}

function emitFromFileList() {
  const urls = fileList.value
    .map((f) => (typeof f.url === 'string' ? f.url : ''))
    .filter(Boolean);
  emit('update:modelValue', urls.join(','));
}

watch(
  () => props.modelValue,
  (val) => {
    const urls = urlsFromModel(val || '');
    const current = fileList.value.map((f) => f.url).filter(Boolean);
    if (urls.join(',') === current.join(',')) return;
    syncingFromModel = true;
    fileList.value = urls.map((url, i) => ({
      name: `image-${i + 1}`,
      url,
      status: 'success' as const,
      uid: Date.now() + i
    }));
    syncingFromModel = false;
  },
  { immediate: true }
);

function beforeUpload(file: UploadRawFile) {
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件');
    return false;
  }
  const maxBytes = props.maxMb * 1024 * 1024;
  if (file.size > maxBytes) {
    ElMessage.warning(`图片请小于 ${props.maxMb}MB`);
    return false;
  }
  return true;
}

async function doUpload(options: UploadRequestOptions) {
  const file = options.file;
  try {
    const fd = new FormData();
    fd.append('file', file);
    const raw = await uploadApi.uploadFile(fd);
    const url = parseUploadUrl(raw);
    if (!url) {
      ElMessage.error('上传成功但未返回图片地址');
      options.onError?.(new Error('empty url') as never);
      return;
    }
    const target = fileList.value.find((f) => f.uid === file.uid);
    if (target) {
      target.url = url;
      target.status = 'success';
      target.name = file.name;
    } else {
      fileList.value.push({
        name: file.name,
        url,
        status: 'success',
        uid: file.uid
      });
    }
    if (!syncingFromModel) emitFromFileList();
    options.onSuccess?.(url as never);
    ElMessage.success('上传成功');
  } catch (e) {
    console.error(e);
    ElMessage.error('上传失败，请重试');
    options.onError?.(e as never);
  }
}

function onRemove(_file: UploadFile, files: UploadFile[]) {
  if (syncingFromModel) return;
  const urls = files.map((f) => (typeof f.url === 'string' ? f.url : '')).filter(Boolean);
  emit('update:modelValue', urls.join(','));
}

function onPreview(file: UploadFile) {
  previewUrl.value = file.url || '';
  previewVisible.value = true;
}

function onExceed() {
  ElMessage.warning(`最多上传 ${props.maxCount} 张图片`);
}
</script>

<style scoped>
.image-uploader {
  width: 100%;
}

.tip {
  margin: 8px 0 0;
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

.preview-img {
  width: 100%;
  display: block;
}
</style>
