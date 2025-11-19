<!-- 
文献比对模块组件 - BAAD碳储区间参考验证器

功能概述：
• 用户输入碳储量估算值与BAAD数据库参考区间进行比对
• 可视化显示估算值在文献区间中的相对位置
• 自动生成生态学解释和合理性评估
• 提供BAAD数据集的科学背景说明

核心功能：
• 数值比对：将用户输入与BAAD参考区间对比
• 可视化反馈：使用颜色条显示相对位置
• 智能解释：根据比对结果生成生态学解释
• 数据溯源：展示BAAD数据集的权威性和合法性

技术特点：
• 响应式布局适配不同屏幕
• 动态颜色反馈和动画效果
• 实时计算结果和解释生成
-->
<template>
    <div class="literature-compare-container">
        <h2 class="section-title">📚 文献比对模块 · BAAD 碳储区间参考验证器</h2>
        <!-- 模块功能描述 -->
        <p class="module-description">
            文献比对模块用于将用户估算的生物量（t/ha）与全球公认的结构-生物量数据库 <b>BAAD</b>（Biomass And Allometry Database）中“类红树林”样本的参考区间进行对比验证。系统从
            BAAD
            数据集中提取相似生态类型样本，计算其生物量区间范围，并据此生成比对图和生态解释。用户输入估算值后，即可直观看到其在文献参考区间中的相对位置（过低、正常、偏高等），并获得自动生成的生态学解释，辅助判断估算结果的合理性与生态意义。本模块适用于模型结果验证、生态合理性分析与政策报告编制等场景。
        </p>

        <!-- 主要内容网格布局 -->
        <div class="grid-wrapper">
            <!-- 左侧：输入模块 -->
            <el-card class="input-card" shadow="hover">
                <p class="hint-text">请填写你的估算碳储量（单位：t/ha）：</p>
                <!-- 输入区域 -->
                <div class="input-row">
                    <el-input-number v-model="userValue" :min="0" :step="0.1" placeholder="请输入估算值" />
                    <el-button type="primary" @click="compareValue" :disabled="!userValue">比对</el-button>
                </div>
                <!-- 比对结果展示 -->
                <div v-if="rangeLoaded" class="result-area">
                    <h3>🔬 参考区间统计</h3>
                    <p>文献参考区间（基于 BAAD 数据集类红树林样本）：<b>{{ range.min }} ~ {{ range.max }} t/ha</b></p>
                    <p>你输入的估算值：<b>{{ userValue }} t/ha</b></p>
                    <!-- 可视化比对条 -->
                    <div class="bar-chart">
                        <div class="bar-track">
                            <div class="bar-fill" :style="{ left: offsetLeft + '%', background: resultColor }"></div>
                        </div>
                        <div class="bar-labels">
                            <span>{{ range.min }}</span>
                            <span>{{ range.max }}</span>
                        </div>
                    </div>
                    <!-- 生态学解释 -->
                    <div class="explanation">
                        <p v-html="explanationText"></p>
                    </div>
                </div>
            </el-card>

            <!-- 右侧：数据集介绍模块 -->
            <el-card class="desc-card" shadow="hover">
                <h3>📘 数据来源：BAAD 数据集</h3>
                <p>
                    <b>BAAD</b>（Biomass And Allometry Database）是全球最权威的木本植物结构与生物量数据库之一，
                    包含 <b>259,634 条记录</b>，涵盖 <b>678 种植物</b>，来源于 <b>176 篇文献</b>。
                </p>
                <p>
                    数据特点包括：直接测量单株个体、标准化单位转换、覆盖全球主要植被类型。系统使用其中的类红树林样本数据
                    进行碳储参考区间统计，作为碳储估算结果的科学对照。
                </p>
                <p class="legal-note">✅ 本系统已合法使用该数据集，数据仅用于科研与决策辅助，不涉及商业用途。</p>
            </el-card>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';

// 响应式数据定义
const userValue = ref(null);           // 用户输入的估算值
const range = ref({ min: 0, max: 0 }); // BAAD参考区间
const rangeLoaded = ref(false);        // 数据加载状态
const offsetLeft = ref(0);             // 可视化条位置
const resultColor = ref('#409EFF');    // 结果颜色反馈
const explanationText = ref('');       // 生态学解释文本

