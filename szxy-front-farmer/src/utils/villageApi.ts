/**
 * 后端 spring.jackson.property-naming-strategy: SNAKE_CASE 时，
 * 村落 VO 中多词属性会变成 snake_case（如 manager_name、best_time），
 * 与模板中驼峰不一致。统一在此处合并为驼峰优先可读字段。
 */
export function normalizeVillageVO(raw: Record<string, unknown> | null | undefined): Record<string, unknown> | null {
  if (raw == null || typeof raw !== 'object') {
    return null;
  }
  const r = raw;
  return {
    ...r,
    managerName: r.managerName ?? r.manager_name,
    bestTime: r.bestTime ?? r.best_time
  };
}
