<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const user = useUserStore()
const router = useRouter()

async function handleLogout() {
  await user.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">🍜 点餐平台管理端</div>
      <el-menu
        router
        :default-active="$route.path"
        background-color="#1d2935"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>仪表盘
        </el-menu-item>
        <el-sub-menu index="goods-menu">
          <template #title>
            <el-icon><Goods /></el-icon>商品管理
          </template>
          <el-menu-item index="/goods">商品列表</el-menu-item>
          <el-menu-item v-if="user.isAdmin" index="/categories">分类管理</el-menu-item>
        </el-sub-menu>
        <el-menu-item disabled>
          <el-icon><Tickets /></el-icon>订单管理（M2）
        </el-menu-item>
        <el-menu-item disabled>
          <el-icon><Discount /></el-icon>优惠券管理（M3）
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span>{{ $route.meta.title ?? '' }}</span>
        <el-dropdown>
          <span class="user">{{ user.username || '未登录' }}</span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  height: 100vh;
}

.aside {
  background-color: #1d2935;
}

.logo {
  color: #fff;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e6e6e6;
  background: #fff;
}

.user {
  cursor: pointer;
}
</style>
