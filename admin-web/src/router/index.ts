import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

// M0 期间不强制登录，方便端到端验收；M1 接入 JWT 后改为 true
const REQUIRE_AUTH = false

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
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  if (!REQUIRE_AUTH) return true
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) return { path: '/login' }
  if (to.path === '/login' && token) return { path: '/dashboard' }
  return true
})

router.afterEach((to) => {
  const title = to.meta.title as string | undefined
  document.title = title ? `${title} · 本地生活点餐平台管理端` : '本地生活点餐平台管理端'
})

export default router
