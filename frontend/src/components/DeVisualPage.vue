<!--
区域详情分析页面组件 - 展示特定区域蓝碳资源详细信息

功能概述：
• 展示区域基础信息概览卡片
• 7大分析模块：碳储量/通量趋势、通量类型对比、多碳指标分析、多年趋势、通量影响因子、物种分布、经济价值估算
• 支持图表交互和PDF报告导出

模块说明：
① 城市基础信息概览卡片
② 碳储量/碳通量变化趋势（支持年份筛选）
③ 碳通量类型对比（CO₂/CH₄/N₂O/GHG）
④ 多碳指标对比分析（雷达图+柱状图）
⑤ 多年碳储量与碳通量趋势
⑥ 通量影响因子分析（散点图+趋势线）
⑦ 优势物种分布分析（南丁格尔玫瑰图）
⑧ 蓝碳经济价值估算（50年情景分析）

数据来源：
• 通过API动态获取区域数据
• 支持多维度数据可视化展示
-->
<template>
  <div class="devisual-container">
    <!-- 顶部标题与返回按钮 -->
    <div class="header">
      <button @click="goBack">← 返回</button>
      <h1>{{ name }} 蓝碳资源详情分析</h1>
      <button class="export-btn" @click="exportPDF">导出报告</button>
    </div>

    <!-- ① 城市基础信息概览卡片 -->
    <section class="city-info-cards">
      <div class="info-card">
        <h3>地区名称</h3>
        <p>{{ name }}</p>
      </div>
      <div class="info-card">
        <h3>优势物种</h3>
        <p>{{ regionInfo.dominantSpecies }}</p>
      </div>
      <div class="info-card">
        <h3>红树林面积</h3>
        <p>{{ regionInfo.mangroveArea }} ha</p>

      </div>
      <div class="info-card">
        <h3>森林年龄</h3>
        <p>{{ regionInfo.forestAge }} 年</p>

      </div>
      <div class="info-card">
        <h3>碳氮比</h3>
        <p>{{ regionInfo.cnRatio }}</p>

      </div>
    </section>

    <!-- ② 碳储量、碳通量变化趋势 -->
    <h2>一、碳储量、碳通量变化趋势</h2>
    <div class="info-panel">
      <p>
        当前区域：<strong>{{ name }}</strong>，请选择时间区间查看碳储量和碳通量变化趋势：
        <select v-model="startYear" :style="selectStyle">
          <option v-for="year in years" :key="'start-' + year" :value="year">{{ year }}</option>
        </select>
        年 ~
        <select v-model="endYear" :style="selectStyle">
          <option v-for="year in years" :key="'end-' + year" :value="year">{{ year }}</option>
        </select>
        年
      </p>
      <p v-if="yearWarning" class="warning-text">{{ yearWarning }}</p>
    </div>

    <!-- 图表类型切换 -->
    <div class="chart-toggle">
      <button :class="{ active: chartType === 'bar' }" @click="chartType = 'bar'">柱状图</button>
      <button :class="{ active: chartType === 'line' }" @click="chartType = 'line'">折线图</button>
    </div>
    <div ref="chartContainer" class="chart-container"></div>

    <!-- ③ 碳通量类型对比 -->
    <section class="flux-area-chart">
      <h2>二、碳通量类型对比（CO₂ / CH₄ / N₂O / GHG）</h2>
      <div ref="fluxLineContainer" class="chart-container"></div>
    </section>

    <!-- ④ 多碳指标对比分析 -->
    <section class="carbon-metrics-comparison">
      <h2>三、多碳指标对比分析</h2>
      <div class="carbon-charts-row">
        <div class="chart-container half" ref="carbonRadarContainer"></div>
        <div class="chart-container half" ref="carbonBarContainer"></div>
      </div>
    </section>


    <!-- ⑤ 多年趋势图 -->
    <section class="trend-analysis">
      <h2>四、多年碳储量与碳通量趋势</h2>
      <div ref="trendChartContainer" class="chart-container"></div>
    </section>

    <!-- ⑥ 通量影响因子分析 -->
    <section class="flux-scatter-select">
      <h2>五、通量影响因子分析</h2>
      <label for="factor-select">选择影响因子：</label>
      <select id="factor-select" v-model="selectedFactor" :style="selectStyle">
        <option value="age">森林年龄 (年)</option>
        <option value="rainfall">降雨量 (mm)</option>
        <option value="salinity">盐度 (‰)</option>
        <option value="temp">温度 (°C)</option>
      </select>
      <div ref="fluxFactorChartRef" class="chart-container" style="margin-top: 20px;"></div>
    </section>

    <!-- ⑦ 优势物种分布分析 -->
    <section class="species-pie-chart">
      <h2>六、优势物种分布分析</h2>
      <div ref="speciesChartRef" class="chart-container"></div>
    </section>

    <!-- ⑧ 蓝碳经济价值估算 -->
    <h2>七、蓝碳经济价值估算（50年情景分析）</h2>
    <section class="economic-section">
      <div ref="economyChartRef" class="economic-chart"></div>
    </section>

    <!-- 经济价值估算公式说明 -->
    <div class="economic-section">
      <h2 class="section-title">注：经济价值估算公式</h2>

      <div class="formula-card">
        <p class="center-text"><strong>蓝碳净现值（PVBC） = 碳储现值（PVCS） - 碳损失现值（PVCL）</strong></p>
        <p class="center-text">
          <strong>PVCS = Σ [ V<sub>t</sub> × (A<sub>t</sub> × C<sub>b</sub>) ] / (1 + r)<sup>t</sup>,
            PVCL = Σ [ V<sub>t</sub> × (ΔA<sub>t</sub> × C<sub>f</sub>) ] / (1 + r)<sup>t</sup></strong>
        </p>
        <p class="center-text" style="margin-top: 10px;">
          <em>
            V<sub>t</sub> = 碳价格（$/tC），A<sub>t</sub> = 某年蓝碳生态系统面积（ha），ΔA<sub>t</sub> = 某年面积变化（ha）<br />
            C<sub>b</sub> = 单位面积碳埋藏速率（tC/ha），C<sub>f</sub> = 单位面积碳释放量（tC/ha），r = 折现率，t = 年份区间
          </em>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import * as echarts from 'echarts';
