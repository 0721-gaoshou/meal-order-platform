<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from "vue";
import { mountTransferGlow, type TransferGlowOptions } from "./TransferGlowEngine.js";
import type { MotionController } from "./motion-runtime";
const props = defineProps<{ options?: TransferGlowOptions }>();
const host = ref<HTMLDivElement | null>(null);
let controller: MotionController | undefined;
onMounted(() => { controller = mountTransferGlow(host.value!, props.options); });
watch(() => props.options, value => controller?.update(value ?? {}), { deep: true });
onBeforeUnmount(() => controller?.destroy());
</script>

<template><div ref="host" data-motion-component="transfer-glow" /></template>
