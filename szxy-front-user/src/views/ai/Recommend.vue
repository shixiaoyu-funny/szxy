<template>
  <div class="ai-recommend-container">
    <header class="ai-header">
      <h1 class="ai-title">智能推荐</h1>
      <Location />
    </header>
    <div class="recommend-content">
      <div class="input-section">
        <!-- 省份和城市 → 联动下拉选择器 -->
        <div class="form-row">
          <div class="form-item">
            <label>省份</label>
            <select v-model="formData.province" class="form-select" @change="handleProvinceChange">
              <option value="">请选择省份</option>
              <option v-for="province in provinceList" :key="province" :value="province">
                {{ province }}
              </option>
            </select>
          </div>
          <div class="form-item">
            <label>城市</label>
            <select v-model="formData.city" class="form-select">
              <option value="">请选择城市</option>
              <option v-for="city in cityList" :key="city" :value="city">
                {{ city }}
              </option>
            </select>
          </div>
        </div>
        <!-- 行程信息 -->
        <div class="form-row">
          <div class="form-item">
            <label>预计旅行时长（天）</label>
            <input v-model="formData.tripDays" type="text" placeholder="例如：1" class="form-input" />
          </div>
          <div class="form-item">
            <label>预计旅行人数</label>
            <select v-model="formData.tripPeople" class="form-select">
              <option value="">请选择</option>
              <option value="1">单人</option>
              <option value="2">双人</option>
              <option value="3">家庭</option>
              <option value="4">多人</option>
            </select>
          </div>
          <div class="form-item">
            <label>旅行喜好</label>
            <select v-model="formData.tripPrefer" class="form-select">
              <option value="">请选择</option>
              <option v-for="(prefer, index) in preferList" :key="index" :value="index">
                {{ prefer }}
              </option>
            </select>
          </div>
        </div>
        <!-- 预算 -->
        <div class="form-row">
          <div class="form-item full-width">
            <label>预计开销</label>
            <input v-model="formData.money" type="text" placeholder="请输入预算" class="form-input" />
          </div>
        </div>
        <!-- 用户输入内容 -->
        <div class="form-row">
          <div class="form-item full-width">
            <label>其他要求</label>
            <textarea v-model="formData.content" placeholder="请输入您的偏好，没有可填'无'..." class="recommend-textarea"></textarea>
          </div>
        </div>
        <button @click="getRecommendation" class="recommend-btn" :disabled="loading">
          {{ loading ? '生成中...（请在下方查看结果）' : '获取推荐' }}
        </button>
      </div>

      <!-- 结果展示区域 -->
      <div v-if="recommendationResult" class="recommendation-result">
        <!-- 流式推荐内容 -->
        <div class="result-section">
          <h3>村落相关特产及行程介绍</h3>
          <div class="result-container">
            <div class="result-content">{{ streamContent }}</div>
          </div>
        </div>
        <!-- 推荐乡村轮播 -->
        <div v-if="recommendationResult.recommendList && recommendationResult.recommendList.length > 0"
          class="result-section">
          <h3>推荐景点</h3>
          <div class="carousel-container">
            <div class="carousel-wrapper" ref="carouselWrapper">
              <div class="carousel" :style="carouselStyle" @mouseenter="stopAutoPlay" @mouseleave="startAutoPlay">
                <div v-for="(villageMap, index) in recommendationResult.recommendList" :key="index"
                  class="carousel-item" :class="{ active: index === currentIndex }" :style="getItemStyle(index)">
                  <div v-for="(scenicList, villageName) in villageMap" :key="villageName">
                    <h4 class="village-name">{{ villageName }}</h4>
                    <div class="scenic-list">
                      <div v-for="scenic in scenicList" :key="scenic.id" class="scenic-card">
                        <div class="scenic-image">
                          <img :src="scenic.image" :alt="scenic.name" />
                        </div>
                        <div class="scenic-info">
                          <h5>{{ scenic.name }}</h5>
                          <p class="scenic-intro">{{ scenic.intro }}</p>
                          <div class="scenic-meta">
                            <span class="meta-item">💰 ¥{{ scenic.price }}</span>
                            <span class="meta-item">❤️ {{ scenic.likes }}</span>
                            <span class="meta-item">⭐ {{ scenic.collections }}</span>
                          </div>
                          <div v-if="scenic.hasAccommodation" class="accommodation">
                            <span class="accommodation-tag">🏠 提供住宿</span>
                            <p class="accommodation-info">{{ scenic.accommodationInfo }}</p>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <!-- 轮播上下页按钮 -->
              <button class="carousel-btn prev" @click="prev">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M15 18l-6-6 6-6v12z" />
                </svg>
              </button>
              <button class="carousel-btn next" @click="next">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M9 6l6 6-6 6V6z" />
                </svg>
              </button>
            </div>
            <!-- 轮播指示器 -->
            <div class="carousel-indicators" v-if="recommendationResult.recommendList.length > 1">
              <span v-for="(villageMap, index) in recommendationResult.recommendList" :key="index" class="indicator"
                :class="{ active: index === currentIndex }" @click="goTo(index)"></span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { ElMessage } from 'element-plus';
