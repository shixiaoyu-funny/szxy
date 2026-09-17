<template>
  <div class="disclaimer-page">
    <article class="disclaimer-sheet">
      <h1 class="doc-title">免责声明</h1>

      <section class="doc-section">
        <p class="doc-paragraph">
          欢迎访问数智乡约平台。本平台为乡村振兴信息展示、资源推广服务线上系统，平台所发布的农村景点、农户信息、图文介绍等内容，大部分由各村村长、农户自主上传发布，仅作为公益性信息展示用途，不构成任何交易邀约、投资建议或者出行担保。
        </p>
      </section>

      <section class="doc-section">
        <p class="doc-paragraph">
          平台仅提供信息存储与展示技术服务，不对第三方用户上传信息的真实性、完整性、时效性作出绝对保证。因农户、村庄自行发布的信息有误、内容失实所引发的一切纠纷、损失，均由信息发布主体自行承担相应责任，本平台不承担连带赔偿责任。
        </p>
      </section>

      <section class="doc-section">
        <p class="doc-paragraph">
          访问者依据本平台内容做出任何出游、合作、采购等决策前，建议通过线下渠道与当地农户、村委会进一步核实确认相关信息。因使用平台信息而产生的任何直接或间接损失，平台运营方无需承担赔偿责任。
        </p>
      </section>

      <section class="doc-section">
        <p class="doc-paragraph">
          本平台会不定期对站点内容、功能、服务规则进行调整更新，且无需提前通知用户。如您在浏览本站时发现存在侵权、虚假、违规内容，欢迎及时联系管理人员，我们将在收到反馈后尽快核查处理。
        </p>
      </section>

      <section class="doc-section">
        <p class="doc-paragraph">
          若您对本免责声明条款存有异议，请停止使用本网站所有服务；您继续浏览、使用本平台，即代表您已经阅读、理解并且完全同意本份免责声明全部条款。
        </p>
      </section>
    </article>
  </div>
</template>

<style scoped>
.disclaimer-page {
  min-height: calc(100vh - 180px);
  margin: -24px;
  padding: 32px 24px 48px;
  background: #ececec;
}

.disclaimer-sheet {
  max-width: 794px;
  min-height: 1123px;
  margin: 0 auto;
  padding: 72px 64px 80px;
  background: #fff;
  color: #1a1a1a;
  box-shadow:
    0 1px 3px rgba(0, 0, 0, 0.08),
    0 8px 24px rgba(0, 0, 0, 0.06);
  box-sizing: border-box;
}

.doc-title {
  margin: 0 0 40px;
  font-size: 28px;
  font-weight: 700;
  text-align: center;
  letter-spacing: 0.08em;
  color: #000;
}

.doc-section {
  margin-bottom: 28px;
}

.doc-section:last-child {
  margin-bottom: 0;
}

.doc-heading {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 600;
  color: #000;
}

.doc-paragraph {
  margin: 0;
  font-size: 15px;
  line-height: 2;
  text-align: justify;
  text-indent: 2em;
  color: #222;
}

@media (max-width: 860px) {
  .disclaimer-sheet {
    min-height: auto;
    padding: 48px 28px 56px;
  }
}
</style>
