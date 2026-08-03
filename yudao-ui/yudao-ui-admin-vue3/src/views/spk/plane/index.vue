<template>
  <ContentWrap :bodyStyle="{ padding: '0px' }" class="!mb-0">
    <IFrame :src="src" />
  </ContentWrap>
</template>
<script lang="ts" setup>
defineOptions({ name: 'SpkPlane' })

// 嵌入本地部署的 Plane 项目管理 Web UI（AIO 化后 Plane 跑在根路径 http://192.168.56.101/）。
// 首次进入在 iframe 内用 Plane 账号登录一次后会话复用（同站 cookie）。
const src = ref(import.meta.env.VITE_PLANE_URL || 'http://192.168.56.101/')
</script>