import { aiRecommend } from '../../api/ai';
import Location from '@/components/Location.vue';



// 表单数据定义
interface AIDTO {
  province: string;
  city: string;
  tripDays: string;
  tripPeople: string;
  tripPrefer: string;
  money: string;
  content: string;
}
// 景点类型定义
interface Scenic {
  id: number;
  name: string;
  intro: string;
  image: string;
  price: number;
  likes: number;
  collections: number;
  hasAccommodation?: boolean;
  accommodationInfo?: string;
}
// 推荐结果类型定义
interface RecommendResult {
  content: string;
  recommendList: Record<string, Scenic[]>[];
}

// 核心响应式数据
const formData = ref<AIDTO>({
  province: '',
  city: '',
  tripDays: '',
  tripPeople: '',
  tripPrefer: '',
  money: '',
  content: ''
});
const loading = ref(false);
const recommendationResult = ref<RecommendResult | null>(null);
const streamContent = ref(''); // 流式输出的推荐内容

// 省份列表（对齐AI.vue）
const provinceList = ref([
  '北京市', '天津市', '河北省', '山西省', '内蒙古自治区',
  '辽宁省', '吉林省', '黑龙江省', '上海市', '江苏省',
  '浙江省', '安徽省', '福建省', '江西省', '山东省',
  '河南省', '湖北省', '湖南省', '广东省', '广西壮族自治区',
  '海南省', '重庆市', '四川省', '贵州省', '云南省',
  '西藏自治区', '陕西省', '甘肃省', '青海省', '宁夏回族自治区',
  '新疆维吾尔自治区', '香港特别行政区', '澳门特别行政区','台湾省'
]);
// 旅行喜好列表（对齐AI.vue，替换原数字值为文字）
const preferList = ref([
  '休闲', '打卡', '亲子', '康养', '民俗', '徒步', '美食', '研学', '露营', '水乡'
]);

