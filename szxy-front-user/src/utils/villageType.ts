/** 村落特色类型（与后端 VillageTypeEnum 一致） */
export const VILLAGE_TYPE_MAP: Record<number, string> = {
  1: '古建聚落型',
  2: '非遗民俗型',
  3: '山水生态型',
  4: '农业观光型',
  5: '近郊休闲型',
  6: '康养度假型',
  7: '红色研学型',
  8: '滨水渔乡型',
  9: '民族村寨型',
  10: '综合文旅型'
};

export function getVillageTypeLabel(type?: number | null): string {
  if (type == null) return '特色农村';
  return VILLAGE_TYPE_MAP[type] ?? '特色农村';
}
