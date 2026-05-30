<script setup lang="ts">
</script>

<template>
  <router-view v-slot="{ Component }">
    <transition name="fade" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
</template>

<style>
:root {
  --primary: #ff6b35;
  --primary-light: #fff0e8;
  --primary-gradient: linear-gradient(135deg, #ff6b35, #ff8c5a);
  --success: #07c160;
  --danger: #ee0a24;
  --warning: #ff976a;
  --info: #1989fa;
  --bg: #f5f5f5;
  --card-bg: #ffffff;
  --text: #1a1a1a;
  --text-secondary: #999999;
  --text-muted: #cccccc;
  --radius: 12px;
  --radius-sm: 8px;
  --radius-lg: 16px;
  --radius-round: 25px;
  --shadow-sm: 0 1px 4px rgba(0,0,0,0.04);
  --shadow: 0 2px 12px rgba(0,0,0,0.08);
  --shadow-lg: 0 8px 32px rgba(0,0,0,0.10);

  --van-primary-color: var(--primary);
  --van-danger-color: var(--danger);
  --van-success-color: var(--success);
  --van-warning-color: var(--warning);
  --van-text-color: var(--text);
  --van-text-color-regular: #666;
}

* { margin: 0; padding: 0; box-sizing: border-box; }
html { -webkit-font-smoothing: antialiased; -moz-osx-font-smoothing: grayscale; }
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  background: var(--bg);
  color: var(--text);
  line-height: 1.5;
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