// 省份-城市映射数据（对齐AI.vue）
interface CityDataType {
  [key: string]: string[];
}
const cityData = ref<CityDataType>({
  '北京市': ['北京市'],
  '天津市': ['天津市'],
  '河北省': ['石家庄市', '唐山市', '秦皇岛市', '邯郸市', '邢台市', '保定市', '张家口市', '承德市', '沧州市', '廊坊市', '衡水市'],
  '山西省': ['太原市', '大同市', '阳泉市', '长治市', '晋城市', '朔州市', '晋中市', '运城市', '忻州市', '临汾市', '吕梁市'],
  '内蒙古自治区': ['呼和浩特市', '包头市', '乌海市', '赤峰市', '通辽市', '鄂尔多斯市', '呼伦贝尔市', '巴彦淖尔市', '乌兰察布市', '兴安盟', '锡林郭勒盟', '阿拉善盟'],
  '辽宁省': ['沈阳市', '大连市', '鞍山市', '抚顺市', '本溪市', '丹东市', '锦州市', '营口市', '阜新市', '辽阳市', '盘锦市', '铁岭市', '朝阳市', '葫芦岛市'],
  '吉林省': ['长春市', '吉林市', '四平市', '辽源市', '通化市', '白山市', '松原市', '白城市', '延边朝鲜族自治州'],
  '黑龙江省': ['哈尔滨市', '齐齐哈尔市', '鸡西市', '鹤岗市', '双鸭山市', '大庆市', '伊春市', '佳木斯市', '七台河市', '牡丹江市', '黑河市', '绥化市', '大兴安岭地区'],
  '上海市': ['上海市'],
  '江苏省': ['南京市', '无锡市', '徐州市', '常州市', '苏州市', '南通市', '连云港市', '淮安市', '盐城市', '扬州市', '镇江市', '泰州市', '宿迁市'],
  '浙江省': ['杭州市', '宁波市', '温州市', '嘉兴市', '湖州市', '绍兴市', '金华市', '衢州市', '舟山市', '台州市', '丽水市'],
  '安徽省': ['合肥市', '芜湖市', '蚌埠市', '淮南市', '马鞍山市', '淮北市', '铜陵市', '安庆市', '黄山市', '滁州市', '阜阳市', '宿州市', '六安市', '亳州市', '池州市', '宣城市'],
  '福建省': ['福州市', '厦门市', '莆田市', '三明市', '泉州市', '漳州市', '南平市', '龙岩市', '宁德市'],
  '江西省': ['南昌市', '景德镇市', '萍乡市', '九江市', '新余市', '鹰潭市', '赣州市', '吉安市', '宜春市', '抚州市', '上饶市'],
  '山东省': ['济南市', '青岛市', '淄博市', '枣庄市', '东营市', '烟台市', '潍坊市', '济宁市', '泰安市', '威海市', '日照市', '临沂市', '德州市', '聊城市', '滨州市', '菏泽市'],
  '河南省': ['郑州市', '开封市', '洛阳市', '平顶山市', '安阳市', '鹤壁市', '新乡市', '焦作市', '濮阳市', '许昌市', '漯河市', '三门峡市', '南阳市', '商丘市', '信阳市', '周口市', '驻马店市'],
  '湖北省': ['武汉市', '黄石市', '十堰市', '宜昌市', '襄阳市', '鄂州市', '荆门市', '孝感市', '荆州市', '黄冈市', '咸宁市', '随州市', '恩施土家族苗族自治州'],
  '湖南省': ['长沙市', '株洲市', '湘潭市', '衡阳市', '邵阳市', '岳阳市', '常德市', '张家界市', '益阳市', '郴州市', '永州市', '怀化市', '娄底市', '湘西土家族苗族自治州'],
  '广东省': ['广州市', '韶关市', '深圳市', '珠海市', '汕头市', '佛山市', '江门市', '湛江市', '茂名市', '肇庆市', '惠州市', '梅州市', '汕尾市', '河源市', '阳江市', '清远市', '东莞市', '中山市', '潮州市', '揭阳市', '云浮市'],
  '广西壮族自治区': ['南宁市', '柳州市', '桂林市', '梧州市', '北海市', '防城港市', '钦州市', '贵港市', '玉林市', '百色市', '贺州市', '河池市', '来宾市', '崇左市'],
  '海南省': ['海口市', '三亚市', '三沙市', '儋州市'],
  '重庆市': ['重庆市'],
  '四川省': ['成都市', '自贡市', '攀枝花市', '泸州市', '德阳市', '绵阳市', '广元市', '遂宁市', '内江市', '乐山市', '南充市', '眉山市', '宜宾市', '广安市', '达州市', '雅安市', '巴中市', '资阳市', '阿坝藏族羌族自治州', '甘孜藏族自治州', '凉山彝族自治州'],
  '贵州省': ['贵阳市', '六盘水市', '遵义市', '安顺市', '毕节市', '铜仁市', '黔西南布依族苗族自治州', '黔东南苗族侗族自治州', '黔南布依族苗族自治州'],
  '云南省': ['昆明市', '曲靖市', '玉溪市', '保山市', '昭通市', '丽江市', '普洱市', '临沧市', '楚雄彝族自治州', '红河哈尼族彝族自治州', '文山壮族苗族自治州', '西双版纳傣族自治州', '大理白族自治州', '德宏傣族景颇族自治州', '怒江傈僳族自治州', '迪庆藏族自治州'],
  '西藏自治区': ['拉萨市', '日喀则市', '昌都市', '林芝市', '山南市', '那曲市', '阿里地区'],
  '陕西省': ['西安市', '铜川市', '宝鸡市', '咸阳市', '渭南市', '延安市', '汉中市', '榆林市', '安康市', '商洛市'],
  '甘肃省': ['兰州市', '嘉峪关市', '金昌市', '白银市', '天水市', '武威市', '张掖市', '平凉市', '酒泉市', '庆阳市', '定西市', '陇南市', '临夏回族自治州', '甘南藏族自治州'],
  '青海省': ['西宁市', '海东市', '海北藏族自治州', '黄南藏族自治州', '海南藏族自治州', '果洛藏族自治州', '玉树藏族自治州', '海西蒙古族藏族自治州'],
  '宁夏回族自治区': ['银川市', '石嘴山市', '吴忠市', '固原市', '中卫市'],
  '新疆维吾尔自治区': ['乌鲁木齐市', '克拉玛依市', '吐鲁番市', '哈密市', '昌吉回族自治州', '博尔塔拉蒙古自治州', '巴音郭楞蒙古自治州', '阿克苏地区', '克孜勒苏柯尔克孜自治州', '喀什地区', '和田地区', '伊犁哈萨克自治州', '塔城地区', '阿勒泰地区'],
  '香港特别行政区': ['香港'],
  '澳门特别行政区': ['澳门'],
  '台湾省': ['台北市', '新北市', '桃园市', '台中市', '台南市', '高雄市', '基隆市', '新竹市', '嘉义市']
});
// 计算属性：根据选中省份动态获取城市列表
const cityList = computed(() => {
  return cityData.value[formData.value.province] || [];
});

