<template>
  <article :id="section.id" class="guide-section" tabindex="-1">
    <header class="section-head">
      <div>
        <p class="section-audience">{{ section.audience }}</p>
        <h3>{{ section.title }}</h3>
        <p>{{ section.subtitle }}</p>
      </div>
      <button v-if="section.route" type="button" class="route-button" @click="$emit('navigate', section)">前往该页面</button>
    </header>

    <div class="section-route" v-if="section.route">实际入口：{{ section.route }}</div>

    <div class="guide-grid two" v-if="section.value || section.coreFunctions?.length">
      <section v-if="section.value" class="info-panel">
        <h4>解决的问题</h4>
        <p>{{ section.value }}</p>
      </section>
      <section v-if="section.coreFunctions?.length" class="info-panel">
        <h4>核心功能</h4>
        <ul>
          <li v-for="item in section.coreFunctions" :key="item">{{ item }}</li>
        </ul>
      </section>
    </div>

    <section v-if="section.components?.length" class="info-panel">
      <h4>页面组成</h4>
      <div class="component-list">
        <div v-for="item in section.components" :key="item.name" class="component-item">
          <strong>{{ item.name }}</strong>
          <span>{{ item.description }}</span>
        </div>
      </div>
    </section>

    <section v-if="section.actions?.length" class="info-panel">
      <h4>可执行操作</h4>
      <ul class="check-list">
        <li v-for="item in section.actions" :key="item">{{ item }}</li>
      </ul>
    </section>

    <section v-if="section.steps?.length" class="info-panel">
      <h4>建议操作步骤</h4>
      <ol class="step-list">
        <li v-for="item in section.steps" :key="item">{{ item }}</li>
      </ol>
    </section>

    <div class="guide-grid two" v-if="section.interpretation?.length || section.scenarios?.length">
      <section v-if="section.interpretation?.length" class="info-panel">
        <h4>结果解读</h4>
        <ul>
          <li v-for="item in section.interpretation" :key="item">{{ item }}</li>
        </ul>
      </section>
      <section v-if="section.scenarios?.length" class="info-panel">
        <h4>适用场景</h4>
        <div class="scenario-list">
          <span v-for="item in section.scenarios" :key="item">{{ item }}</span>
        </div>
      </section>
    </div>

    <section v-if="section.themePreview" class="theme-panel">
      <div class="theme-card light-card">
        <span>浅色主题</span>
        <strong>清爽、适合公开展示与日常查阅</strong>
        <p>浅色背景强调数据卡片、表格和按钮边界，便于在办公环境快速扫描。</p>
      </div>
      <div class="theme-card dark-card">
        <span>深色主题</span>
        <strong>低亮、适合长时间研判与投屏</strong>
        <p>深色背景降低视觉疲劳，保留绿色强调色和清晰的卡片层级。</p>
      </div>
    </section>

    <section v-if="section.faq?.length" class="info-panel">
      <h4>常见问题</h4>
      <div class="faq-list">
        <details v-for="item in section.faq" :key="item.q">
          <summary>{{ item.q }}</summary>
          <p>{{ item.a }}</p>
        </details>
      </div>
    </section>

    <section v-if="section.cautions?.length" class="caution-panel">
      <h4>使用边界</h4>
      <ul>
        <li v-for="item in section.cautions" :key="item">{{ item }}</li>
      </ul>
    </section>
  </article>
</template>

<script setup>
defineProps({
  section: { type: Object, required: true },
});
defineEmits(['navigate']);
</script>

