import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/',
    component: () => import('@/layout/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '仪表盘' },
      },
      {
        path: 'goods',
        name: 'goods',
        component: () => import('@/views/goods/GoodsList.vue'),
        meta: { title: '商品列表', roles: ['ADMIN', 'MERCHANT'] },
      },
      {
        path: 'categories',
        name: 'categories',
        component: () => import('@/views/goods/CategoryManage.vue'),
        meta: { title: '分类管理', roles: ['ADMIN'] },
      },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const user = useUserStore()
  if (to.path !== '/login' && !user.isLoggedIn) {
    return { path: '/login' }
  }
  if (to.path === '/login' && user.isLoggedIn) {
    return { path: '/dashboard' }
  }
  // RBAC：路由 meta.roles 与用户角色比对，无权回仪表盘
  const roles = to.meta.roles as string[] | undefined
  if (roles && roles.length > 0 && !user.hasRole(...roles)) {
    ElMessage.warning('无权访问该页面')
    return { path: '/dashboard' }
  }
  return true
})

router.afterEach((to) => {
  const title = to.meta.title as string | undefined
  document.title = title ? `${title} · 本地生活点餐平台管理端` : '本地生活点餐平台管理端'
})

export default router
