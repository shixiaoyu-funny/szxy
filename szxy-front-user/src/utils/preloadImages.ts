/** 从逗号分隔字符串或单 URL 收集图片地址 */
export function collectImageUrls(...sources: (string | undefined | null)[]): string[] {
  const urls = new Set<string>();
  for (const src of sources) {
    if (!src) continue;
    for (const part of src.split(',')) {
      const url = part.trim();
      if (url) urls.add(url);
    }
  }
  return [...urls];
}

/** 从列表项中收集 image / avatar / commentImg 等字段 */
export function collectImageUrlsFromItems(
  items: Array<{ image?: string | null; avatar?: string | null; commentImg?: string | null }>
): string[] {
  const urls: string[] = [];
  for (const item of items) {
    urls.push(...collectImageUrls(item.image, item.avatar, item.commentImg));
  }
  return [...new Set(urls)];
}

/** 预加载图片，单张失败或超时均不阻塞整页 */
export function preloadImages(urls: string[], timeoutMs = 10000): Promise<void> {
  const unique = [...new Set(urls.filter(Boolean))];
  if (!unique.length) return Promise.resolve();

  return Promise.all(
    unique.map(
      (url) =>
        new Promise<void>((resolve) => {
          const img = new Image();
          let settled = false;
          const done = () => {
            if (settled) return;
            settled = true;
            resolve();
          };
          const timer = window.setTimeout(done, timeoutMs);
          img.onload = () => {
            window.clearTimeout(timer);
            done();
          };
          img.onerror = () => {
            window.clearTimeout(timer);
            done();
          };
          img.src = url;
        })
    )
  ).then(() => undefined);
}
