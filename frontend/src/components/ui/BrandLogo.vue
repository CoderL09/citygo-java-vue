<script setup lang="ts">
withDefaults(defineProps<{ light?: boolean; subtitle?: string }>(), {
  light: false,
  subtitle: '城市活动',
})
</script>
<template>
  <span class="brand-logo" :class="{ 'is-light': light }">
    <span class="brand-symbol" aria-hidden="true"
      ><svg viewBox="0 0 32 32" fill="none">
        <path
          d="M23 9a10 10 0 1 0 0 14"
          stroke="currentColor"
          stroke-width="4.5"
          stroke-linecap="round"
        />
        <path d="m19 16 7-4v8l-7-4Z" fill="currentColor" /></svg
    ></span>
    <span class="brand-word"
      >CityGo<span class="brand-caption">{{ subtitle }}</span></span
    >
  </span>
</template>
<style scoped>
.brand-logo {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--city-deep);
  white-space: nowrap;
}
.brand-symbol {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: var(--city-pine);
  color: #fff;
}
.brand-symbol svg {
  width: 28px;
  height: 28px;
}
.brand-word {
  font-size: 25px;
  font-weight: 750;
  letter-spacing: -1px;
  line-height: 1.1;
}
.brand-caption {
  display: block;
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 2px;
  margin-top: 5px;
  opacity: 0.7;
}
.is-light {
  color: #fff;
}
.is-light .brand-symbol {
  background: #d5e9bc;
  color: #224b37;
}
</style>
