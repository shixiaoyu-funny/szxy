/** 评论分词统计（extractWords / buildWordCloudSeriesData）+ 词频配色（wordColorByName）；热词排行由原生 HTML/CSS 展示 */

const STOP_WORDS = new Set([
  '的',
  '了',
  '和',
  '是',
  '在',
  '我',
  '有',
  '就',
  '不',
  '人',
  '都',
  '一',
  '一个',
  '上',
  '也',
  '很',
  '到',
  '说',
  '要',
  '去',
  '你',
  '会',
  '着',
  '没有',
  '看',
  '好',
  '自己',
  '这',
  '那',
  '吗',
  '呢',
  '吧',
  '啊',
  '还',
  '又',
  '与',
  '及',
  '对',
  '为',
  '以',
  '从',
  '被',
  '让',
  '给',
  '能',
  '可以',
  '什么',
  '怎么',
  '非常',
  '比较',
  '真的',
  '就是',
  '还是',
  '这个',
  '那个',
  '这里',
  '景区',
  '景点',
  '地方'
]);

/**
 * 过滤明显无效片段；中文以正则切分为准，规则从宽，避免误杀有效词。
 * 不使用 Intl.Segmenter，故此处仅校验拉丁等杂项。
 */
export function isMeaningfulToken(w: string): boolean {
  const s = w.trim();
  if (s.length < 2 || s.length > 32) return false;

  if (/^[\u4e00-\u9fff]+$/.test(s)) {
    return s.length <= 18;
  }

  if (/^[a-z]{2,24}$/.test(s)) {
    if (s.length > 10 && !/[aeiouy]/.test(s)) return false;
    return true;
  }

  if (/^[A-Z]{2,5}$/.test(s)) return true;

  if (/^[A-Z][a-z]+$/.test(s) && s.length <= 20) return true;

  if (/^[a-zA-Z]+$/.test(s)) {
    if (/[a-z][A-Z]/.test(s) || /[A-Z][a-z][A-Z]/.test(s)) return false;
    const upper = (s.match(/[A-Z]/g) ?? []).length;
    if (upper > 3 && s.length > 8) return false;
    if (s.length > 8 && upper > 1 && /[a-z]/.test(s) && /[A-Z]/.test(s)) return false;
    return true;
  }

  if (/[\d_]/.test(s)) return false;

  return false;
}

function extractWords(text: string): string[] {
  const out: string[] = [];
  const latin = text.match(/[a-zA-Z]{2,}/g);
  if (latin) {
    for (const w of latin) {
      const t = w.toLowerCase();
      if (isMeaningfulToken(t)) out.push(t);
    }
  }

  /** 全程稳定正则分块（禁用 Intl.Segmenter，避免各手机 WebView 分词结果为空或异常导致无热词） */
  const cjk = text.match(/[\u4e00-\u9fff]{2,12}/g) || [];
  for (const w of cjk) {
    if (!STOP_WORDS.has(w) && isMeaningfulToken(w)) out.push(w);
  }
  return out;
}

export function buildWordCloudSeriesData(
  comments: ReadonlyArray<{ content?: string }>,
  maxTerms = 100
): { name: string; value: number }[] {
  const freq = new Map<string, number>();
  for (const c of comments) {
    const content = (c.content ?? '').trim();
    if (!content) continue;
    for (const w of extractWords(content)) {
      freq.set(w, (freq.get(w) ?? 0) + 1);
    }
  }
  return [...freq.entries()]
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => b.value - a.value)
    .slice(0, maxTerms);
}

function hashTo01(str: string): number {
  let h = 2166136261;
  for (let i = 0; i < str.length; i++) {
    h ^= str.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  return (h >>> 0) / 4294967296;
}

/** H:120~160，S:55~75%，L:40~60% — 清新绿调、浅色背景可读 */
export function wordColorByName(name: string): string {
  const t = hashTo01(name);
  const h = 120 + t * 40;
  const s = 55 + hashTo01(name + 's') * 20;
  const l = 40 + hashTo01(name + 'l') * 20;
  return `hsl(${h.toFixed(1)}, ${s.toFixed(1)}%, ${l.toFixed(1)}%)`;
}

export const WORD_CLOUD_FONT_FAMILY =
  '"PingFang SC","Microsoft YaHei","Noto Sans SC",ui-sans-serif,sans-serif';

/** 仅展示前 N 个高频词；频次降序，首条为第一名（最上） */
export const COMMENT_RANK_TOP_N = 10;

export type CommentRankRow = { name: string; value: number };

/** 与 buildWordCloudSeriesData(..., topN) 一致，专用于热词排行列表 */
export function getCommentRankRows(
  comments: ReadonlyArray<{ content?: string }>,
  topN = COMMENT_RANK_TOP_N
): CommentRankRow[] {
  return buildWordCloudSeriesData(comments, topN);
}

/** 进度条长度：相对最高频次的百分比 0–100 */
export function commentRankBarPercent(value: number, maxValue: number): number {
  if (maxValue <= 0 || value <= 0) return 0;
  return Math.min(100, Math.round((value / maxValue) * 10000) / 100);
}
