import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/api/token'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/Login.vue') },
    { path: '/register', name: 'register', component: () => import('@/views/Register.vue') },
    {
      path: '/',
      component: () => import('@/layouts/UserLayout.vue'),
      children: [
        { path: 'packages', component: () => import('@/views/user/Packages.vue') },
        { path: 'packages/:id', component: () => import('@/views/user/PackageDetail.vue') },
        { path: 'notifications', component: () => import('@/views/user/Notifications.vue'), meta: { requiresAuth: true } },
        { path: 'contacts', component: () => import('@/views/user/Contacts.vue'), meta: { requiresUser: true } },
        { path: 'cart', component: () => import('@/views/user/Cart.vue'), meta: { requiresUser: true } },
        { path: 'orders', component: () => import('@/views/user/Orders.vue'), meta: { requiresUser: true } },
        { path: 'orders/:id', component: () => import('@/views/user/OrderDetail.vue'), meta: { requiresUser: true } },
        { path: 'profile', component: () => import('@/views/user/Profile.vue'), meta: { requiresAuth: true } },
        { path: '', name: 'user-home', component: () => import('@/views/user/Home.vue') },
        { path: 'activities/:id', name: 'activity-detail', component: () => import('@/views/user/ActivityDetail.vue') },
      ],
    },
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAdmin: true },
      children: [
        { path: '', name: 'admin-dashboard', component: () => import('@/views/admin/Dashboard.vue') },
        { path: 'staff', name: 'admin-staff', component: () => import('@/views/admin/StaffManage.vue') },
        { path: 'categories', name: 'admin-categories', component: () => import('@/views/admin/CategoryManage.vue') },
        { path: 'notifications', component: () => import('@/views/user/Notifications.vue') },
        { path: 'orders', component: () => import('@/views/admin/OrderWorkbench.vue') },
        { path: 'checkin', component: () => import('@/views/admin/CheckinDesk.vue') },
        { path: 'reports', component: () => import('@/views/admin/Reports.vue') },
        { path: 'audit', component: () => import('@/views/admin/Audit.vue'), meta: { requiresSuperAdmin: true } },
        { path: 'refunds', component: () => import('@/views/admin/RefundManage.vue') },
        { path: 'packages', component: () => import('@/views/admin/PackageManage.vue') },
        { path: 'activities', name: 'admin-activities', component: () => import('@/views/admin/ActivityManage.vue') },
      ],
    },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFound.vue') },
  ],
})

// 前端守卫只是体验优化，权限以后端校验为准（实施书 §4）
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!getToken()) auth.applyUser(null)

  if (to.meta.requiresSuperAdmin && auth.user && auth.user.role !== 'ADMIN') return '/admin'
  if (to.meta.requiresUser && auth.user && auth.user.role !== 'USER') return '/admin'
  if (((to.meta.requiresAuth || to.meta.requiresUser) && !getToken()) || (to.meta.requiresAdmin && (!getToken() || !auth.isAdmin))) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