// 省份切换事件（清空城市）
const handleProvinceChange = () => {
  formData.value.city = '';
};

// 轮播图相关配置
const carouselWrapper = ref<HTMLElement | null>(null);
const currentIndex = ref(0);
const autoPlayTimer = ref<number | null>(null);
const autoPlayInterval = 3000; // 3秒自动轮播

// 流式输出文本方法（对齐AI.vue）
const streamText = (text: string) => {
  streamContent.value = '';
  let index = 0;
  const interval = setInterval(() => {
    if (index < text.length) {
      streamContent.value += text[index];
      index++;
    } else {
      clearInterval(interval);
    }
  }, 50); // 每50ms输出一个字符
};

// 获取推荐核心方法（重构：增加校验、对齐AI.vue接口逻辑）
const getRecommendation = async () => {
  // 表单校验（对齐AI.vue）
  if (!formData.value.province) {
    ElMessage.warning('请选择省份');
    return;
  }
  if (!formData.value.content.trim()) {
    ElMessage.warning('请填写其他要求，没有可填"无"');
    return;
  }
  console.log(formData.value);

  loading.value = true;
  try {
    const res = await aiRecommend(formData.value);
    // 处理后端返回数据结构（对齐AI.vue）
    recommendationResult.value = {
      content: res.data.content,
      recommendList: res.data.recommend_list || []
    };
    // 流式输出推荐内容
    if (res.data.content) {
      streamText(res.data.content);
    }
    // 重置轮播图索引
    currentIndex.value = 0;
  } catch (error) {
    ElMessage.error('获取推荐失败，请重试');
  } finally {
    loading.value = false;
  }
};

