<template>
  <div class="region-cascader">
    <el-cascader
      v-model="innerPath"
      :props="cascaderProps"
      clearable
      filterable
      :disabled="disabled"
      placeholder="请选择省 / 市 / 区县"
      style="width: 100%"
      @change="onChange"
    />
    <p v-if="displayText" class="current">当前：{{ displayText }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { ElMessage, type CascaderProps } from 'element-plus';
import { fetchXzqhChildren, type XzqhNode } from '../api/region';

const props = withDefaults(
  defineProps<{
    province?: string;
    city?: string;
    county?: string;
    disabled?: boolean;
  }>(),
  {
    province: '',
    city: '',
    county: '',
    disabled: false
  }
);

const emit = defineEmits<{
  'update:province': [value: string];
  'update:city': [value: string];
  'update:county': [value: string];
}>();

interface CascaderOption {
  value: string;
  label: string;
  leaf?: boolean;
  [key: string]: unknown;
}

const innerPath = ref<string[]>([]);
const nameByCode = new Map<string, string>();
/** 标记「县」等无下级节点，二级选中时 city/county 都用该名称 */
const leafAtCityCodes = new Set<string>();

const displayText = computed(() => {
  const parts = [props.province, props.city, props.county].filter(Boolean);
  return parts.join(' / ');
});

function isHkMo(code: string, name?: string): boolean {
  return code.startsWith('81') || code.startsWith('82') || name === '香港特别行政区' || name === '澳门特别行政区';
}

function isLeafNode(node: XzqhNode, depth: number): boolean {
  // depth：省=1，市/县=2，区=3
  if (depth >= 3) return true;
  // 港澳：省级即叶子
  if (depth === 1 && isHkMo(node.code, node.name)) return true;
  // 台湾县：无 children，市级叶子
  if (depth === 2 && (!node.children || node.children.length === 0) && node.type === '县') {
    return true;
  }
  // 大陆直辖市：省下直接是区
  const level = Number(node.level);
  if (depth === 2 && (level === 3 || level === 4)) return true;
  const t = node.type || '';
  if (depth === 2 && (t === '市辖区' || t === '街道' || t === '镇' || t === '乡')) {
    return true;
  }
  return false;
}

function toOption(node: XzqhNode, depth: number): CascaderOption {
  nameByCode.set(node.code, node.name);
  const leaf = isLeafNode(node, depth);
  if (leaf && depth === 2) {
    leafAtCityCodes.add(node.code);
  }
  return {
    value: node.code,
    label: node.name,
    leaf
  };
}

const cascaderProps: CascaderProps = {
  lazy: true,
  async lazyLoad(node, resolve) {
    try {
      const code = (node.value as string) || '';
      const nodes = await fetchXzqhChildren(code, 1);
      const childDepth = node.level + 1;
      resolve(nodes.map((n) => toOption(n, childDepth)));
    } catch (e) {
      console.error(e);
      ElMessage.error(e instanceof Error ? e.message : '加载行政区划失败');
      resolve([]);
    }
  }
};

/**
 * - 普通三级：[省, 市, 区]
 * - 大陆直辖市两级：[北京市, 东城区] → city=省名
 * - 台湾县二级：[台湾省, 新竹县] → city=county=县名
 * - 港澳一级：[香港特别行政区] → 三省字段同名
 */
function mapPathToRegion(codes: string[]) {
  const names = codes.map((c) => nameByCode.get(c) || '').filter(Boolean);
  if (names.length === 0) {
    return { province: '', city: '', county: '' };
  }
  const n0 = names[0] ?? '';
  const n1 = names[1] ?? '';
  const n2 = names[2] ?? '';
  if (names.length === 1) {
    // 港澳等省级叶子
    return { province: n0, city: n0, county: n0 };
  }
  if (names.length === 2) {
    const cityCode = codes[1] || '';
    // 台湾县：city/county 均写县名；大陆直辖市：city 写省名
    if (leafAtCityCodes.has(cityCode) || cityCode.startsWith('71')) {
      return { province: n0, city: n1, county: n1 };
    }
    return { province: n0, city: n0, county: n1 };
  }
  return { province: n0, city: n1, county: n2 };
}

function onChange(val: string[] | string | null) {
  const codes = Array.isArray(val) ? val.map(String) : [];
  innerPath.value = codes;
  const mapped = mapPathToRegion(codes);
  emit('update:province', mapped.province);
  emit('update:city', mapped.city);
  emit('update:county', mapped.county);
}

watch(
  () => [props.province, props.city, props.county] as const,
  () => {
    if (!props.province && !props.city && !props.county) {
      innerPath.value = [];
    }
  }
);
</script>

<style scoped>
.region-cascader {
  width: 100%;
}

.current {
  margin: 6px 0 0;
  font-size: 12px;
  color: #909399;
}
</style>
