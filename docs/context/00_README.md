# HNBLUE V2.0 项目上下文说明

本目录用于向 Codex 或其他代码代理提供 HNBLUE 项目的背景材料。材料来源包括用户已上传的竞品分析、文昌示范区阶段性建设说明、未来建设路线、本科毕业论文、海南数字化平台实施方案以及数据库总体设计图。

使用方式建议如下。首先让代码代理阅读本目录下全部 Markdown 文件，再读取项目根目录中的 README、前端与后端代码、数据库脚本和运行说明。随后要求代码代理基于现有工程完成“可部署、可演示、可持续扩展”的 V2.0 重构，而不要从零重写系统。

本目录下文件的功能如下。

（1）`01_project_positioning.md` 说明 HNBLUE 的项目定位、政策背景和阶段目标。

（2）`02_existing_system_from_thesis.md` 提炼本科毕业论文中已经实现的系统能力，便于代码代理理解 V1.0 的技术基础。

（3）`03_v2_requirements_wenchang_demo.md` 汇总文昌示范区版 V2.0 需求，强调数据接入、展示口径、AI 碳助手和工程边界。

（4）`04_database_and_data_context.md` 说明数据库总体设计、已知数据类别和后续数据治理方向。

（5）`05_competitor_and_product_strategy.md` 提炼竞品分析结论，用于约束产品定位和展示重点。

（6）`06_roadmap_and_delivery_plan.md` 给出阶段路线、优先级和验收口径。

（7）`07_known_constraints.md` 明确当前不能过度承诺的部分，包括 WebGIS、真实数据、通量塔连续数据和多服务本地启动复杂度。

（8）`08_codex_working_prompt.md` 是可以直接复制给 Codex 的工作提示词。

`assets/database_architecture.png` 为用户提供的数据库总体设计图，展示了 WebGIS 平台、海南蓝碳系统生态碳汇数据库、可视化分析、资料收集及五类数据来源之间的关系。
