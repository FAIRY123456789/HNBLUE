<!-- 
碳储-价值转换器组件 - 生物量到经济价值的转换计算工具

功能概述：
• 将生物量(m.so)转换为碳储量并计算经济价值
• 支持碳分数(CF)和碳价参数调节
• 历史记录管理和CSV导出功能
• 碳价趋势参考图表展示

计算逻辑：
生物量(m.so) × 碳分数(CF) = 碳储量(tC/ha)
碳储量 × 碳价(元/t) = 经济价值(元/ha)

设计特点：
• 横向并排布局：左侧表单计算 + 右侧趋势图表
• 响应式设计适配不同屏幕尺寸
• 实时计算结果和历史记录展示
-->
<template>
  <div class="carbon-value-container">
    <!-- 模块标题和描述 -->
    <h2 class="section-title">💰 碳储-价值转换器</h2>
    <p class="module-description">
      碳储-价值转换器模块用于将单位面积的生物量（m.so, Mg/ha）转化为碳储量，并基于设定的碳分数（Carbon Fraction, CF）与碳价（单位：元/吨）计算出相应的经济价值（单位：元/ha）。
      该模块适用于估算不同林分或样地在当前碳交易价格下的经济潜力，支持历史记录导出与趋势对比分析。右侧附有近年碳价变化趋势图，便于用户参考实际市场背景，进行灵敏度分析与政策模拟。
    </p>

    <!-- 主要内容区域：表单 + 图表横向布局 -->
    <div class="horizontal-layout">

      <!-- Part1：左侧转换器表单区域 -->
      <div class="form-box">
        <el-form :model="formData" label-width="140px" class="convert-form">
          <!-- 生物量输入 -->
          <el-form-item label="单位生物量 m.so (Mg/ha)">
            <el-input v-model.number="formData.mso" type="number" placeholder="请输入生物量" />
          </el-form-item>
          <!-- 碳分数滑块 -->
          <el-form-item label="碳分数 CF (0~1)">
            <el-slider v-model="formData.cf" :min="0.2" :max="0.6" :step="0.01" show-input />
          </el-form-item>
          <!-- 碳价输入 -->
          <el-form-item label="碳价 (元/t)">
            <el-input v-model.number="formData.price" type="number" placeholder="如 100 元/t" />
          </el-form-item>
          <!-- 操作按钮 -->
          <el-form-item>
            <el-button type="primary" @click="computeValue">计算价值</el-button>
            <el-button type="success" :disabled="!history.length" @click="exportCSV">导出记录</el-button>
          </el-form-item>
        </el-form>
        <!-- 计算结果展示 -->
        <div class="result-display" v-if="resultStr">
          <h3>🌱 结果预览</h3>
          <p v-html="resultStr" />
        </div>
        <!-- 历史记录列表 -->
        <div class="history" v-if="history.length">
          <h3>📜 历史记录</h3>
          <ul>
            <li v-for="(item, idx) in history" :key="idx">
              m.so: {{ item.mso }}，CF: {{ item.cf }}，碳价: {{ item.price }} → {{ item.value }} 元/ha
            </li>
          </ul>
        </div>
      </div>

      <!--  Part2：右侧碳价趋势图表 -->
      <div class="chart-box">
        <h3>📈 碳价趋势图</h3>
        <p class="chart-caption">参考近年碳价波动（单位：CNY/t）</p>
        <img src="../assets/carbon_price_trend_en.png" alt="Carbon Price Trend" class="carbon-price-chart" />
      </div>
    </div>
  </div>

</template>

<script setup>
import { ref, onMounted } from 'vue';

// 组件挂载时重置滚动条状态
onMounted(() => {
  document.body.style.overflow = 'auto';
  document.documentElement.style.overflow = 'auto';
});

// 响应式数据
const formData = ref({
  mso: '',
  cf: 0.48,
  price: 100
});
const resultStr = ref('');  // 计算结果字符串
const history = ref([]);  // 历史记录数组

/**
 * 计算碳储价值
 * 计算公式：碳储量 = 生物量 × 碳分数，价值 = 碳储量 × 碳价
 */
const computeValue = () => {
  const { mso, cf, price } = formData.value;
  // 输入验证
  if (!mso || !cf || !price) {
    resultStr.value = '⚠️ 请完整输入生物量、碳分数和碳价';
    return;
  }
  // 核心计算逻辑
  const carbon = mso * cf;
  const value = carbon * price;
  // 格式化结果显示
  resultStr.value = `✅ 生物量 <b>${mso}</b> Mg/ha × 碳分数 <b>${cf}</b> = 碳储量 <b>${carbon.toFixed(2)}</b> tC/ha<br>
    💰 单价 <b>${price}</b> 元/t × 碳储量 = <b style="color:green">${value.toFixed(2)} 元/ha</b>`;
  // 添加到历史记录
  history.value.unshift({ mso, cf, price, value: value.toFixed(2) });
};

/**
 * 导出历史记录为CSV文件
 * 包含BOM头确保中文兼容性
 */
const exportCSV = () => {
  const rows = [
    ['生物量(m.so)', '碳分数(CF)', '碳价(元/t)', '碳储价值(元/ha)'],
    ...history.value.map(i => [i.mso, i.cf, i.price, i.value])
  ];
  const csv = '\uFEFF' + rows.map(r => r.join(',')).join('\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.setAttribute('download', 'carbon_value_history.csv');
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};
</script>

<style scoped>
/* 主容器样式 */
.carbon-value-container {
  padding: 40px;
  background: #f3f6fb;
  min-height: 100vh;
}

/* 标题样式 */
.section-title {
  font-size: 28px;
  margin-bottom: 30px;
  color: #2c3e50;
}

/* 模块描述样式 */
.module-description {
  margin: 10px 0 20px 0;
  font-size: 16px;
  color: #444;
  line-height: 1.6;
  background-color: #f7f9fb;
  padding: 12px 16px;
  border-left: 5px solid #409EFF;
  border-radius: 4px;
}

/* 横向布局容器 */
.horizontal-layout {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  /* ✅ 垂直居中，确保中线对齐 */
  justify-content: space-between;
  gap: 40px;
}

/* 表单区域样式 */
.form-box {
  flex: 1 1 480px;
  min-width: 400px;
}

.convert-form {
  max-width: 600px;
  background: #fff;
  padding: 30px;
  border-radius: 12px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

/* 结果展示区域样式 */
.result-display {
  margin-top: 30px;
  background: #e0f8e9;
  border-left: 5px solid #2d7651;
  padding: 20px;
  border-radius: 10px;
  font-size: 16px;
}

/* 历史记录区域样式 */
.history {
  margin-top: 30px;
  background: #f2f2f2;
  padding: 20px;
  border-radius: 10px;
  font-size: 14px;
}

/* 图表区域样式 */
.chart-box {
  flex: 1 1 500px;
  min-width: 400px;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  text-align: center;
  background: #f5f8fa;

  display: flex;
  flex-direction: column;
  justify-content: center;
  /* ✅ 确保内容垂直居中 */
  height: 100%;
  /* 自动适配 flex 行高 */
}

.chart-section {
  margin-top: 40px;
  background: #f5f8fa;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  text-align: center;
}

.chart-caption {
  font-size: 14px;
  color: #666;
  margin-bottom: 12px;
}

.carbon-price-chart {
  width: 100%;
  max-width: 600px;
  border-radius: 8px;
  border: 1px solid #ddd;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}
</style>