import { onMounted, ref, watch, computed } from 'vue';
import { useRouter } from 'vue-router';
import hainanGeo from '@/assets/json/hainan.json';
import html2canvas from 'html2canvas'
import jsPDF from 'jspdf'
import axios from 'axios';

const props = defineProps({
  name: {
    type: String,
    required: true
  }
});
const router = useRouter();

// 响应式数据定义
const selectedYear = ref('2020');
const years = Array.from({ length: 10 }, (_, i) => (2015 + i).toString());
const chartType = ref('bar');
const startYear = ref('2015');
const endYear = ref('2024');
const chartContainer = ref(null);
let chartInstance = null;
const yearWarning = ref('');
const cityChartData = ref({});
const regionInfo = ref({
  regionName: '',
  dominantSpecies: '',
  mangroveArea: 0,
  forestAge: 0,
  cnRatio: 0
});

// 图表实例引用
// 碳通量类型对比
const fluxLineContainer = ref(null);
let fluxLineInstance = null;
const fluxData = ref([]);
// 多碳指标对比分析
const carbonRadarContainer = ref(null);
const carbonBarContainer = ref(null);
let carbonRadarInstance = null;
let carbonBarInstance = null;
const carbonMetricData = ref({
  litterfall: 0, deadwood: 0, aerialRoot: 0, soil: 0, aboveground: 0
});
// 多年趋势图表
const trendChartContainer = ref(null);
let trendChartInstance = null;
const trendData = ref([]);
// 通量影响因子分析
const fluxFactorChartRef = ref(null);
let fluxFactorChartInstance = null;
const selectedFactor = ref('age');
const fluxSamples = ref([]);
// 优势物种分布分析
const speciesChartRef = ref(null);
let speciesChartInstance = null;
const speciesPieData = ref([]);
// 蓝碳经济价值估算
const economyChartRef = ref(null);
let economyChartInstance = null;
const economyData = ref([]);

