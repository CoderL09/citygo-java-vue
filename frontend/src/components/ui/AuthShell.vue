<script setup lang="ts">
import BrandLogo from './BrandLogo.vue'
import cityScene from '@/assets/city-scene.svg'
defineProps<{ title: string; subtitle: string }>()
</script>
<template>
  <main class="auth-page">
    <section class="auth-story">
      <router-link to="/" aria-label="返回 CityGo 首页"><BrandLogo light /></router-link>
      <div class="story-copy">
        <span class="story-tag">出发，就在这座城</span>
        <h1>日常之外，<br />还有好多好时光。</h1>
        <p>从街角漫步到一场新体验，<br />和 CityGo 一起发现身边的精彩。</p>
      </div>
      <img :src="cityScene" alt="" class="story-city" />
      <p class="story-bottom">城市探索 · 活动预约 · 到场体验</p>
    </section>
    <section class="auth-form-side">
      <router-link to="/" class="auth-back">返回活动首页</router-link>
      <div class="auth-form-wrap">
        <div class="auth-mobile-brand"><BrandLogo /></div>
        <h2>{{ title }}</h2>
        <p class="auth-subtitle">{{ subtitle }}</p>
        <slot />
      </div>
      <p class="auth-bottom">CityGo 城市活动预约平台</p>
    </section>
  </main>
</template>
<style scoped>
.auth-page {
  min-height: 100svh;
  display: grid;
  grid-template-columns: 1fr 1fr;
  background: #fff;
}
.auth-story {
  background: #1b4c3b;
  position: relative;
  overflow: hidden;
  min-height: 760px;
  padding: 42px 10%;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}
.story-copy {
  margin: 75px 0 0;
  position: relative;
  z-index: 1;
}
.story-tag {
  color: #d0e3b5;
  font-size: 13px;
  border: 1px solid #60816a;
  padding: 6px 12px;
  border-radius: 30px;
}
.story-copy h1 {
  color: #f5f8ee;
  font-size: clamp(32px, 3.5vw, 52px);
  letter-spacing: -1.5px;
  margin: 28px 0 21px;
  line-height: 1.45;
}
.story-copy p {
  color: #c0d2c4;
  line-height: 1.9;
  font-size: 15px;
}
.story-city {
  width: 110%;
  align-self: center;
  margin-top: -10px;
  max-width: 620px;
}
.story-bottom {
  font-size: 12px;
  color: #b7cabb;
  margin: auto 0 0;
  padding-top: 16px;
}
.auth-form-side {
  display: flex;
  flex-direction: column;
  padding: 40px 9%;
  min-width: 0;
}
.auth-back {
  align-self: flex-end;
  font-size: 13px;
  color: #758278;
}
.auth-form-wrap {
  width: 100%;
  max-width: 390px;
  margin: auto;
}
.auth-form-wrap h2 {
  font-size: 29px;
  margin-bottom: 9px;
  letter-spacing: -0.6px;
}
.auth-subtitle {
  color: #7b857f;
  font-size: 14px;
  margin-bottom: 30px;
}
.auth-bottom {
  margin: 45px 0 0;
  text-align: center;
  color: #8e9891;
  font-size: 12px;
}
.auth-mobile-brand {
  display: none;
}
@media (max-width: 900px) {
  .auth-story {
    padding-left: 8%;
    padding-right: 8%;
  }
  .story-copy h1 {
    font-size: 35px;
  }
  .auth-form-side {
    padding-left: 7%;
    padding-right: 7%;
  }
}
@media (max-width: 650px) {
  .auth-page {
    display: block;
  }
  .auth-story {
    display: none;
  }
  .auth-form-side {
    min-height: 100svh;
    padding: 25px 24px;
  }
  .auth-form-wrap {
    margin: 45px auto auto;
  }
  .auth-mobile-brand {
    display: block;
    margin-bottom: 34px;
  }
  .auth-bottom {
    padding-top: 35px;
  }
  .auth-back {
    align-self: flex-start;
  }
}
</style>