/**
 * 组件挂载时获取BAAD参考区间数据
 */
onMounted(async () => {
    try {
        const res = await fetch('http://localhost:8880/api/literature-range');
        const data = await res.json();
        range.value = data;
        rangeLoaded.value = true;
    } catch (error) {
        console.error('获取文献区间数据失败:', error);
        ElMessage.error('文献数据加载失败');
    }
});

/**
 * 执行数值比对分析
 * 计算用户输入值在参考区间中的相对位置
 * 生成相应的颜色反馈和生态学解释
 */
const compareValue = () => {
    const { min, max } = range.value;
    const value = parseFloat(userValue.value);

    // 输入验证
    if (isNaN(value)) {
        ElMessage.warning('请输入有效的估算值');
        return;
    }

    // 计算相对位置（0-100%）
    const ratio = ((value - min) / (max - min)) * 100;
    offsetLeft.value = Math.max(0, Math.min(ratio, 100));

    // 根据位置生成反馈
    if (value < min) {
        // 低于参考区间
        resultColor.value = '#F56C6C';
        explanationText.value = `⚠️ 你的估算值低于文献区间，可能表示结构参数对应的样地碳储偏低，建议检查数据合理性。`;
    } else if (value > max) {
        // 高于参考区间
        resultColor.value = '#E6A23C';
        explanationText.value = `📈 你的估算值超出文献区间，可能代表高碳储潜力结构组合，也可能存在异常值，需结合结构变量解释。`;
    } else if (value > min + (max - min) * 0.7) {
        // 区间内高值区域
        resultColor.value = '#67C23A';
        explanationText.value = `✅ 估算值位于区间内的 <b>高碳储区域</b>，与 BAAD 文献中天然红树林样地一致性较好，碳汇潜力优良。`;
    } else {
        // 区间内正常区域
        resultColor.value = '#409EFF';
        explanationText.value = `🟢 估算值落在文献区间内，为正常碳储范围，模型结果具有参考价值。`;
    }
};
</script>

<style scoped>
.literature-compare-container {
    padding: 40px;
    background: #f6f9fc;
    min-height: 100vh;
}

/* 标题样式 */
.section-title {
    font-size: 28px;
    color: #2c3e50;
    margin-bottom: 25px;
    text-align: center;
}

/* 横向排列的内容区域 */
.grid-wrapper {
    display: flex;
    gap: 30px;
    justify-content: space-between;
    flex-wrap: wrap;
}

/* 卡片通用样式 */
.input-card,
.desc-card {
    flex: 1;
    min-width: 360px;
    padding: 20px;
}

/* 比对图 */
.bar-track {
    height: 16px;
    background: #e4e7ed;
    border-radius: 8px;
    position: relative;
    margin-top: 10px;
}

.bar-fill {
    width: 6px;
    height: 24px;
    position: absolute;
    top: -4px;
    transition: left 0.3s ease;
}

.bar-labels {
    display: flex;
    justify-content: space-between;
    font-size: 14px;
    color: #888;
    margin-top: 6px;
}

/* 解释区域 */
.explanation {
    font-size: 16px;
    line-height: 1.6;
    background: #f2fdf3;
    padding: 15px;
    border-left: 4px solid #2d8cf0;
    border-radius: 8px;
    margin-top: 15px;
}

/* 数据集说明样式 */
.legal-note {
    margin-top: 20px;
    font-size: 14px;
    color: #666;
    background: #f9f9f9;
    padding: 10px 12px;
    border-left: 4px solid #bbb;
    border-radius: 6px;
}

.input-row {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 12px;
    /* 控制间距大小，可调 */
    margin-top: 10px;
    margin-bottom: 10px;
}

.module-description {
    margin: 10px 0 30px 0;
    font-size: 16px;
    color: #444;
    line-height: 1.6;
    background-color: #f7f9fb;
    padding: 12px 16px;
    border-left: 5px solid #409EFF;
    border-radius: 4px;
}
</style>