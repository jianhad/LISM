package cn.lims.identity;

import cn.lims.audit.AuditService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system/roles")
public class RoleAdminController {
    private final JdbcTemplate jdbcTemplate;
    private final AuditService auditService;

    public RoleAdminController(JdbcTemplate jdbcTemplate, AuditService auditService) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('system:role:view')")
    public List<RoleRow> list() {
        return jdbcTemplate.query("""
                select r.id,r.code,r.name,r.status,r.built_in,r.version,
                       count(distinct u.id) user_count,count(distinct rp.permission_id) permission_count
                from sys_role r left join sys_user_role ur on ur.role_id=r.id
                left join sys_user u on u.id=ur.user_id and u.deleted=false
                left join sys_role_permission rp on rp.role_id=r.id
                where r.deleted=false group by r.id order by r.id
                """, (rs,n) -> new RoleRow(rs.getLong("id"),rs.getString("code"),rs.getString("name"),
                rs.getString("status"),rs.getBoolean("built_in"),rs.getInt("version"),
                rs.getInt("user_count"),rs.getInt("permission_count")));
    }

    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('system:role:view')")
    public List<Long> permissions(@PathVariable long id) {
        return jdbcTemplate.query("select permission_id from sys_role_permission where role_id=? order by permission_id",
                (rs,n)->rs.getLong(1),id);
    }

    @GetMapping("/{id}/data-scopes")
    @PreAuthorize("hasAuthority('system:role:view')")
    public List<DataScope> dataScopes(@PathVariable long id) {
        return jdbcTemplate.query("select scope_type,dept_id from sys_role_data_scope where role_id=? order by id",
                (rs,n)->new DataScope(rs.getString(1),nullableLong(rs,2)),id);
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasAuthority('system:role:create')")
    public IdResponse create(@Valid @RequestBody RoleRequest request, Authentication authentication) {
        Long id=jdbcTemplate.queryForObject("insert into sys_role(code,name,status,built_in) values (?,?,?,false) returning id",
                Long.class,request.code(),request.name(),request.status());
        replacePermissions(id,request.permissionIds());
        replaceDataScopes(id,request.dataScopes());
        auditService.record(authentication,"system:role:create","ROLE",id,null,request.toString());
        return new IdResponse(id);
    }

    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("hasAuthority('system:role:update')")
    public void update(@PathVariable long id,@Valid @RequestBody RoleRequest request,Authentication authentication) {
        int changed=jdbcTemplate.update("""
                update sys_role set code=?,name=?,status=?,updated_at=current_timestamp,version=version+1
                where id=? and deleted=false and version=?
                """,request.code(),request.name(),request.status(),id,request.version());
        if(changed==0) throw new IllegalArgumentException("角色不存在或数据已被其他用户修改");
        replacePermissions(id,request.permissionIds());
        replaceDataScopes(id,request.dataScopes());
        auditService.record(authentication,"system:role:update","ROLE",id,null,request.toString());
    }

    @PutMapping("/{id}/permissions")
    @Transactional
    @PreAuthorize("hasAuthority('system:role:grant')")
    public void assignPermissions(@PathVariable long id,@RequestBody PermissionIdsRequest request,Authentication authentication) {
        replacePermissions(id,request.permissionIds());
        auditService.record(authentication,"system:role:grant","ROLE",id,null,request.permissionIds().toString());
    }

    @PutMapping("/{id}/data-scopes")
    @Transactional
    @PreAuthorize("hasAuthority('system:role:grant')")
    public void assignDataScopes(@PathVariable long id,@RequestBody DataScopesRequest request,Authentication authentication) {
        replaceDataScopes(id,request.dataScopes());
        auditService.record(authentication,"system:role:grant-data-scope","ROLE",id,null,request.dataScopes().toString());
    }

    @DeleteMapping("/{id}")
    @Transactional
    @PreAuthorize("hasAuthority('system:role:delete')")
    public void delete(@PathVariable long id,Authentication authentication) {
        Boolean builtIn=jdbcTemplate.queryForObject("select built_in from sys_role where id=? and deleted=false",Boolean.class,id);
        if(Boolean.TRUE.equals(builtIn)) throw new IllegalArgumentException("内置角色不能删除");
        Integer users=jdbcTemplate.queryForObject("select count(*) from sys_user_role ur join sys_user u on u.id=ur.user_id and u.deleted=false where ur.role_id=?",Integer.class,id);
        if(users!=null&&users>0) throw new IllegalArgumentException("角色仍分配给用户，不能删除");
        jdbcTemplate.update("delete from sys_user_role where role_id=?",id);
        jdbcTemplate.update("delete from sys_role_permission where role_id=?",id);
        jdbcTemplate.update("delete from sys_role_data_scope where role_id=?",id);
        jdbcTemplate.update("update sys_role set deleted=true,status='DISABLED',updated_at=current_timestamp where id=?",id);
        auditService.record(authentication,"system:role:delete","ROLE",id,null,"deleted");
    }

    private void replacePermissions(long roleId,List<Long> ids){
        jdbcTemplate.update("delete from sys_role_permission where role_id=?",roleId);
        if(ids!=null) ids.stream().distinct().forEach(id->jdbcTemplate.update("insert into sys_role_permission(role_id,permission_id) values (?,?)",roleId,id));
    }
    private void replaceDataScopes(long roleId,List<DataScope> scopes){
        jdbcTemplate.update("delete from sys_role_data_scope where role_id=?",roleId);
        if(scopes!=null) scopes.forEach(s->jdbcTemplate.update("insert into sys_role_data_scope(role_id,scope_type,dept_id) values (?,?,?)",roleId,s.scopeType(),s.deptId()));
    }
    private static Long nullableLong(java.sql.ResultSet rs,int index)throws java.sql.SQLException{long v=rs.getLong(index);return rs.wasNull()?null:v;}

    public record RoleRow(long id,String code,String name,String status,boolean builtIn,int version,int userCount,int permissionCount){}
    public record RoleRequest(@NotBlank String code,@NotBlank String name,@NotBlank String status,int version,List<Long> permissionIds,List<DataScope> dataScopes){}
    public record DataScope(@NotBlank String scopeType,Long deptId){}
    public record PermissionIdsRequest(List<Long> permissionIds){}
    public record DataScopesRequest(List<DataScope> dataScopes){}
    public record IdResponse(Long id){}
}
