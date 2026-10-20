<script setup lang="ts">
import { ArrowRight, Location } from '@element-plus/icons-vue'
import cityScene from '@/assets/city-scene.svg'
const emit = defineEmits<{ explore: [] }>()
</script>
<template>
  <section class="discover-hero">
    <div class="hero-copy">
      <span class="hero-tag"><span aria-hidden="true" /> 城市里，总有新的发现</span>
      <h1>把周末，<br />留给这座城。</h1>
      <p>
        走一条没走过的街，赴一场期待已久的约。<br class="desktop-break" />在
        CityGo，找到你的下一段城市记忆。
      </p>
      <el-button class="hero-cta" @click="emit('explore')"
        >探索活动 <el-icon><ArrowRight /></el-icon
      ></el-button>
    </div>
    <div class="hero-art">
      <img :src="cityScene" alt="" /><span class="city-note"
        ><el-icon><Location /></el-icon>下一站，是你喜欢的生活</span
      >
    </div>
  </section>
</template>
<style scoped>
.discover-hero {
  background: #204f3d;
  border-radius: 22px;
  min-height: 360px;
  display: grid;
  grid-template-columns: 1fr 1.08fr;
  overflow: hidden;
  position: relative;
  margin-bottom: 35px;
}
.hero-copy {
  padding: 39px 0 39px 44px;
  z-index: 1;
}
.hero-tag {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 11px;
  color: #d1e0ca;
}
.hero-tag span {
  width: 6px;
  height: 6px;
  background: #ddaf66;
  border-radius: 50%;
}
.hero-copy h1 {
  font-size: 46px;
  letter-spacing: -1.5px;
  line-height: 1.25;
  color: #f2f6eb;
  margin: 22px 0 18px;
}
.hero-copy p {
  color: #c3d5c4;
  font-size: 13px;
  line-height: 1.9;
  margin-bottom: 24px;
}
.hero-copy .hero-cta {
  background: #d5e8bb;
  color: #264b32;
  border: 0;
  min-height: 42px;
  gap: 21px;
  font-size: 12px;
  padding: 12px 19px;
}
.hero-cta .el-icon {
  margin-left: 22px;
}
.hero-art {
  align-self: end;
  position: relative;
  height: 100%;
  display: flex;
  align-items: flex-end;
}
.hero-art img {
  width: 114%;
  max-width: none;
  margin-left: -7%;
  margin-bottom: -16px;
  object-fit: contain;
}
.city-note {
  position: absolute;
  bottom: 26px;
  right: 28px;
  display: flex;
  align-items: center;
  gap: 6px;
  border-radius: 30px;
  background: #f4f5e9;
  color: #37513a;
  padding: 10px 14px;
  font-size: 11px;
  box-shadow: 0 5px 20px #1b3d2e26;
}
@media (max-width: 1000px) {
  .hero-copy {
    padding: 35px 0 35px 32px;
  }
  .hero-copy h1 {
    font-size: 40px;
  }
  .discover-hero {
    min-height: 340px;
  }
  .city-note {
    right: 18px;
    bottom: 22px;
    font-size: 10px;
  }
}
@media (max-width: 650px) {
  .discover-hero {
    grid-template-columns: 1fr;
    min-height: 0;
    border-radius: 17px;
    margin-bottom: 28px;
  }
  .hero-copy {
    padding: 30px 27px 15px;
  }
  .hero-copy h1 {
    font-size: 38px;
    margin-top: 18px;
  }
  .hero-copy p {
    font-size: 12px;
  }
  .hero-art {
    height: 200px;
    justify-content: flex-end;
  }
  .hero-art img {
    height: 260px;
    width: auto;
    max-width: 100%;
    margin: 0 -15px -25px 0;
  }
  .city-note {
    bottom: 20px;
    left: 22px;
    right: auto;
    font-size: 10px;
  }
  .desktop-break {
    display: none;
  }
}
</style>
