<script setup>
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import { Carousel } from 'ant-design-vue'
import { LeftOutlined, RightOutlined } from '@ant-design/icons-vue'
import ArticleCard from './ArticleCard.vue'

const props = defineProps({ articles: { type: Array, required: true } })
const motion = ref(false)
let preference
function syncMotion() { motion.value = !preference.matches }
onMounted(() => {
  preference = window.matchMedia('(prefers-reduced-motion: reduce)')
  syncMotion()
  preference.addEventListener('change', syncMotion)
})
onBeforeUnmount(() => preference?.removeEventListener('change', syncMotion))

function settings(slides) {
  const overflow = props.articles.length > slides
  return { slidesToShow: Math.min(slides, props.articles.length), infinite: overflow, arrows: overflow, dots: overflow, autoplay: overflow && motion.value }
}
const desktop = computed(() => settings(3))
const responsive = computed(() => [
  { breakpoint: 1024, settings: settings(2) },
  { breakpoint: 640, settings: settings(1) }
])
</script>

<template>
  <div class="pinned-carousel">
    <div class="carousel-heading"><slot name="heading" /></div>
    <Carousel class="recommendation-carousel" v-bind="desktop"
      :slides-to-scroll="1" :responsive="responsive" :autoplay-speed="5000"
      pause-on-hover pause-on-focus draggable swipe-to-slide accessibility>
      <template #prevArrow>
        <button type="button" class="recommendation-arrow" aria-label="上一张置顶文章">
          <LeftOutlined />
        </button>
      </template>
      <template #nextArrow>
        <button type="button" class="recommendation-arrow" aria-label="下一张置顶文章">
          <RightOutlined />
        </button>
      </template>
      <div v-for="item in articles" :key="item.id" class="recommendation-slide">
        <ArticleCard :article="item" featured />
      </div>
    </Carousel>
  </div>
</template>

<style scoped>
.pinned-carousel { min-width: 0; }
.carousel-heading { margin-bottom: 16px; }
.recommendation-carousel { padding-bottom: 24px; }
.recommendation-carousel :deep(.slick-list) { margin-inline: -8px; }
.recommendation-carousel :deep(.slick-track) { display: flex; }
.recommendation-carousel :deep(.slick-slide) { float: none; height: auto; }
.recommendation-carousel :deep(.slick-slide > div) { height: 100%; }
.recommendation-slide { height: 100%; padding: 6px 8px 12px; }
.recommendation-carousel :deep(.slick-slider .slick-dots) { bottom: -12px; }
.recommendation-carousel :deep(.slick-dots li button) { background: var(--brand-1); }
.recommendation-carousel :deep(.recommendation-arrow) {
  z-index: 2;
  width: 36px;
  height: 36px;
  margin-top: -18px;
  border: 1px solid var(--surface-border);
  border-radius: 50%;
  color: var(--brand-1);
  background: var(--surface-solid);
  box-shadow: var(--shadow-soft);
  font-size: 16px;
  line-height: 34px;
  text-align: center;
}
.recommendation-carousel :deep(.recommendation-arrow::before) { display: none; }
.recommendation-carousel :deep(.slick-prev) { left: -18px; }
.recommendation-carousel :deep(.slick-next) { right: -18px; }
.recommendation-carousel :deep(.slick-disabled) { opacity: 0.4; cursor: default; }
@media (max-width: 640px) {
  .recommendation-carousel :deep(.slick-prev) { left: 0; }
  .recommendation-carousel :deep(.slick-next) { right: 0; }
}
</style>