<style scoped>
.guide-section {
  display: grid;
  gap: 14px;
  padding: 20px;
  border: 1px solid var(--hn-border);
  border-radius: 8px;
  background: var(--hn-panel-solid);
  box-shadow: var(--hn-shadow-soft);
  scroll-margin-top: 16px;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.section-audience,
.section-route {
  margin: 0;
  color: var(--hn-accent);
  font-size: 12px;
  font-weight: 900;
}

.section-route {
  width: fit-content;
  padding: 6px 10px;
  border: 1px solid var(--hn-border);
  border-radius: 999px;
  background: var(--hn-soft);
}

h3 {
  margin: 6px 0 8px;
  color: var(--hn-text);
  font-size: 24px;
  line-height: 1.28;
}

h4 {
  margin: 0 0 10px;
  color: var(--hn-text);
  font-size: 15px;
}

p,
li,
.component-item span,
.faq-list p {
  color: var(--hn-muted);
  line-height: 1.72;
}

p {
  margin: 0;
}

ul,
ol {
  margin: 0;
  padding-left: 20px;
}

.route-button {
  min-width: 112px;
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid var(--hn-accent);
  border-radius: 8px;
  background: var(--hn-accent);
  color: #fff;
  font-weight: 900;
  cursor: pointer;
}

.guide-grid {
  display: grid;
  gap: 12px;
}

.guide-grid.two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.info-panel,
.caution-panel {
  padding: 15px;
  border: 1px solid var(--hn-border);
  border-radius: 8px;
  background: var(--hn-surface);
}

.component-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.component-item {
  display: grid;
  gap: 5px;
  padding: 12px;
  border: 1px solid var(--hn-border);
  border-radius: 8px;
  background: var(--hn-card);
}

.component-item strong {
  color: var(--hn-text);
}

.check-list li::marker {
  color: var(--hn-accent);
}

.step-list {
  counter-reset: guide-step;
  display: grid;
  gap: 8px;
  padding-left: 0;
  list-style: none;
}

.step-list li {
  counter-increment: guide-step;
  display: grid;
  grid-template-columns: 30px 1fr;
  gap: 10px;
  align-items: start;
}

.step-list li::before {
  content: counter(guide-step);
  width: 26px;
  height: 26px;
  display: inline-grid;
  place-items: center;
  border-radius: 50%;
  background: var(--hn-soft);
  color: var(--hn-accent);
  font-size: 12px;
  font-weight: 900;
}

.scenario-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.scenario-list span {
  padding: 7px 10px;
  border: 1px solid var(--hn-border);
  border-radius: 999px;
  background: var(--hn-card);
  color: var(--hn-muted);
  font-size: 12px;
  font-weight: 800;
}

.theme-panel {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.theme-card {
  min-height: 150px;
  display: grid;
  align-content: end;
  gap: 8px;
  padding: 18px;
  border-radius: 8px;
  border: 1px solid var(--hn-border);
}

.theme-card span,
.theme-card strong {
  font-weight: 900;
}

.light-card {
  background: linear-gradient(135deg, #f7fbf9, #e8f5f1);
  color: #123f37;
}

.light-card p {
  color: #5b746d;
}

.dark-card {
  background: linear-gradient(135deg, #071f22, #12383d);
  color: #eef9f6;
}

.dark-card p {
  color: #abc8c1;
}

.faq-list {
  display: grid;
  gap: 8px;
}

.faq-list details {
  border: 1px solid var(--hn-border);
  border-radius: 8px;
  background: var(--hn-card);
  overflow: hidden;
}

.faq-list summary {
  padding: 12px;
  color: var(--hn-text);
  font-weight: 900;
  cursor: pointer;
}

.faq-list p {
  padding: 0 12px 12px;
}

.caution-panel {
  border-color: rgba(217, 155, 53, 0.34);
  background: color-mix(in srgb, var(--hn-card) 84%, #d99b35 16%);
}

.caution-panel h4 {
  color: var(--hn-blue);
}

@media (max-width: 860px) {
  .section-head,
  .guide-grid.two,
  .component-list,
  .theme-panel {
    grid-template-columns: 1fr;
  }

  .section-head {
    display: grid;
  }

  .route-button {
    width: 100%;
  }
}

:global(:root[data-hn-theme="dark"] .route-button),
:global(:root.theme-dark .route-button) {
  color: var(--hn-on-accent);
}

:global(:root[data-hn-theme="dark"] .caution-panel),
:global(:root.theme-dark .caution-panel) {
  background: rgba(217, 155, 53, 0.12);
}

:global(:root[data-hn-theme="dark"] .caution-panel h4),
:global(:root.theme-dark .caution-panel h4) {
  color: #f0cf8a;
}
</style>
