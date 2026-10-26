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
@RequestMapping("/api/v1/system/permissions")
public class PermissionAdminController {
    private final JdbcTemplate jdbcTemplate;
    private final AuditService auditService;
    public PermissionAdminController(JdbcTemplate jdbcTemplate,AuditService auditService){this.jdbcTemplate=jdbcTemplate;this.auditService=auditService;}

    @GetMapping
    @PreAuthorize("hasAuthority('system:permission:view') or hasAuthority('system:role:view')")
    public List<PermissionRow> list(){return jdbcTemplate.query("select id,code,name,type,status from sys_permission order by code",
            (rs,n)->new PermissionRow(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5)));}

    @PostMapping
    @Transactional
    @PreAuthorize("hasAuthority('system:permission:create')")
    public IdResponse create(@Valid @RequestBody PermissionRequest request,Authentication authentication){
        Long id=jdbcTemplate.queryForObject("insert into sys_permission(code,name,type,status) values (?,?,?,?) returning id",Long.class,request.code(),request.name(),request.type(),request.status());
        auditService.record(authentication,"system:permission:create","PERMISSION",id,null,request.toString());return new IdResponse(id);
    }

    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("hasAuthority('system:permission:update')")
    public void update(@PathVariable long id,@Valid @RequestBody PermissionRequest request,Authentication authentication){
        int changed=jdbcTemplate.update("update sys_permission set code=?,name=?,type=?,status=? where id=?",request.code(),request.name(),request.type(),request.status(),id);
        if(changed==0)throw new IllegalArgumentException("权限不存在");auditService.record(authentication,"system:permission:update","PERMISSION",id,null,request.toString());
    }

    @DeleteMapping("/{id}")
    @Transactional
    @PreAuthorize("hasAuthority('system:permission:delete')")
    public void delete(@PathVariable long id,Authentication authentication){
        Integer roles=jdbcTemplate.queryForObject("select count(*) from sys_role_permission where permission_id=?",Integer.class,id);
        if(roles!=null&&roles>0)throw new IllegalArgumentException("权限已分配给角色，不能删除");
        if(jdbcTemplate.update("delete from sys_permission where id=?",id)==0)throw new IllegalArgumentException("权限不存在");
        auditService.record(authentication,"system:permission:delete","PERMISSION",id,null,"deleted");
    }
    public record PermissionRow(long id,String code,String name,String type,String status){}
    public record PermissionRequest(@NotBlank String code,@NotBlank String name,@NotBlank String type,@NotBlank String status){}
    public record IdResponse(Long id){}
}

