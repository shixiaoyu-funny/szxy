import { onMounted, onUnmounted, ref, type Ref } from 'vue';
import { fetchIpLocation, type IpLocationResult } from '../utils/amapIpLocate';
import { reportPos } from '../api/pos';
import { useUserStore } from '../stores/user';

const POLL_MS = 3 * 60 * 1000;

export interface UseIpLocationPollResult {
  locationText: Ref<string>;
  loading: Ref<boolean>;
  error: Ref<string>;
  lastResult: Ref<IpLocationResult | null>;
  refresh: () => Promise<void>;
}

/**
 * 每 3 分钟轮询：公网 IP → 高德 IP 定位；登录用户同步上报后端 Redis。
 */
export function useIpLocationPoll(): UseIpLocationPollResult {
  const locationText = ref('定位中…');
  const loading = ref(false);
  const error = ref('');
  const lastResult = ref<IpLocationResult | null>(null);
  const userStore = useUserStore();

  let timer: ReturnType<typeof setInterval> | null = null;
  let abort: AbortController | null = null;

  const reportToBackend = async (result: IpLocationResult): Promise<void> => {
    if (!userStore.isLoggedIn) return;
    const display = (result.displayText || '').trim();
    if (!display || display === '未知区域' || display === '未知位置' || display.includes('局域网')) {
      console.warn('[ip-location] 未知区域，跳过上报', result);
      return;
    }
    const province = (result.province || '').trim();
    const city = (result.city || '').trim();
    const county = (result.district || '').trim();
    if (!province || !city || !county || province === '局域网') {
      console.warn('[ip-location] 省市区不完整或无效，跳过上报', result);
      return;
    }
    try {
      await reportPos({ province, city, county });
    } catch (e) {
      console.warn('[ip-location] 上报失败', e);
    }
  };

  const refresh = async (): Promise<void> => {
    abort?.abort();
    abort = new AbortController();
    loading.value = true;
    error.value = '';
    try {
      const result = await fetchIpLocation(abort.signal);
      lastResult.value = result;
      locationText.value = result.displayText;
      await reportToBackend(result);
    } catch (e) {
      if ((e as Error)?.name === 'AbortError') return;
      const msg = e instanceof Error ? e.message : '未知区域';
      error.value = msg;
      // 定位失败视为未知区域：不上报、不触发推荐
      locationText.value = '未知区域';
      console.warn('[ip-location]', msg);
    } finally {
      loading.value = false;
    }
  };

  onMounted(() => {
    void refresh();
    timer = setInterval(() => {
      void refresh();
    }, POLL_MS);
  });

  onUnmounted(() => {
    if (timer != null) {
      clearInterval(timer);
      timer = null;
    }
    abort?.abort();
    abort = null;
  });

  return { locationText, loading, error, lastResult, refresh };
}