// 轮播图样式计算
const carouselStyle = computed(() => {
  if (!recommendationResult.value || !recommendationResult.value.recommendList || recommendationResult.value.recommendList.length === 0) {
    return { transform: 'translateX(0)', transition: 'transform 0.5s ease' };
  }
  return {
    transform: `translateX(-${currentIndex.value * 100}%)`,
    transition: 'transform 0.5s ease'
  };
});

// 轮播项3D样式计算
const getItemStyle = (index: number | string) => {
  if (!recommendationResult.value || !recommendationResult.value.recommendList || recommendationResult.value.recommendList.length === 0) {
    return { opacity: 1, transform: 'scale(1)', zIndex: 1 };
  }
  const numIndex = typeof index === 'string' ? parseInt(index) : index;
  const distance = Math.abs(numIndex - currentIndex.value);
  if (distance > 2) return { opacity: 0, transform: 'scale(0.8)' };

  let opacity = 1;
  let scale = 1;
  let zIndex = recommendationResult.value.recommendList.length - distance;

  switch (distance) {
    case 0:
      opacity = 1;
      scale = 1;
      break;
    case 1:
      opacity = 0.8;
      scale = 0.9;
      break;
    case 2:
      opacity = 0.6;
      scale = 0.8;
      break;
  }

  return {
    opacity,
    transform: `scale(${scale})`,
    zIndex,
    transition: 'all 0.5s ease'
  };
};

// 轮播图方法：上一张
const prev = () => {
  if (!recommendationResult.value || !recommendationResult.value.recommendList || recommendationResult.value.recommendList.length === 0) return;
  currentIndex.value = (currentIndex.value - 1 + recommendationResult.value.recommendList.length) % recommendationResult.value.recommendList.length;
  // 暂停自动轮播5秒
  pauseAutoPlay();
};
// 轮播图方法：下一张
const next = () => {
  if (!recommendationResult.value || !recommendationResult.value.recommendList || recommendationResult.value.recommendList.length === 0) return;
  currentIndex.value = (currentIndex.value + 1) % recommendationResult.value.recommendList.length;
  // 暂停自动轮播5秒
  pauseAutoPlay();
};
// 轮播图方法：跳转到指定索引
const goTo = (index: number | string) => {
  currentIndex.value = typeof index === 'string' ? parseInt(index) : index;
  // 暂停自动轮播5秒
  pauseAutoPlay();
};
// 开始自动轮播
const startAutoPlay = () => {
  if (autoPlayTimer.value) return;
  autoPlayTimer.value = window.setInterval(() => {
    if (recommendationResult.value && recommendationResult.value.recommendList && recommendationResult.value.recommendList.length > 0) {
      next();
    }
  }, autoPlayInterval);
};
// 停止自动轮播
const stopAutoPlay = () => {
  if (autoPlayTimer.value) {
    clearInterval(autoPlayTimer.value);
    autoPlayTimer.value = null;
  }
};

// 暂停自动轮播5秒
const pauseAutoPlay = () => {
  stopAutoPlay();
  // 5秒后重新开始自动轮播
  setTimeout(() => {
    startAutoPlay();
  }, 5000);
};

// 生命周期：挂载时启动轮播，卸载时停止
onMounted(() => {
  startAutoPlay();
});
onUnmounted(() => {
  stopAutoPlay();
});
</script>

