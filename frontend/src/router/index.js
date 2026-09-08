import { createRouter, createWebHistory } from "vue-router";
import { useAuth } from "@/composables/useAuth";

const HomePage = () => import("@/components/HomePage.vue");
const AboutPage = () => import("@/components/AboutPage.vue");
const CarbonSeek = () => import("@/components/CarbonSeek.vue");
const Login = () => import("@/components/Login.vue");
const PublicDataPage = () => import("@/components/PublicDataPage.vue");
const V2GovernmentPage = () => import("@/components/V2GovernmentPage.vue");
const CarbonValueConverter = () => import("@/components/CarbonValueConverter.vue");
const LiteratureCompare = () => import("@/components/LiteratureCompare.vue");
const CarbonAiOpenRedirect = () => import("@/components/CarbonAiOpenRedirect.vue");
const MapExplorePage = () => import("@/components/MapExplorePage.vue");
const StructureParameterPage = () => import("@/components/StructureParameterPage.vue");
const VirtualPlotDesignerPage = () => import("@/components/VirtualPlotDesignerPage.vue");
const UserInfo = () => import("@/components/UserInfo.vue");
const UserManage = () => import("@/components/UserManage.vue");

const PlaceholderPage = {
  props: {
    title: { type: String, default: "模块说明" },
    description: { type: String, default: "当前模块暂无可用记录，可继续查看数据资产中心" },
  },
  template: `
    <main class="placeholder-page">
      <section class="placeholder-panel">
        <h1>{{ title }}</h1>
        <p>{{ description }}</p>
        <router-link to="/v2-public-data">进入数据资产中心</router-link>
      </section>
    </main>
  `,
};

const routes = [
  { path: "/", name: "Home", component: HomePage },
  { path: "/homepage", redirect: "/" },
  { path: "/about", name: "About", component: AboutPage },
  { path: "/carbonseek", name: "CarbonSeek", component: CarbonSeek, alias: ["/carbon-trace"] },
  { path: "/login", name: "Login", component: Login },
  { path: "/v2-public-data", name: "DataAssets", component: PublicDataPage, alias: ["/data-assets", "/public-data", "/public-data-2", "/v2-data-assets"] },
  { path: "/v2-government", name: "V2Government", component: V2GovernmentPage, alias: ["/government-workbench"] },
  { path: "/visual", name: "Visual", component: MapExplorePage, alias: ["/devisual"] },
  { path: "/devisual/:name", name: "DeVisualCompat", component: MapExplorePage },
  { path: "/userinfo", name: "UserInfo", component: UserInfo, meta: { requiresAuth: true, roles: ["User", "Admin"] } },
  { path: "/usermanage", name: "UserManage", component: UserManage, meta: { requiresAuth: true, roles: ["Admin"] } },
  { path: "/structure-predictor", name: "StructurePredictor", component: StructureParameterPage },
  { path: "/virtual-plot-designer", name: "VirtualPlotDesigner", component: VirtualPlotDesignerPage },
  { path: "/shap-visualizer", name: "ShapVisualizer", component: PlaceholderPage, props: { title: "模型解释图" } },
  { path: "/response-curve", name: "ResponseCurve", component: PlaceholderPage, props: { title: "响应曲线图" } },
  { path: "/param-sensitivity", name: "ParamSensitivity", component: PlaceholderPage, props: { title: "参数敏感性分析" } },
  { path: "/carbon-value-converter", name: "CarbonValueConverter", component: CarbonValueConverter },
  { path: "/literature-compare", name: "LiteratureCompare", component: LiteratureCompare },
  { path: "/ai-assistant", name: "CarbonAiOpenRedirect", component: CarbonAiOpenRedirect },
  { path: "/:pathMatch(.*)*", redirect: "/" },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 };
  },
});


router.beforeEach((to) => {
  const auth = useAuth();
  auth.restoreSession();
  if (!to.meta?.requiresAuth) return true;
  if (!auth.isAuthenticated.value) return { path: '/login', query: { redirect: to.fullPath } };
  const roles = to.meta.roles || [];
  if (!auth.hasRole(roles)) return auth.isAdmin.value ? { path: '/usermanage' } : { path: '/userinfo', query: { denied: 'admin' } };
  return true;
});
export default router;
