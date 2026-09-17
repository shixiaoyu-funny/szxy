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
        <a
          v-for="t in techStack"
          :key="t.name"
          class="tech-card"
          :class="t.accent ? `tech-card--${t.accent}` : undefined"
          :href="t.url"
          target="_blank"
          rel="noopener noreferrer"
          :title="`访问 ${t.name} 官网`"
        >
          <span class="tech-name">{{ t.name }}</span>
          <span class="tech-cat">{{ t.cat }}</span>
          <p class="tech-desc">{{ t.desc }}</p>
        </a>
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
  // AI
  {
    name: 'Spring AI',
    cat: 'AI',
    desc: '对话编排、Tool Calling 与 Embedding 统一接入',
    url: 'https://spring.io/projects/spring-ai',
    accent: 'red'
  },
  {
    name: 'Milvus',
    cat: '向量数据库',
    desc: '向量检索引擎，支撑知识库与 RAG 召回',
    url: 'https://milvus.io/',
    accent: 'red'
  },
  // 后端
  {
    name: 'Java 17',
    cat: '后端运行环境',
    desc: 'LTS 长期支持版本，稳定支撑 Spring Boot 服务',
    url: 'https://openjdk.org/projects/jdk/17/',
    accent: 'blue'
  },
  {
    name: 'Spring Boot',
    cat: '后端框架',
    desc: '简洁高效的服务端框架，快速构建 RESTful API',
    url: 'https://spring.io/projects/spring-boot',
    accent: 'blue'
  },
  {
    name: 'MyBatis-Plus',
    cat: 'ORM',
    desc: '持久层框架，简化数据访问与分页查询',
    url: 'https://baomidou.com/',
    accent: 'blue'
  },
  {
    name: 'MySQL',
    cat: '数据库',
    desc: '关系型数据库，承载核心业务数据',
    url: 'https://www.mysql.com/',
    accent: 'blue'
  },
  {
    name: 'Redis',
    cat: '缓存',
    desc: '会话与热点数据缓存，支持登录态高效续期',
    url: 'https://redis.io/',
    accent: 'blue'
  },
  {
    name: 'RabbitMQ',
    cat: '消息队列',
    desc: '异步解耦与削峰，支撑邮件/短信等后台任务',
    url: 'https://www.rabbitmq.com/',
    accent: 'blue'
  },
  {
    name: '阿里云 OSS',
    cat: '对象存储',
    desc: '云端存储景点封面、用户头像等静态资源',
    url: 'https://www.aliyun.com/product/oss',
    accent: 'blue'
  },
  // 前端
  {
    name: 'Vue 3',
    cat: '框架',
    desc: '组合式 API 构建响应式单页应用',
    url: 'https://vuejs.org/',
    accent: 'green'
  },
  {
    name: 'TypeScript',
    cat: '前端语言',
    desc: '静态类型约束，提升代码可维护性',
    url: 'https://www.typescriptlang.org/',
    accent: 'green'
  },
  {
    name: 'Vite',
    cat: '前端构建',
    desc: '极速开发构建，HMR 热更新体验',
    url: 'https://vite.dev/',
    accent: 'green'
  },
  {
    name: 'Pinia',
    cat: '状态管理',
    desc: '轻量 Store，跨组件共享用户会话',
    url: 'https://pinia.vuejs.org/',
    accent: 'green'
  },
  {
    name: 'Vue Router',
    cat: '路由',
    desc: '页面导航与路由级权限控制',
    url: 'https://router.vuejs.org/',
    accent: 'green'
  },
  {
    name: 'Element Plus',
    cat: 'UI 组件',
    desc: '企业级组件库，统一界面交互风格',
    url: 'https://element-plus.org/',
    accent: 'green'
  },
  {
    name: 'Axios',
    cat: 'HTTP',
    desc: '请求客户端，统一拦截与 Token 鉴权',
    url: 'https://axios-http.com/',
    accent: 'green'
  }
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
  display: block;
  padding: 16px;
  border-radius: 10px;
  background: #f9fbf7;
  border: 1px solid #eef4e6;
  text-decoration: none;
  color: inherit;
  cursor: pointer;
  transition: all 0.3s ease;
}

.tech-card:hover {
  border-color: #8BC34A;
  box-shadow: 0 4px 12px rgba(139, 195, 74, 0.2);
  transform: translateY(-2px);
}

.tech-card:focus-visible {
  outline: 2px solid #8BC34A;
  outline-offset: 2px;
}

.tech-card--red {
  border: 2px solid #ef5350;
  background: #fff8f8;
}

.tech-card--red:hover {
  border-color: #e53935;
  box-shadow: 0 4px 12px rgba(229, 57, 53, 0.22);
}

.tech-card--red:focus-visible {
  outline-color: #ef5350;
}

.tech-card--blue {
  border: 2px solid #42a5f5;
  background: #f5faff;
}

.tech-card--blue:hover {
  border-color: #1e88e5;
  box-shadow: 0 4px 12px rgba(30, 136, 229, 0.22);
}

.tech-card--blue:focus-visible {
  outline-color: #42a5f5;
}

.tech-card--green {
  border: 2px solid #66bb6a;
  background: #f6fbf6;
}

.tech-card--green:hover {
  border-color: #43a047;
  box-shadow: 0 4px 12px rgba(67, 160, 71, 0.22);
}

.tech-card--green:focus-visible {
  outline-color: #66bb6a;
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
