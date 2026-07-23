<template>
  <div class="dashboard-container">
    <h2 class="page-title">报表查询</h2>
    <div class="stats-grid">
      <div v-for="(stat, index) in stats" :key="index" class="stat-card">
        <div class="stat-icon" :class="stat.color">
          <el-icon :size="28"><component :is="stat.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <h3 class="stat-title">{{ stat.title }}</h3>
          <p class="stat-value">{{ formatAnimatedValue(index) }}</p>
        </div>
      </div>
    </div>

    <h3 class="section-title">近 7 日访问走势</h3>
    <div class="charts-row">
      <div class="chart-card">
        <div class="chart-head">
          <span class="chart-title">页面点击量 PV</span>
        </div>
        <div ref="pvChartRef" class="chart-host" />
      </div>
      <div class="chart-card">
        <div class="chart-head">
          <span class="chart-title">独立访客 UV</span>
        </div>
        <div ref="uvChartRef" class="chart-host" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import { UserFilled, View, Histogram, TrendCharts, Picture, LocationFilled } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import * as echarts from 'echarts';
import type { ECharts } from 'echarts';
import { reportApi } from '../api';

type StatFormat = 'int' | 'float' | 'ratio';

interface Stat {
  title: string;
  value: number;
  icon: typeof UserFilled;
  color: string;
  format: StatFormat;
}

const pvChartRef = ref<HTMLDivElement | null>(null);
const uvChartRef = ref<HTMLDivElement | null>(null);
let pvChart: ECharts | null = null;
let uvChart: ECharts | null = null;

const stats = ref<Stat[]>([
  { title: '农户总数', value: 0, icon: UserFilled, color: 'green', format: 'int' },
  { title: '当日独立访客量UV', value: 0, icon: View, color: 'blue', format: 'int' },
  { title: '当日页面点击量PV', value: 0, icon: Histogram, color: 'cyan', format: 'int' },
  { title: 'PV / UV（粘性）', value: 0, icon: TrendCharts, color: 'purple', format: 'ratio' },
  { title: '当日新增景区数', value: 0, icon: Picture, color: 'yellow', format: 'int' },
  { title: '当日新增农村数', value: 0, icon: LocationFilled, color: 'red', format: 'int' }
]);

/** 与 stats 同步，用于展示从 0 递增至目标值的动画 */
const displayValues = ref<number[]>([0, 0, 0, 0, 0, 0]);

let countUpRafId: number | null = null;

function cancelCountUp() {
  if (countUpRafId != null) {
    cancelAnimationFrame(countUpRafId);
    countUpRafId = null;
  }
}

/** 末端更慢，观感更稳 */
function easeOutQuint(t: number): number {
  return 1 - Math.pow(1 - t, 5);
}

function prefersReducedMotion(): boolean {
  if (typeof window === 'undefined' || !window.matchMedia) return false;
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

/**
 * 每张卡片从 0 递增至目标值；错开起播时间，整体更有层次。
 * 刷新时始终从 0 开始。
 */
function runCountUp(targets: number[]) {
  cancelCountUp();
  const n = targets.length;
  if (n === 0) return;

  if (prefersReducedMotion()) {
    displayValues.value = targets.slice();
    return;
  }

  displayValues.value = targets.map(() => 0);

  const staggerMs = 72;
  const eachMs = 780;
  const start = performance.now();

  const tick = (now: number) => {
    const elapsed = now - start;
    displayValues.value = targets.map((target, i) => {
      const t0 = i * staggerMs;
      const local = elapsed - t0;
      if (local <= 0) return 0;
      const u = Math.min(1, local / eachMs);
      return target * easeOutQuint(u);
    });
    const lastEnd = (n - 1) * staggerMs + eachMs;
    if (elapsed < lastEnd) {
      countUpRafId = requestAnimationFrame(tick);
    } else {
      displayValues.value = targets.slice();
      countUpRafId = null;
    }
  };

  countUpRafId = requestAnimationFrame(tick);
}

/** 后端顺序为「当天 → 往前」；图表横轴按时间从左到右为「早 → 晚」 */
function last7DayLabels(): string[] {
  const labels: string[] = [];
  for (let daysAgo = 6; daysAgo >= 0; daysAgo--) {
    const d = new Date();
    d.setHours(0, 0, 0, 0);
    d.setDate(d.getDate() - daysAgo);
    labels.push(`${d.getMonth() + 1}/${d.getDate()}`);
  }
  return labels;
}

function normalizeSeries7(raw: unknown): number[] {
  const arr = Array.isArray(raw) ? raw : [];
  const nums = arr.map((x) => {
    const n = Number(x);
    return Number.isFinite(n) ? n : 0;
  });
  const seven = nums.slice(0, 7);
  while (seven.length < 7) seven.push(0);
  return seven.reverse();
}

function baseLineOption(title: string, color: string, data: number[]) {
  return {
    color: [color],
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'line' }
    },
    grid: { left: 48, right: 24, top: 32, bottom: 28 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: last7DayLabels(),
      axisLabel: { color: '#666' }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { type: 'dashed', color: '#eee' } },
      axisLabel: { color: '#666' }
    },
    series: [
      {
        name: title,
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { width: 2 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: `${color}55` },
            { offset: 1, color: `${color}08` }
          ])
        },
        data
      }
    ]
  };
}