/**
 * 从后端API获取区域数据
 */
const fetchRegionData = async () => {
  try {
    const res = await axios.get(`/api/devisual/${props.name}`);
    const data = res.data;

    // 数据绑定到各个响应式变量
    regionInfo.value = data.regionInfo;
    cityChartData.value = parseCarbonChartData(data.carbonTrends); // 第二组
    fluxData.value = data.fluxComposition;                         // 第三组
    carbonMetricData.value = data.multiCarbonMetrics || {
      litterfall: 0, deadwood: 0, aerialRoot: 0, soil: 0, aboveground: 0
    };
    trendData.value = data.yearlyTrends; //  第五组数据绑定
    fluxSamples.value = data.fluxSamples;
    speciesPieData.value = data.speciesPie;
    economyData.value = data.economyProjections;

    // 渲染所有图表
    renderChart();
    renderFluxAreaChart();
    renderCarbonComparisonCharts();
    renderTrendChart();
    renderFluxScatterChart();
    renderSpeciesChart();
    renderEconomyChart();

  } catch (err) {
    console.error("数据获取失败", err);
  }
};



//一、碳储量、碳通量变化趋势
/**
 * 解析碳趋势数据
 */
function parseCarbonChartData(list) {
  const parsed = {};
  list.forEach(item => {
    parsed[item.year] = {
      carbonStorage: item.carbonStorage,
      carbonFlux: item.carbonFlux
    };
  });
  return parsed;
}
/**
 * 渲染主趋势图表（碳储量/碳通量）
 */
function renderChart() {

  if (!chartContainer.value || Object.keys(cityChartData.value).length === 0) return;
  if (!chartInstance) {
    chartInstance = echarts.init(chartContainer.value);
  }

  const start = Number(startYear.value);
  const end = Number(endYear.value);

  // 校验年份合法性
  if (start > end) {
    yearWarning.value = '⚠️ 起始年份不能大于结束年份！';
    return;
  } else {
    yearWarning.value = '';
  }

  const allData = cityChartData.value;
  const yearsList = years.filter(y => Number(y) >= start && Number(y) <= end);
  const storage = yearsList.map(y => allData[y].carbonStorage);
  const flux = yearsList.map(y => allData[y].carbonFlux);

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: params => {
        return `
          <b>${params[0].axisValue} 年</b><br/>
          ${params.map(p => `${p.seriesName}: ${p.data} t/ha`).join('<br/>')}
        `;
      }
    },
    legend: { data: ['碳储量', '碳通量'] },
    xAxis: { type: 'category', data: yearsList },
    yAxis: { type: 'value' },
    series: [
      {
        name: '碳储量',
        type: chartType.value,
        data: storage,
        itemStyle: { color: '#A5D6A7' }
      },
      {
        name: '碳通量',
        type: chartType.value,
        data: flux,
        itemStyle: { color: '#81D4FA' }
      }
    ]
  };

  chartInstance.setOption(option);
}

// 二、碳通量类型对比（CO₂ / CH₄ / N₂O / GHG），渲染面积堆叠图
/**
 * 渲染碳通量类型对比面积图
 */
