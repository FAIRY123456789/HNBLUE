<template>
  <footer class="platform-footer" aria-label="网站落款">
    <div class="platform-footer__inner">
      <div class="platform-footer__line platform-footer__line--primary">
        <strong>HNBLUE</strong>
        <span>成果支撑：海南蓝碳资源评估与数字化治理研究成果</span>
        <span>技术成果：北京大学深圳研究生院李瑞利老师课题组</span>
      </div>
    </div>
  </footer>
</template>

<script>
export default {
  name: 'PlatformFooter'
}
</script>

<style scoped>
.platform-footer {
  position: relative;
  z-index: 2;
  width: 100%;
  margin-top: 52px;
  color: var(--hn-text);
  background: color-mix(in srgb, var(--hn-accent) 9%, var(--hn-bg));
  border-top: 1px solid color-mix(in srgb, var(--hn-accent) 22%, var(--hn-border));
}

:global(:root[data-hn-theme="dark"]) .platform-footer,
:global(:root.theme-dark) .platform-footer {
  background: color-mix(in srgb, var(--hn-accent) 13%, var(--hn-bg));
}

.platform-footer__inner {
  width: min(1160px, calc(100% - 40px));
  margin: 0 auto;
  padding: 14px 0;
}

.platform-footer__line {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: nowrap;
  gap: 24px;
  color: var(--hn-text-muted);
  font-size: 12px;
  line-height: 1.5;
  text-align: center;
}

.platform-footer__line strong {
  color: var(--hn-accent);
  letter-spacing: 0.08em;
}

@media (max-width: 820px) {
  .platform-footer__line {
    flex-wrap: wrap;
    gap: 6px 16px;
  }
}
</style>
