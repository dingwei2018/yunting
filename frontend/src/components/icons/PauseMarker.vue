<template>
  <span :class="['pause-marker', { 'pause-marker--inline': inline }]">
    <span class="pause-marker__bar" />
    <span class="pause-marker__bar" />
  </span>
</template>

<script setup>
defineProps({
  inline: {
    type: Boolean,
    default: false
  }
})
</script>

<style scoped>
.pause-marker {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #79c0b7;
  border: 2px solid #d9d9d9;
  width: 18px;
  height: 18px;
  box-shadow: 0 2px 6px rgba(49, 64, 77, 0.25);
}

.pause-marker--inline {
  margin: 0 4px;
}

.pause-marker__bar {
  width: 2px;
  height: 8px;
  background: #d9d9d9;
  margin: 0 2px;
  border-radius: 1px;
}
</style>