const renderFluxAreaChart = () => {
  if (!fluxLineContainer.value) return;
  if (!fluxLineInstance) {
    fluxLineInstance = echarts.init(fluxLineContainer.value);
  }

  const xYears = fluxData.value.map(d => d.year);

  const areaStackOption = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#ccc',
      borderWidth: 1,
      textStyle: { color: '#333' },
      formatter: params => {
        return `<strong>${params[0].axisValue} 年</strong><br/>` +
          params.map(p => `${p.marker} ${p.seriesName}: ${p.data}`).join('<br/>');
      }
    },
    legend: {
      data: ['CO₂', 'CH₄', 'N₂O', 'GHG'],
      top: '5px'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '5%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: xYears
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: 'CO₂',
        type: 'line',
        stack: 'total',
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(144,202,249,0.8)' },
            { offset: 1, color: 'rgba(144,202,249,0.1)' }
          ])
        },
        emphasis: { focus: 'series' },
        data: fluxData.value.map(d => d.co2)
      },
      {
        name: 'CH₄',
        type: 'line',
        stack: 'total',
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(179,157,219,0.8)' },
            { offset: 1, color: 'rgba(179,157,219,0.1)' }
          ])
        },
        emphasis: { focus: 'series' },
        data: fluxData.value.map(d => d.ch4)
      },
      {
        name: 'N₂O',
        type: 'line',
        stack: 'total',
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(255,204,128,0.8)' },
            { offset: 1, color: 'rgba(255,204,128,0.1)' }
          ])
        },
        emphasis: { focus: 'series' },
        data: fluxData.value.map(d => d.n2o)
      },
      {
        name: 'GHG',
        type: 'line',
        stack: 'total',
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(255,171,145,0.8)' },
            { offset: 1, color: 'rgba(255,171,145,0.1)' }
          ])
        },
        emphasis: { focus: 'series' },
        data: fluxData.value.map(d => d.ghg)
      }
    ]
  };

  fluxLineInstance.setOption(areaStackOption);
};

// 三、多碳指标对比分析
/**
 * 渲染多碳指标对比图表（雷达图+柱状图）
 */
const renderCarbonComparisonCharts = () => {
  const data = carbonMetricData.value;
  // 雷达图渲染
  if (!carbonRadarInstance) {
    carbonRadarInstance = echarts.init(carbonRadarContainer.value);
  }
  const radarOption = {
    tooltip: {
      formatter: (params) => {
        const values = params.value;
        return `凋落物: ${values[0]} t/ha/year<br/>
            枯木: ${values[1]} t/ha<br/>
            气生根: ${values[2]} t/ha<br/>
            土壤碳: ${values[3]} t/ha<br/>
            地上碳: ${values[4]} t/ha`;
      }
    },
    radar: {
      indicator: [
        { name: '凋落物', max: 6 },
        { name: '枯木', max: 3 },
        { name: '气生根', max: 2 },
        { name: '土壤碳', max: 20 },
        { name: '地上碳', max: 25 }
      ]
    },
    series: [{
      name: '碳指标对比',
      type: 'radar',
      data: [
        {
          value: [
            data.litterfall,
            data.deadwood,
            data.aerialRoot,
            data.soil,
            data.aboveground
          ],
          name: '当前城市'
        }
      ]
    }]
  };
  carbonRadarInstance.setOption(radarOption);
  // 柱状图渲染
  if (!carbonBarInstance) {
    carbonBarInstance = echarts.init(carbonBarContainer.value);
  }
  const barOption = {
    tooltip: {
      trigger: 'axis',
      formatter: params => {
        return params.map(p => {
          const unitMap = ['t/ha/year', 't/ha', 't/ha', 't/ha', 't/ha'];
          return `${p.name}: ${p.data} ${unitMap[p.dataIndex]}`;
        }).join('<br/>');
      }
    },
    xAxis: {
      type: 'category',
      data: ['凋落物', '枯木', '气生根', '土壤碳', '地上碳']
    },
    yAxis: { type: 'value' },
    series: [{
      data: [
        data.litterfall,
        data.deadwood,
        data.aerialRoot,
        data.soil,
        data.aboveground
      ],
      type: 'bar',
      itemStyle: {
        color: (params) => {
          const colors = ['#F7C59F', '#F4A896', '#A3D2CA', '#FFE0AC', '#D9B8C4'];
          return colors[params.dataIndex % colors.length];
        },
        borderRadius: [6, 6, 0, 0]
      }
    }]
  };
  carbonBarInstance.setOption(barOption);
};


