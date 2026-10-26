import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  { path: '/login', component: () => import('@/views/LoginView.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('@/layouts/AppLayout.vue'),
    children: [
      { path: '', redirect: '/workbench' },
      { path: 'workbench', component: () => import('@/views/WorkbenchView.vue') },
      { path: 'finance', component: () => import('@/views/modules/FinanceView.vue'), meta: { permission: 'finance:charge:view' } },
      { path: 'inventory', component: () => import('@/views/modules/InventoryView.vue'), meta: { permission: 'inventory:stock:view' } },
      { path: 'documents', component: () => import('@/views/modules/DocumentsView.vue'), meta: { permission: 'document:file:view' } },
      { path: 'reports', component: () => import('@/views/modules/ReportsView.vue'), meta: { permission: 'report:report:view' } },
      {
        path: 'system/users',
        component: () => import('@/views/system/UserListView.vue'),
        meta: { permission: 'system:user:view' },
      },
      {
        path: 'system/roles',
        component: () => import('@/views/system/RoleListView.vue'),
        meta: { permission: 'system:role:view' },
      },
      { path: 'system/departments', component: () => import('@/views/system/DepartmentView.vue'), meta: { permission: 'system:department:view' } },
      { path: 'system/permissions', component: () => import('@/views/system/PermissionView.vue'), meta: { permission: 'system:permission:view' } },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/workbench' },
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.public) return auth.authenticated ? '/workbench' : true
  if (!auth.authenticated) return { path: '/login', query: { redirect: to.fullPath } }
  if (!auth.user) {
    try {
      await auth.loadCurrentUser()
    } catch {
      auth.logout()
      return '/login'
    }
  }
  const permission = to.meta.permission as string | undefined
  return permission && !auth.hasPermission(permission) ? '/workbench' : true
})

export default router
