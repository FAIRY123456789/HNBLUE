const { defineConfig } = require('@vue/cli-service');
const apiProxyTarget = process.env.VUE_APP_API_PROXY_TARGET || 'http://localhost:8088';

module.exports = defineConfig({
  publicPath: process.env.VUE_APP_PUBLIC_PATH || '/',
  transpileDependencies: ['@kousum/semi-ui-vue'],
  productionSourceMap: false,
  devServer: {
    historyApiFallback: true,
    proxy: {
      '/api': {
        target: apiProxyTarget,
        changeOrigin: true,
      },
      '/user/info': {
        target: apiProxyTarget,
        changeOrigin: true,
      },
      '/user/updateInfo': {
        target: apiProxyTarget,
        changeOrigin: true,
      },
      '/user/avatar': {
        target: apiProxyTarget,
        changeOrigin: true,
      },
      '/admin': {
        target: apiProxyTarget,
        changeOrigin: true,
      },
    },
  },
});
