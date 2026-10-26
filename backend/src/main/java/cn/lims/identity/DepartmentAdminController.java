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
@RequestMapping("/api/v1/system/departments")
public class DepartmentAdminController {
    private final JdbcTemplate jdbcTemplate;
    private final AuditService auditService;

    public DepartmentAdminController(JdbcTemplate jdbcTemplate, AuditService auditService) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('system:department:view')")
    public List<DepartmentRow> list() {
        return jdbcTemplate.query("""
                select id, parent_id, code, name, path, status, version
                from sys_department where deleted = false order by path, id
                """, (rs, n) -> new DepartmentRow(rs.getLong("id"), nullableLong(rs, "parent_id"),
                rs.getString("code"), rs.getString("name"), rs.getString("path"),
                rs.getString("status"), rs.getInt("version")));
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasAuthority('system:department:create')")
    public IdResponse create(@Valid @RequestBody DepartmentRequest request, Authentication authentication) {
        Long organizationId = jdbcTemplate.queryForObject("select id from sys_organization where code='LIMS'", Long.class);
        String parentPath = request.parentId() == null ? "/" : jdbcTemplate.queryForObject(
                "select path from sys_department where id=? and deleted=false", String.class, request.parentId());
        String path = parentPath + request.code() + "/";
        Long id = jdbcTemplate.queryForObject("""
                insert into sys_department(organization_id, parent_id, code, name, path, status)
                values (?, ?, ?, ?, ?, ?) returning id
                """, Long.class, organizationId, request.parentId(), request.code(), request.name(), path, request.status());
        auditService.record(authentication, "system:department:create", "DEPARTMENT", id, null, request.toString());
        return new IdResponse(id);
    }

    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("hasAuthority('system:department:update')")
    public void update(@PathVariable long id, @Valid @RequestBody DepartmentRequest request,
                       Authentication authentication) {
        String before = jdbcTemplate.queryForObject("select code || ':' || name from sys_department where id=? and deleted=false", String.class, id);
        String oldPath = jdbcTemplate.queryForObject("select path from sys_department where id=?", String.class, id);
        String parentPath = request.parentId() == null ? "/" : jdbcTemplate.queryForObject(
                "select path from sys_department where id=? and deleted=false", String.class, request.parentId());
        String newPath = parentPath + request.code() + "/";
        int changed = jdbcTemplate.update("""
                update sys_department set parent_id=?, code=?, name=?, path=?, status=?,
                       updated_at=current_timestamp, version=version+1
                where id=? and deleted=false and version=?
                """, request.parentId(), request.code(), request.name(), newPath, request.status(), id, request.version());
        if (changed == 0) throw new IllegalArgumentException("部门不存在或数据已被其他用户修改");
        if (!oldPath.equals(newPath)) {
            jdbcTemplate.update("""
                    update sys_department set path = ? || substring(path from ?), updated_at=current_timestamp
                    where path like ? and id <> ?
                    """, newPath, oldPath.length() + 1, oldPath + "%", id);
        }
        auditService.record(authentication, "system:department:update", "DEPARTMENT", id, before, request.toString());
    }

    @DeleteMapping("/{id}")
    @Transactional
    @PreAuthorize("hasAuthority('system:department:delete')")
    public void delete(@PathVariable long id, Authentication authentication) {
        Integer children = jdbcTemplate.queryForObject("select count(*) from sys_department where parent_id=? and deleted=false", Integer.class, id);
        Integer users = jdbcTemplate.queryForObject("select count(*) from sys_user where dept_id=? and deleted=false", Integer.class, id);
        if ((children != null && children > 0) || (users != null && users > 0)) throw new IllegalArgumentException("部门下存在子部门或用户，不能删除");
        int changed = jdbcTemplate.update("update sys_department set deleted=true, updated_at=current_timestamp where id=? and deleted=false", id);
        if (changed == 0) throw new IllegalArgumentException("部门不存在");
        auditService.record(authentication, "system:department:delete", "DEPARTMENT", id, null, "deleted");
    }

    private static Long nullableLong(java.sql.ResultSet rs, String field) throws java.sql.SQLException {
        long value = rs.getLong(field);
        return rs.wasNull() ? null : value;
    }

    public record DepartmentRow(long id, Long parentId, String code, String name, String path, String status, int version) {}
    public record DepartmentRequest(Long parentId, @NotBlank String code, @NotBlank String name,
                                    @NotBlank String status, int version) {}
    public record IdResponse(Long id) {}
}

