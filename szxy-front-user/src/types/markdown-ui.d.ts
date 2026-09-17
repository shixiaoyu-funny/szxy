declare module '@markdown-ui/vue' {
  import type { DefineComponent } from 'vue';

  export const MarkdownUI: DefineComponent<
    { html: string },
    Record<string, never>,
    unknown,
    Record<string, never>,
    Record<string, never>,
    Record<string, never>,
    Record<string, never>,
    { widgetEvent: (event: CustomEvent<{ id: string; value: unknown }>) => void }
  >;
}

declare module '@markdown-ui/vue/widgets.css';
