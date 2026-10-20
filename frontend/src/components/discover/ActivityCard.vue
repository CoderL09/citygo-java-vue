<script setup lang="ts">
import { Location, ArrowRight } from '@element-plus/icons-vue'
import type { ActivityView } from '@/api/types'
import cityScene from '@/assets/city-scene.svg'
defineProps<{ activity: ActivityView; index: number }>()
</script>
<template>
  <article class="activity-card" :class="'cover-tone-' + (index % 3)">
    <router-link
      :to="'/activities/' + activity.id"
      class="activity-cover"
      tabindex="-1"
      aria-hidden="true"
      ><img :src="cityScene" alt="" loading="lazy" /><span class="category-sticker">{{
        activity.categoryName || '城市探索'
      }}</span></router-link
    >
    <div class="activity-body">
      <h3>
        <router-link :to="'/activities/' + activity.id">{{ activity.name }}</router-link>
      </h3>
      <p class="activity-address">
        <el-icon><Location /></el-icon><span>{{ activity.address }}</span>
      </p>
      <p class="activity-description">
        {{ activity.description || '发现城市里的新体验，挑选适合你的场次。' }}
      </p>
      <div class="activity-bottom">
        <span class="activity-kind"><span />活动预约</span
        ><router-link
          :to="'/activities/' + activity.id"
          class="detail-link"
          :aria-label="'查看详情：' + activity.name"
          >查看详情 <el-icon><ArrowRight /></el-icon
        ></router-link>
      </div>
    </div>
  </article>
</template>
<style scoped>
.activity-card {
  background: #fff;
  border: 1px solid #e5ebe5;
  border-radius: 15px;
  overflow: hidden;
  min-width: 0;
}
.activity-cover {
  display: block;
  height: 165px;
  position: relative;
  background: #d7e5d4;
  overflow: hidden;
}
.activity-cover img {
  width: 100%;
  height: 225px;
  object-fit: cover;
  object-position: center 55%;
  opacity: 0.93;
  margin-top: -17px;
}
.cover-tone-1 .activity-cover {
  background: #e9dfc9;
}
.cover-tone-1 .activity-cover img {
  filter: sepia(0.25);
}
.cover-tone-2 .activity-cover {
  background: #d6e3e5;
}
.cover-tone-2 .activity-cover img {
  filter: hue-rotate(20deg);
}
.category-sticker {
  position: absolute;
  left: 16px;
  top: 14px;
  border-radius: 6px;
  background: #fffffff0;
  color: #446044;
  padding: 4px 10px;
  font-size: 11px;
  max-width: calc(100% - 30px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.activity-body {
  padding: 21px;
}
.activity-body h3 {
  font-size: 16px;
  font-weight: 650;
  line-height: 1.65;
  margin-bottom: 9px;
  overflow-wrap: anywhere;
}
.activity-body h3 a {
  color: var(--city-ink);
}
.activity-address {
  display: flex;
  gap: 6px;
  color: #68776a;
  font-size: 11px;
  align-items: flex-start;
  margin-bottom: 12px;
}
.activity-address .el-icon {
  margin-top: 3px;
  flex-shrink: 0;
}
.activity-address span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.activity-description {
  font-size: 12px;
  color: #637263;
  line-height: 1.8;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 43px;
  margin-bottom: 19px;
}
.activity-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #eef2ed;
  padding-top: 15px;
  gap: 10px;
}
.activity-kind {
  font-size: 11px;
  color: #849482;
  display: flex;
  gap: 5px;
  align-items: center;
}
.activity-kind span {
  width: 4px;
  height: 4px;
  background: #73a16b;
  border-radius: 50%;
}
.detail-link {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 11px;
  font-weight: 650;
}
.activity-card {
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease;
}
.activity-cover img {
  transition: transform 220ms cubic-bezier(0.23, 1, 0.32, 1);
}
@media (hover: hover) and (pointer: fine) {
  .activity-card:hover {
    border-color: #aac6ad;
    box-shadow: 0 8px 25px #234e3410;
  }
  .activity-card:hover .activity-cover img {
    transform: scale(1.025);
  }
}
</style>
