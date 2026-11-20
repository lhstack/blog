<script setup>
import { RouterView } from 'vue-router'
import AuroraBackground from '@/components/effects/AuroraBackground.vue'
// 全局背景和点击特效。
import ClickBurst from '@/components/effects/ClickBurst.vue'
</script>

<template>
  <div class="app-root">
    <!-- 全局视觉层：背景动画 / 点击特效 -->
    <AuroraBackground />
    <ClickBurst />

    <RouterView v-slot="{ Component }">
      <Transition name="route-fade" mode="out-in">
        <component :is="Component" />
      </Transition>
    </RouterView>
  </div>
</template>