// 四、多年趋势图表
/**
 * 渲染多年趋势图表
 */
const renderTrendChart = () => {
  if (!trendChartContainer.value) return;
  if (!trendChartInstance) {
    trendChartInstance = echarts.init(trendChartContainer.value);
  }

  if (!trendData.value || trendData.value.length === 0) return;

  const xData = trendData.value.map(item => item.year);
  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: params => {
        const year = params[0].axisValue;
        const lines = params.map(p => `${p.seriesName}: ${p.data}`).join('<br/>');
        return `<strong>${year}</strong><br/>${lines}`;
      }
    },
    legend: {
      data: ['SOC', '地上生物量', 'CO₂通量', 'CH₄通量']
    },
    xAxis: { type: 'category', data: xData },
    yAxis: [
      { type: 'value', name: '碳储量 (t/ha)' },
      { type: 'value', name: '通量', position: 'right' }
    ],
    series: [
      {
        name: 'SOC',
        type: 'line',
        data: trendData.value.map(d => d.soc),
        yAxisIndex: 0
      },
      {
        name: '地上生物量',
        type: 'line',
        data: trendData.value.map(d => d.biomass),
        yAxisIndex: 0
      },
      {
        name: 'CO₂通量',
        type: 'line',
        data: trendData.value.map(d => d.co2Flux),
        yAxisIndex: 1
      },
      {
        name: 'CH₄通量',
        type: 'line',
        data: trendData.value.map(d => d.ch4Flux),
        yAxisIndex: 1
      }
    ]
  };
  trendChartInstance.setOption(option);
};

// 五、通量影响因子分析（下拉切换 + 散点图 + 趋势线）
/**
 * 通量影响因子分析配置
 */
const fluxFactorMeta = {
  temp: { label: '温度 (°C)', color: '#F64044' },
  rainfall: { label: '降雨量 (mm)', color: '#81D4FA' },
  salinity: { label: '盐度 (‰)', color: '#FFB74D' },
  age: { label: '森林年龄 (年)', color: '#AED581' }
};
const renderFluxScatterChart = () => {
  const factorKey = selectedFactor.value;
  const meta = fluxFactorMeta[factorKey];

  // 判断数据是否有效
  if (!fluxSamples.value || fluxSamples.value.length === 0) return;

  const scatterData = fluxSamples.value
    .filter(d => d[factorKey] != null && d.flux != null)
    .map(d => [d[factorKey], d.flux]);

  if (!fluxFactorChartRef.value) return;
  if (!fluxFactorChartInstance) {
    fluxFactorChartInstance = echarts.init(fluxFactorChartRef.value);
  }

  const sorted = [...scatterData].sort((a, b) => a[0] - b[0]);
  const minX = sorted[0][0];
  const maxX = sorted[sorted.length - 1][0];
  const slope = (sorted[sorted.length - 1][1] - sorted[0][1]) / (maxX - minX || 1); // 防除0
  const trendLine = [
    [minX, sorted[0][1]],
    [maxX, sorted[0][1] + slope * (maxX - minX)]
  ];

  const option = {
    tooltip: {
      formatter: (p) => `${meta.label}: ${p.data[0]}<br/>通量: ${p.data[1]} mg/m²/h`
    },
    xAxis: { name: meta.label, type: 'value' },
    yAxis: { name: '通量 (mg/m²/h)', type: 'value' },
    series: [
      {
        name: '样本点',
        type: 'scatter',
        data: scatterData,
        itemStyle: { color: meta.color }
      },
      {
        name: '趋势线',
        type: 'line',
        symbol: 'none',
        lineStyle: { type: 'dashed', color: '#888' },
        data: trendLine
      }
    ]
  };
  fluxFactorChartInstance.setOption(option);
};

