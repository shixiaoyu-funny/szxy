<template>
  <footer class="site-footer">
    <p class="footer-line footer-copy">
      © 2026 数智乡约 — AI驱动乡村振兴服务平台 版权所有
    </p>
    <p class="footer-line">地址：山西省太原市</p>
    <p class="footer-line">
      联系方式：
      <a class="footer-link" href="mailto:2547679180@qq.com">2547679180@qq.com</a>
    </p>
    <p class="footer-line">备案号：待填写ICP备案号</p>
    <p class="footer-line">
      友情链接：
      <router-link class="footer-link" to="/disclaimer">免责声明</router-link>
    </p>
  </footer>
</template>

<style scoped>
.site-footer {
  padding: 20px 24px 24px;
  text-align: center;
  background: #fff;
  border-top: 1px solid #ebeef5;
  color: #909399;
  font-size: 13px;
  line-height: 1.85;
}

.footer-line {
  margin: 0;
}

.footer-copy {
  color: #606266;
  font-weight: 500;
}

.footer-link {
  color: #4caf50;
  text-decoration: none;
  transition: color 0.2s ease;
}

.footer-link:hover {
  color: #2e7d32;
  text-decoration: underline;
}
</style>
