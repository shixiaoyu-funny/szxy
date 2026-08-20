<template>
  <div class="about-page">
    <div class="about-hero">
      <h1 class="about-title">数智乡约</h1>
      <p class="about-slogan">AI 驱动乡村振兴服务平台</p>
    </div>

    <div class="about-section">
      <h2 class="section-title">项目亮点</h2>
      <div class="highlight-list">
        <div v-for="item in highlights" :key="item.title" class="highlight-item">
          <span class="highlight-icon">{{ item.icon }}</span>
          <div class="highlight-body">
            <h3 class="highlight-title">{{ item.title }}</h3>
            <p class="highlight-desc">{{ item.desc }}</p>
          </div>
        </div>
      </div>
    </div>

    <div class="about-section">
      <h2 class="section-title">技术栈</h2>
      <div class="tech-grid">
        <div v-for="t in techStack" :key="t.name" class="tech-card">
          <span class="tech-name">{{ t.name }}</span>
          <span class="tech-cat">{{ t.cat }}</span>
          <p class="tech-desc">{{ t.desc }}</p>
        </div>
      </div>
    </div>

    <div class="about-meta">
      <p>版本：1.0.0</p>
      <p>数智乡约 —— 让乡村走向智慧化</p>
    </div>
  </div>
</template>

<script setup lang="ts">
const highlights = [
  {
    icon: '🌾',
    title: '乡村数字化',
    desc: '聚合优质农村与景点资源，一键浏览、搜索、详情直达，助力乡村品牌推广'
  },
  {
    icon: '🤝',
    title: '农户直连',
    desc: '农户建档 + 村长任命体系，农户自主管理村庄景点，村长统筹本村农户'
  },
  {
    icon: '📊',
    title: '数据看板',
    desc: '管理员实时掌握农户数量、UV/PV、新增资源与近 7 天访问趋势'
  },
  {
    icon: '🛡️',
    title: '安全可靠',
    desc: '无状态 Token 鉴权、Redis 会话、角色权限分级（游客/农户/村长/管理员）'
  }
];

const techStack = [
  { name: 'Vue 3', cat: '前端', desc: '组合式 API + TypeScript，构建响应式单页应用' },
  { name: 'Element Plus', cat: '前端', desc: '企业级 UI 组件库，统一美观的界面交互' },
  { name: 'Vite', cat: '前端', desc: '极速开发构建，HMR 热更新体验' },
  { name: 'Pinia', cat: '前端', desc: '轻量状态管理，跨组件共享用户会话' },
  { name: 'Vue Router', cat: '前端', desc: '路由驱动页面导航与权限守卫' },
  { name: 'Spring Boot', cat: '后端', desc: '简洁高效的服务端框架，快速构建 RESTful API' },
  { name: 'MyBatis-Plus', cat: '后端', desc: 'ORM 持久层，简化数据访问与分页查询' },
  { name: 'Redis', cat: '中间件', desc: '无状态会话存储，支持登录态高效续期与注销' },
  { name: 'RabbitMQ', cat: '中间件', desc: '消息队列，支撑异步解耦与削峰' },
  { name: 'MySQL', cat: '数据库', desc: '可靠的关系型数据库，承载核心业务数据' }
];
</script>

<style scoped>
.about-page {
  max-width: 1000px;
  margin: 0 auto;
}

.about-hero {
  text-align: center;
  padding: 48px 24px;
  background: linear-gradient(135deg, #4CAF50 0%, #8BC34A 100%);
  border-radius: 16px;
  color: #fff;
  margin-bottom: 32px;
  box-shadow: 0 8px 24px rgba(76, 175, 80, 0.3);
}

.about-title {
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 12px;
  color: #fff;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.about-slogan {
  font-size: 16px;
  margin: 0;
  opacity: 0.92;
}

.about-section {
  background: #fff;
  border-radius: 12px;
  padding: 28px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  margin-bottom: 24px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0 0 20px;
  padding-left: 12px;
  border-left: 4px solid #8BC34A;
}

.highlight-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.highlight-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px;
  border-radius: 10px;
  background: #f9fbf7;
  transition: all 0.3s ease;
}

.highlight-item:hover {
  background: #f0f9e8;
  transform: translateY(-2px);
}

.highlight-icon {
  font-size: 28px;
  line-height: 1;
  flex-shrink: 0;
}

.highlight-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 6px;
}

.highlight-desc {
  font-size: 13px;
  color: #666;
  line-height: 1.6;
  margin: 0;
}

.tech-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.tech-card {
  padding: 16px;
  border-radius: 10px;
  background: #f9fbf7;
  border: 1px solid #eef4e6;
  transition: all 0.3s ease;
}

.tech-card:hover {
  border-color: #8BC34A;
  box-shadow: 0 4px 12px rgba(139, 195, 74, 0.2);
  transform: translateY(-2px);
}

.tech-name {
  font-size: 16px;
  font-weight: 600;
  color: #2E7D32;
  margin-right: 8px;
}

.tech-cat {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: #e8f5e9;
  color: #558B2F;
}

.tech-desc {
  font-size: 12px;
  color: #888;
  margin: 8px 0 0;
  line-height: 1.5;
}

.about-meta {
  text-align: center;
  color: #999;
  font-size: 13px;
  padding: 8px 0 24px;
}

.about-meta p {
  margin: 0 0 4px;
}
</style>