// 六、优势物种分布分析（饼图 / 南丁格尔图）
/**
 * 计算物种分布饼图数据
 */
const pieChartData = computed(() =>
  speciesPieData.value.map(item => ({
    name: item.speciesName,
    value: item.carbonRatio
  }))
);
const renderSpeciesChart = () => {
  if (!speciesChartRef.value || speciesPieData.value.length === 0) return;

  if (!speciesChartInstance) {
    speciesChartInstance = echarts.init(speciesChartRef.value);
  }

  const option = {
    title: {
      text: '优势物种分布（碳储能力占比）',
      left: 'center',
      top: 10,
      textStyle: { fontSize: 20, color: '#333' }
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}<br/>碳储占比: {d}%'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      top: 'middle',
      textStyle: { fontSize: 14 }
    },
    series: [
      {
        name: '物种占比',
        type: 'pie',
        radius: ['30%', '75%'],
        center: ['50%', '55%'],
        roseType: 'area',
        itemStyle: {
          borderRadius: 6,
          shadowBlur: 6,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.2)'
        },
        label: {
          show: true,
          fontSize: 14,
          formatter: '{b}: {d}%'
        },
        data: pieChartData.value
      }
    ]
  };
  speciesChartInstance.setOption(option);
};


// 七、蓝碳经济价值估算（参考 Ellen McHarg 等 + 海南本地情景扩展）
/**
 * 渲染经济价值估算图表
 */
const renderEconomyChart = () => {
  if (!economyChartRef.value) return;
  if (!economyChartInstance) {
    economyChartInstance = echarts.init(economyChartRef.value);
  }
  const data = economyData.value;
  const option = {
    title: {
      text: '蓝碳经济价值估算（未来50年，单位：百万美元）',
      left: 'center',
      textStyle: { fontSize: 20, color: '#333' }
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}<br/>经济价值：{c} 百万美元'
    },
    grid: {
      left: '3%',
      right: '3%',
      bottom: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: data.map(d => d.scenarioName),
      axisLabel: { fontSize: 14, rotate: 30 }
    },
    yAxis: {
      type: 'value',
      name: '百万美元',
      axisLabel: { fontSize: 14 }
    },
    series: [
      {
        name: '经济估值',
        type: 'bar',
        data: data.map(d => d.value),
        itemStyle: {
          color: (params) => {
            const colors = ['#fdd835', '#ef9a9a', '#aed581', '#81d4fa', '#ce93d8', '#ffcc80'];
            return colors[params.dataIndex % colors.length];
          },
          borderRadius: [6, 6, 0, 0]
        },
        label: {
          show: true,
          position: 'top',
          formatter: '{c}M',
          fontSize: 14
        }
      }
    ]
  };
  economyChartInstance.setOption(option);
};

/**
 * 导出PDF报告（支持全页面长截图 + 导出 PDF）
 */
const exportPDF = async () => {
  const element = document.querySelector('.devisual-container')
  if (!element) return

  const canvas = await html2canvas(element, {
    scale: 2,
    useCORS: true,
    windowWidth: document.body.scrollWidth,
    windowHeight: document.body.scrollHeight,
  })

  const imgData = canvas.toDataURL('image/png')
  const pdf = new jsPDF('p', 'mm', 'a4')

  const pageWidth = 210
  const pageHeight = 297
  const imgWidth = pageWidth
  const imgHeight = (canvas.height * imgWidth) / canvas.width

  let heightLeft = imgHeight
  let position = 0

  pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight)
  heightLeft -= pageHeight

  while (heightLeft > 0) {
    position -= pageHeight
    pdf.addPage()
    pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight)
    heightLeft -= pageHeight
  }

  pdf.save(`${props.name}-蓝碳详情报告.pdf`)
}

/**
 * 返回上一页
 */
const goBack = () => router.back();

