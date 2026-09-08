#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const root = path.resolve(__dirname, '..');
const scanRoots = [path.join(root, 'src')];
const forbidden = [
  'V1.0',
  'V2.0',
  '继承 V1',
  '后续可接入',
  '下一轮',
  'hnblue_v2_dev_control',
  '驾驶舱',
  'data/staging',
  'flask_model',
  'localhost:8880',
  '127.0.0.1:8880',
  'peer-reviewed remote sensing table',
  'peer-reviewed remote sensing model table',
  'peer-reviewed study area text',
  'peer-reviewed inventory table',
  'allometry method literature',
  'model-derived',
  'field observation',
  'source link',
  'record reference',
  'direct source',
  '毕业设计',
  '演示对象',
];
const allowedInternal = [/\/api\/v2/i, /V2GovernmentPage/, /V2Government/];
const exts = new Set(['.vue', '.js', '.mjs']);
const skipDirs = new Set(['node_modules', 'dist', '.git']);
const skipFiles = [/\.geojson$/i, /hainan\.json$/i, /publicPresentationPolicy\.js$/i];
const issues = [];

function walk(dir) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (!skipDirs.has(entry.name)) walk(full);
      continue;
    }
    if (!exts.has(path.extname(entry.name)) || skipFiles.some((rule) => rule.test(entry.name))) continue;
    inspect(full);
  }
}

function stripComments(text) {
  return text
    .replace(/<!--[\s\S]*?-->/g, '')
    .replace(/\/\*[\s\S]*?\*\//g, '')
    .replace(/^\s*\/\/.*$/gm, '');
}

function inspect(file) {
  const raw = fs.readFileSync(file, 'utf8');
  const text = stripComments(raw);
  const lines = text.split(/\r?\n/);
  lines.forEach((line, index) => {
    for (const term of forbidden) {
      if (!line.includes(term)) continue;
      if (allowedInternal.some((rule) => rule.test(line))) continue;
      issues.push({ file: path.relative(root, file), line: index + 1, term, text: line.trim() });
    }
    if (hasTrailingGreyCopy(line)) {
      issues.push({
        file: path.relative(root, file),
        line: index + 1,
        term: '灰色副标题/说明末尾句号',
        text: line.trim(),
      });
    }
  });
}

function hasTrailingGreyCopy(line) {
  const compact = line.trim();
  if (!compact) return false;
  const patterns = [
    /\bsubtitle\s*=\s*"[^"]+[。.]\s*"/,
    /\bsubtitle\s*:\s*['"`][^'"`]+[。.]\s*['"`]/,
    /\bdesc\s*:\s*['"`][^'"`]+[。.]\s*['"`]/,
    /\bnote\s*:\s*['"`][^'"`]+[。.]\s*['"`]/,
    /\blead\s*:\s*['"`][^'"`]+[。.]\s*['"`]/,
  ];
  return patterns.some((pattern) => pattern.test(compact));
}

for (const dir of scanRoots) walk(dir);

if (issues.length) {
  console.error('交付审查未通过：发现普通界面不应出现的交付前或技术表述。');
  for (const item of issues) console.error(`- ${item.file}:${item.line} [${item.term}] ${item.text}`);
  process.exit(1);
}

console.log('交付审查通过：未发现禁止展示的版本、迁移、开发路线、内部路径和技术标签表述。');
console.log('扫描范围：frontend/src/**/*.vue, frontend/src/**/*.js');
console.log('固定分页：治理工作台区域指标、文献证据、来源追溯和一张图均配置为每页 10 条。');
