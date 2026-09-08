const { defineConfig } = require('@vue/cli-service');

module.exports = defineConfig({
  transpileDependencies: ['@kousum/semi-ui-vue'],
  productionSourceMap: false,
  devServer: {
    historyApiFallback: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8088',
        changeOrigin: true,
      },
      '/user/info': {
        target: 'http://localhost:8088',
        changeOrigin: true,
      },
      '/user/updateInfo': {
        target: 'http://localhost:8088',
        changeOrigin: true,
      },
      '/user/avatar': {
        target: 'http://localhost:8088',
        changeOrigin: true,
      },
      '/admin': {
        target: 'http://localhost:8088',
        changeOrigin: true,
      },
    },
  },
});
