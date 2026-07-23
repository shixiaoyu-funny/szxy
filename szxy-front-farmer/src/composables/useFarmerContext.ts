import { ref } from 'vue';
import { getVillageInfo } from '../api/user';
import { getFarmerList } from '../api/farmer';
import { normalizeVillageVO } from '../utils/villageApi';

export type VillageInfo = {
  id?: number;
  name?: string;
  [key: string]: unknown;
};

const villageInfo = ref<VillageInfo | null>(null);
const isChief = ref(false);
const loading = ref(false);

/** 刷新：当前用户归属村、是否村长（通过本村农户列表接口是否成功推断） */
export async function refreshFarmerContext() {
  loading.value = true;
  try {
    const vRes: { data?: VillageInfo | null } = await getVillageInfo();
    const raw = vRes.data as Record<string, unknown> | null | undefined;
    villageInfo.value = (normalizeVillageVO(raw) as VillageInfo | null) ?? null;
    isChief.value = false;
    if (!villageInfo.value) {
      return { villageInfo: villageInfo.value, isChief: false };
    }
    try {
      await getFarmerList();
      isChief.value = true;
    } catch {
      isChief.value = false;
    }
    return { villageInfo: villageInfo.value, isChief: isChief.value };
  } finally {
    loading.value = false;
  }
}

export function useFarmerContext() {
  return { villageInfo, isChief, loading, refreshFarmerContext };
}
