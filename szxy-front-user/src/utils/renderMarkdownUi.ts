import { Marked } from 'marked';
import { markedUiExtension } from '@markdown-ui/marked-ext';
import DOMPurify from 'dompurify';

const markedUi = new Marked();

const renderer = {
  link({ href, title, text }: { href: string; title?: string | null; text: string }) {
    const safeTitle = title ? ` title="${title}"` : '';
    return `<a href="${href}"${safeTitle} target="_blank" rel="noopener noreferrer">${text}</a>`;
  },
};

markedUi.use(markedUiExtension);
markedUi.use({
  renderer,
  gfm: true,
  breaks: true,
});

/**
 * 将助手 Markdown（含 markdown-ui-widget）转为可交给 MarkdownUI 的 HTML。
 * 白名单保留自定义元素，否则 widget 会被消毒掉。
 */
export function renderMarkdownUi(content: string): string {
  if (!content) return '';
  const raw = markedUi.parse(content, { async: false }) as string;
  return DOMPurify.sanitize(raw, {
    ADD_TAGS: ['markdown-ui-widget'],
    ADD_ATTR: ['id', 'content', 'target', 'rel'],
  });
}

/** 从 widget id 解析下单商品 ID：buy-39 / cart-39（兼容旧加购 id） */
export function parseCartProductId(widgetId: string | undefined | null): number | null {
  if (!widgetId) return null;
  const m = String(widgetId).trim().match(/^(?:cart|buy)-(\d+)$/i);
  if (!m) return null;
  const id = Number(m[1]);
  return Number.isFinite(id) && id > 0 ? id : null;
}