<style scoped>
/* 基础样式保留，新增轮播、结果容器等样式（对齐AI.vue） */
.ai-recommend-container {
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

.input-section {
  background: white;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.form-row {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.form-item {
  flex: 1;
  min-width: 150px;
}

.form-item.full-width {
  flex: 100%;
  min-width: 100%;
}

.form-item label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 6px;
}

.form-input,
.form-select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.3s ease;
}

.form-input:focus,
.form-select:focus {
  border-color: #8BC34A;
}

.recommend-textarea {
  width: 100%;
  min-height: 120px;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  resize: vertical;
  outline: none;
  transition: border-color 0.3s ease;
}

.recommend-textarea:focus {
  border-color: #8BC34A;
}

.recommend-btn {
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

.recommend-btn:hover:not(:disabled) {
  background: #7CB342;
}

.recommend-btn:disabled {
  background: #e0e0e0;
  cursor: not-allowed;
}



.recommendation-result {
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

/* 新增：结果分区样式 */
.result-section {
  margin-bottom: 24px;
}

.result-section h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
  color: #2E7D32;
  border-bottom: 1px solid #e0e0e0;
  padding-bottom: 8px;
}

/* 新增：结果容器样式（对齐AI.vue） */
.result-container {
  background: linear-gradient(135deg, #f0f9e8 0%, #e6f7e1 100%);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border: 1px solid #8BC34A;
  position: relative;
  overflow: hidden;
}

.result-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #8BC34A, #4CAF50);
}

.result-content {
  font-size: 14px;
  line-height: 1.6;
  color: #333;
}

/* 新增：轮播图样式（对齐AI.vue） */
.carousel-container {
  padding: 0 4px 24px;
  position: relative;
}

.carousel-wrapper {
  position: relative;
  overflow: hidden;
  border-radius: 12px;
  height: 400px;
}

.carousel {
  display: flex;
  height: 100%;
  transition: transform 0.5s ease;
}

.carousel-item {
  flex: 0 0 100%;
  height: 100%;
  position: relative;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.5s ease;
}

.carousel-item.active {
  z-index: 10;
}

.carousel-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  border: 2px solid #8BC34A;
  color: #8BC34A;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  z-index: 20;
}

.carousel-btn.prev {
  left: 10px;
}

.carousel-btn.next {
  right: 10px;
}

.carousel-btn:hover {
  background: #8BC34A;
  color: white;
  transform: translateY(-50%) scale(1.1);
}

.carousel-indicators {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 16px;
}

.indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e0e0e0;
  cursor: pointer;
  transition: all 0.3s ease;
}

.indicator.active {
  width: 24px;
  border-radius: 4px;
  background: #8BC34A;
}

/* 新增：轮播内乡村和景点样式 */
.carousel-item .village-name {
  font-size: 18px;
  font-weight: 600;
  margin: 16px 16px 12px;
  color: #2E7D32;
  border-bottom: 1px solid #e0e0e0;
  padding-bottom: 8px;
}

.carousel-item .scenic-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 0 16px 16px;
  height: calc(100% - 60px);
  overflow-y: auto;
}

.carousel-item .scenic-card {
  background: #f9f9f9;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.carousel-item .scenic-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.12);
}

.carousel-item .scenic-image {
  width: 100%;
  height: 160px;
  overflow: hidden;
}

.carousel-item .scenic-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.carousel-item .scenic-card:hover .scenic-image img {
  transform: scale(1.05);
}

.carousel-item .scenic-info {
  padding: 12px;
}

.carousel-item .scenic-info h5 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #333;
}

.carousel-item .scenic-intro {
  font-size: 14px;
  line-height: 1.4;
  color: #666;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.carousel-item .scenic-meta {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: #999;
  margin-bottom: 12px;
}

.carousel-item .meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.carousel-item .accommodation {
  background: #e8f5e8;
  border-radius: 6px;
  padding: 10px;
  margin-top: 12px;
}

.carousel-item .accommodation-tag {
  font-size: 13px;
  font-weight: 500;
  color: #4CAF50;
  margin-bottom: 4px;
  display: block;
}

.carousel-item .accommodation-info {
  font-size: 12px;
  line-height: 1.4;
  color: #666;
  margin: 0;
}

/* 移动端适配 */
@media (max-width: 480px) {
  .ai-recommend-container {
    padding: 12px;
  }

  .recommend-textarea {
    min-height: 100px;
  }

  .carousel-wrapper {
    height: 350px;
  }

  .carousel-item .scenic-image {
    height: 140px;
  }
}
</style>