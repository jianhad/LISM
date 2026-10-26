insert into sys_permission(code,name,type) values
('system:user:delete','删除用户','API'),
('system:user:reset-password','重置用户密码','API'),
('system:role:delete','删除角色','API'),
('system:department:delete','删除部门','API'),
('system:permission:view','查看权限','API'),
('system:permission:create','新增权限','API'),
('system:permission:update','修改权限','API'),
('system:permission:delete','删除权限','API')
on conflict (code) do nothing;

insert into sys_role_permission(role_id,permission_id)
select r.id,p.id from sys_role r cross join sys_permission p
where r.code='SYSTEM_ADMIN'
on conflict do nothing;

