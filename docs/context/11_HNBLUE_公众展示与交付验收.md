# HNBLUE 公众展示与交付验收记录

## 需求编号
HNBLUE-PUBLIC-DELIVERY-20260712

## 修改目标
完成正式交付前的界面内容清理、AI 碳助手交互优化、流式 Markdown 渲染、地图主岛/三沙拆分、指标卡单位与价值说明、来源中文化、治理工作台分页和交付审查脚本建设。

## 修改文件
- `frontend/src/config/publicPresentationPolicy.js`
- `frontend/src/utils/aiStreamParser.js`
- `frontend/src/components/CarbonAiFloatingAssistant.vue`
- `frontend/src/components/HomePage.vue`
- `frontend/src/components/AboutPage.vue`
- `frontend/src/components/CarbonSeek.vue`
- `frontend/src/components/HainanBlueCarbonMap.vue`
- `frontend/src/components/PublicDataPage.vue`
- `frontend/src/components/V2GovernmentPage.vue`
- `frontend/src/components/MapExplorePage.vue`
- `frontend/src/components/Login.vue`
- `frontend/src/components/LiteratureCompare.vue`
- `frontend/src/components/common/StatCard.vue`
- `frontend/src/components/common/DataTable.vue`
- `frontend/src/components/common/SourceBadge.vue`
- `frontend/src/components/common/DetailDrawer.vue`
- `frontend/src/components/common/PaginationBar.vue`
- `frontend/scripts/audit-public-presentation.mjs`
- `frontend/package.json`

## 验证方式
1. 执行 `npm run audit:delivery`，检查普通界面禁止展示的版本、迁移、开发路线和技术标签表述。
2. 执行 `npm run build`，验证 Vue SFC、路由、组件引用和前端构建。
3. 源码复查 AI 助手收起形态、SSE delta 追加、地图视图筛选和治理工作台每页 10 条分页。

## 验收结果
`npm run audit:delivery` 已通过：未发现禁止展示的版本、迁移、开发路线和技术标签表述。`npm run build` 已通过：构建完成，输出 `dist` 目录；保留资源体积和 browserslist 数据过期警告。

## 尚存边界
本轮不启动 Spring Boot、AnythingLLM、Flask、MySQL 或 Redis；后端接口协议和数据库结构保持不变。地图拆分基于现有 GeoJSON 中的真实要素，不自行生成岛礁名称或坐标。
