<template>
  <div class="ai-multimodal-container">
    <header class="ai-header">
      <h1 class="ai-title">农产品识别</h1>
      <Location />
    </header>

    <div class="multimodal-content">
      <div class="upload-section">
        <div class="upload-container">
          <input type="file" ref="fileInput" accept="image/*" @change="handleFileUpload" class="file-input" />
          <div v-if="!localPreviewUrl && !imageUrl" class="upload-placeholder">
            <div class="upload-icon">📷</div>
            <p>点击上传图片</p>
          </div>
          <div v-else class="image-preview">
            <img :src="localPreviewUrl || imageUrl" alt="预览" class="preview-image" />
            <button @click="removeImage" class="remove-btn">×</button>
          </div>
        </div>
        <button @click="startRecognition" class="recognize-btn" :disabled="!imageUrl || loading">
          {{ loading ? '识别中...' : '开始识别' }}
        </button>
      </div>

      <div v-if="recognitionResult" class="recognition-result">
        <div class="result-header">
          <h3>识别结果</h3>
        </div>
        <div class="result-content">
          <p v-for="(line, index) in streamRecognitionResult.split('\n')" :key="index" class="content-line">
            {{ line }}
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { uploadFile } from '../../api/upload';
import { aiMultimodal } from '../../api/ai';
import Location from '../../components/Location.vue';

const fileInput = ref<HTMLInputElement | null>(null);
const imageUrl = ref('');
const localPreviewUrl = ref('');
const recognitionResult = ref('');
const streamRecognitionResult = ref('');
const loading = ref(false);

// 🔥 修复：简化上传逻辑，正确提取图片URL，拼接完整地址
const handleFileUpload = async (event: Event) => {
  event.preventDefault();
  event.stopPropagation();
  const target = event.target as HTMLInputElement;
  if (!target.files?.length) return;

  const file = target.files[0];

  // ✅ 本地预览（和头像组件一致，轻量稳定）
  if (file) {
    localPreviewUrl.value = URL.createObjectURL(file);
  }

  loading.value = true;
  try {
    if (!file) {
      throw new Error('文件对象不存在');
    }
    const res = await uploadFile(file);
    console.log('上传返回原始数据:', res); // 排查用

    // ✅ 正确提取图片URL（核心修复！不转JSON）
    let realImageUrl = '';
    if (typeof res === 'string') {
      realImageUrl = res;
    } else if (res) {
      // 兼容后端返回：res.url 或 res.data
      realImageUrl = (res as any).url || (res as any).data || '';
    }

    // ✅ 拼接完整域名（核心修复！AI接口必须要完整URL）
    if (realImageUrl && !realImageUrl.startsWith('http')) {
      realImageUrl = `${window.location.origin}/${realImageUrl}`;
    }

    if (!realImageUrl) {
      throw new Error('未获取到有效图片地址');
    }

    imageUrl.value = realImageUrl;
    console.log('最终有效图片URL:', imageUrl.value);
    ElMessage.success('图片上传成功！');

    // 清空历史结果
    streamRecognitionResult.value = '';
    recognitionResult.value = '';

  } catch (error) {
    console.error('上传失败:', error);
    ElMessage.error('上传失败：' + (error as Error).message);
    localPreviewUrl.value = '';
  } finally {
    loading.value = false;
    target.value = '';
  }
};

const removeImage = () => {
  imageUrl.value = '';
  localPreviewUrl.value = '';
  streamRecognitionResult.value = '';
  recognitionResult.value = '';
  if (fileInput.value) {
    fileInput.value.value = '';
  }
};

// 文字流效果
const streamTextRecognition = (text: string) => {
  streamRecognitionResult.value = '';
  let index = 0;
  const interval = setInterval(() => {
    if (index < text.length) {
      streamRecognitionResult.value += text[index++];
    } else {
      clearInterval(interval);
    }
  }, 30);
};

// 🔥 修复：识别函数加打印，错误清晰提示
const startRecognition = async () => {
  if (!imageUrl.value) {
    ElMessage.warning('请先上传图片');
    return;
  }

  console.log('开始识别，传递给AI的URL:', imageUrl.value); // 排查关键
  loading.value = true;

  try {
    const res = await aiMultimodal(imageUrl.value);
    console.log('识别返回:', res);

    if (!res?.data) {
      throw new Error('识别结果为空');
    }

    recognitionResult.value = res.data;
    streamTextRecognition(res.data);
    ElMessage.success('识别完成！');
  } catch (error) {
    console.error('识别失败详情:', error);
    ElMessage.error('识别失败：' + (error as Error).message);
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
/* 样式完全不变，直接保留你原来的 */
.ai-multimodal-container {
  min-height: 100vh;
  background: transparent;
  padding: 16px;
  margin: 5px;
}

.ai-header {
  background: linear-gradient(-45deg, #11998e, #38ef7d, #11998e, #38ef7d);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ai-title {
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
  margin: 0;
}

.multimodal-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.upload-section {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.upload-container {
  position: relative;
  width: 100%;
  height: 200px;
  border: 2px dashed #e0e0e0;
  border-radius: 8px;
  margin-bottom: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
}

.upload-container:hover {
  border-color: #8BC34A;
}

.file-input {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
  z-index: 2;
}

.upload-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 1;
}

.upload-icon {
  font-size: 48px;
  margin-bottom: 12px;
  color: #e0e0e0;
}

.upload-placeholder p {
  font-size: 14px;
  color: #999;
  margin: 0;
}

.image-preview {
  position: relative;
  width: 100%;
  height: 100%;
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border: none;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.3s ease;
}

.remove-btn:hover {
  background: rgba(0, 0, 0, 0.8);
}

.recognize-btn {
  width: 100%;
  padding: 12px;
  background: #8BC34A;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.3s ease;
}

.recognize-btn:hover:not(:disabled) {
  background: #7CB342;
}

.recognize-btn:disabled {
  background: #e0e0e0;
  cursor: not-allowed;
}

.recognition-result {
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.result-header {
  margin-bottom: 16px;
}

.result-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.result-content {
  line-height: 1.6;
  color: #666;
}

.content-line {
  margin: 8px 0;
}

@media (max-width: 480px) {
  .ai-multimodal-container {
    padding: 12px;
  }

  .upload-container {
    height: 160px;
  }

  .upload-icon {
    font-size: 36px;
  }
}
</style>