// 组件挂载时初始化
onMounted(() => {
  fetchRegionData(); // 👈 替换原随机初始化
  renderEconomyChart();
  document.body.style.overflow = 'auto';
  document.documentElement.style.overflow = 'auto';
});

// 监听器设置
watch(chartType, renderChart);
watch(() => props.name, () => {
  fetchRegionData(); // 👈 替换原随机初始化
  renderChart();
  renderFluxAreaChart();
  renderTrendChart();
  renderCarbonComparisonCharts();
  renderFluxScatterChart();
  renderSpeciesChart();
  renderEconomyChart();
});
// 监听年份区间变化，刷新图表
watch([startYear, endYear], () => {
  renderChart();
});
watch(selectedFactor, () => {
  renderFluxScatterChart();
});

/* 下拉菜单样式配置（需配合 <style scoped> 中样式） */
const selectStyle = {
  padding: '8px 12px',
  fontSize: '16px',
  borderRadius: '8px',
  border: '1px solid #ccc',
  backgroundColor: '#f9f9f9',
  boxShadow: '0 2px 6px rgba(0,0,0,0.05)',
  cursor: 'pointer',
  transition: 'border-color 0.3s ease'
};

</script>


<style scoped>
/* 主容器样式 */
.devisual-container {
  padding: 30px;
  font-family: 'Arial', sans-serif;
  background-color: #f0f4f8;
  min-height: 100vh;
}

/* 头部样式 */
.header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}

.header button {
  padding: 8px 16px;
  background-color: #4682b4;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.header h1 {
  font-size: 35px;
  color: #333;
}

/* 信息卡片样式 */
.city-info-cards {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 20px;
  margin-bottom: 30px;
}

.info-card {
  background: white;
  padding: 16px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  width: 180px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  cursor: pointer;
}

.info-card:hover {
  transform: translateY(-5px) scale(1.05);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
}

.info-card h3 {
  font-size: 16px;
  margin-bottom: 8px;
  color: #4682b4;
}

.info-card p {
  font-size: 18px;
  font-weight: bold;
  color: #222;
}

.info-panel {
  margin-bottom: 20px;
}

.chart-toggle {
  margin-bottom: 10px;
}

.chart-toggle button {
  margin-right: 10px;
  padding: 6px 12px;
  border: 1px solid #ccc;
  border-radius: 6px;
  background-color: white;
  cursor: pointer;
  transition: background-color 0.3s ease, transform 0.2s;
}

.chart-toggle button:hover {
  background-color: #d9f3f7;
  transform: scale(1.05);
}

.chart-toggle .active {
  background-color: #00bcd4;
  color: white;
}

/* 图表容器通用样式 */
.chart-container {
  width: 100%;
  height: 400px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  margin-bottom: 30px;
}

.map-section {
  margin-top: 40px;
}

/* 经济价值图表样式 */
.economic-chart {
  width: 100%;
  height: 420px;
  margin-top: 20px;
}

.economic-section {
  margin-top: 21px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: #ffffff;
  padding: 25px 30px;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

/* 公式卡片样式 */
.formula-card {
  background: #f9f9f9;
  border-radius: 12px;
  padding: 20px 40px;
  max-width: 900px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  font-size: 18px;
  line-height: 1.8;
}

.center-text {
  text-align: center;
}

/* 双图表行样式 */
.carbon-charts-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  align-items: flex-start;
  /* 导出时强制顶部对齐 */
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  margin-bottom: 30px;
}

.chart-container.half {
  width: 480px;
  /* 明确宽度，防止跳动 */
  height: 400px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  margin: 0 auto;
  /* 居中防止导出错位 */
}

.header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
  justify-content: space-between;
  /* 右对齐按钮 */
}

/* 导出按钮样式 */
.export-btn {
  padding: 8px 16px;
  background-color: #4caf50;
  color: white;
  font-size: 15px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.export-btn:hover {
  background-color: #388e3c;
  transform: scale(1.05);
}
</style>