function disposeTrendCharts() {
  pvChart?.dispose();
  uvChart?.dispose();
  pvChart = null;
  uvChart = null;
}

function bindResize() {
  pvChart?.resize();
  uvChart?.resize();
}

let onResize: (() => void) | null = null;

async function renderTrendCharts(pvRaw: unknown, uvRaw: unknown) {
  if (onResize) {
    window.removeEventListener('resize', onResize);
    onResize = null;
  }
  await nextTick();
  disposeTrendCharts();
  const pvData = normalizeSeries7(pvRaw);
  const uvData = normalizeSeries7(uvRaw);

  if (pvChartRef.value) {
    pvChart = echarts.init(pvChartRef.value);
    pvChart.setOption(baseLineOption('PV', '#00acc1', pvData));
  }
  if (uvChartRef.value) {
    uvChart = echarts.init(uvChartRef.value);
    uvChart.setOption(baseLineOption('UV', '#1e88e5', uvData));
  }

  onResize = () => bindResize();
  window.addEventListener('resize', onResize);
}

function formatAnimatedValue(index: number): string {
  const fmt = stats.value[index]?.format;
  const v = displayValues.value[index];
  if (v == null || Number.isNaN(v) || !Number.isFinite(v)) {
    return fmt === 'ratio' ? '—' : '0';
  }
  if (fmt === 'ratio') {
    return v.toFixed(4);
  }
  return String(Math.round(v));
}

const fetchStats = async () => {
  try {
    const [farm, uv, pv, uvpv, scenic, village, pv7, uv7] = await Promise.all([
      reportApi.getFarmCount(),
      reportApi.getUv(),
      reportApi.getPv(),
      reportApi.getUvpv(),
      reportApi.getNewScenicCount(),
      reportApi.getNewVillageCount(),
      reportApi.getPv7(),
      reportApi.getUv7()
    ]);
    const targets = [
      Number(farm) || 0,
      Number(uv) || 0,
      Number(pv) || 0,
      Number(uvpv) || 0,
      Number(scenic) || 0,
      Number(village) || 0
    ];
    const next: Stat[] = [
      { ...stats.value[0]!, value: targets[0]! },
      { ...stats.value[1]!, value: targets[1]! },
      { ...stats.value[2]!, value: targets[2]! },
      { ...stats.value[3]!, value: targets[3]! },
      { ...stats.value[4]!, value: targets[4]! },
      { ...stats.value[5]!, value: targets[5]! }
    ];
    stats.value = next;
    runCountUp(targets);
    await renderTrendCharts(pv7, uv7);
  } catch (e) {
    console.error(e);
    ElMessage.error('加载报表失败，请检查登录态与后端服务');
  }
};

onMounted(() => {
  displayValues.value = stats.value.map(() => 0);
  fetchStats();
});

onUnmounted(() => {
  cancelCountUp();
  if (onResize) {
    window.removeEventListener('resize', onResize);
    onResize = null;
  }
  disposeTrendCharts();
});
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: bold;
  margin: 0 0 8px;
  color: #333;
}

.page-desc {
  font-size: 13px;
  color: #888;
  margin: 0 0 24px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.stat-card {
  background: white;
  border-radius: 10px;
  padding: 18px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  gap: 16px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #fff;
  flex-shrink: 0;
}

.stat-icon.green {
  background: linear-gradient(135deg, #4caf50 0%, #8bc34a 100%);
}
.stat-icon.blue {
  background: linear-gradient(135deg, #2196f3 0%, #64b5f6 100%);
}
.stat-icon.cyan {
  background: linear-gradient(135deg, #00bcd4 0%, #4dd0e1 100%);
}
.stat-icon.purple {
  background: linear-gradient(135deg, #7e57c2 0%, #b39ddb 100%);
}
.stat-icon.yellow {
  background: linear-gradient(135deg, #ffc107 0%, #ffd54f 100%);
}
.stat-icon.red {
  background: linear-gradient(135deg, #f44336 0%, #ef9a9a 100%);
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-title {
  font-size: 13px;
  color: #666;
  margin: 0 0 6px;
  font-weight: 500;
}

.stat-value {
  font-size: 26px;
  font-weight: bold;
  color: #222;
  margin: 0;
  word-break: break-all;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.02em;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  margin: 28px 0 14px;
  color: #333;
}

.charts-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 16px;
}

.chart-card {
  background: white;
  border-radius: 10px;
  padding: 0 0 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.chart-head {
  padding: 14px 18px 0;
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: #444;
}

.chart-host {
  width: 100%;
  height: 300px;
}
</style>
