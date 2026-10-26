import { http } from './http'

export interface Department { id:number; parentId:number|null; code:string; name:string; path:string; status:string; version:number }
export interface Role { id:number; code:string; name:string; status:string; builtIn:boolean; version:number; userCount:number; permissionCount:number }
export interface Permission { id:number; code:string; name:string; type:string; status:string }
export interface UserRow { id:number; username:string; displayName:string; deptId:number|null; departmentName:string|null; roleNames:string; status:string; version:number }
export interface DataScope { scopeType:string; deptId:number|null }

export const systemApi = {
  departments: () => http.get<Department[]>('/system/departments').then(r=>r.data),
  createDepartment: (data:Partial<Department>) => http.post('/system/departments',data),
  updateDepartment: (id:number,data:Partial<Department>) => http.put(`/system/departments/${id}`,data),
  deleteDepartment: (id:number) => http.delete(`/system/departments/${id}`),
  users: () => http.get<UserRow[]>('/system/users').then(r=>r.data),
  userRoles: (id:number) => http.get<number[]>(`/system/users/${id}/roles`).then(r=>r.data),
  createUser: (data:unknown) => http.post('/system/users',data),
  updateUser: (id:number,data:unknown) => http.put(`/system/users/${id}`,data),
  userStatus: (id:number,status:string) => http.put(`/system/users/${id}/status`,{status}),
  resetPassword: (id:number,password:string) => http.put(`/system/users/${id}/password`,{password}),
  deleteUser: (id:number) => http.delete(`/system/users/${id}`),
  roles: () => http.get<Role[]>('/system/roles').then(r=>r.data),
  rolePermissions: (id:number) => http.get<number[]>(`/system/roles/${id}/permissions`).then(r=>r.data),
  roleScopes: (id:number) => http.get<DataScope[]>(`/system/roles/${id}/data-scopes`).then(r=>r.data),
  createRole: (data:unknown) => http.post('/system/roles',data),
  updateRole: (id:number,data:unknown) => http.put(`/system/roles/${id}`,data),
  deleteRole: (id:number) => http.delete(`/system/roles/${id}`),
  permissions: () => http.get<Permission[]>('/system/permissions').then(r=>r.data),
  createPermission: (data:unknown) => http.post('/system/permissions',data),
  updatePermission: (id:number,data:unknown) => http.put(`/system/permissions/${id}`,data),
  deletePermission: (id:number) => http.delete(`/system/permissions/${id}`),
}
