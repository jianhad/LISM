package cn.lims.identity;

import cn.lims.audit.AuditService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
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
@RequestMapping("/api/v1/system/users")
public class UserAdminController {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserAdminController(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder, AuditService auditService) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('system:user:view')")
    public List<UserSummary> list() {
        return jdbcTemplate.query("""
                select u.id, u.username, u.display_name, u.dept_id, u.status, u.version,
                       d.name as department_name,
                       coalesce(string_agg(r.name, ', ' order by r.name), '') as role_names
                from sys_user u
                left join sys_department d on d.id=u.dept_id
                left join sys_user_role ur on ur.user_id=u.id
                left join sys_role r on r.id=ur.role_id and r.deleted=false
                where u.deleted=false
                group by u.id,d.name order by u.id
                """, (rs, n) -> new UserSummary(rs.getLong("id"), rs.getString("username"),
                rs.getString("display_name"), nullableLong(rs, "dept_id"), rs.getString("department_name"),
                rs.getString("role_names"), rs.getString("status"), rs.getInt("version")));
    }

    @GetMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('system:user:view')")
    public List<Long> roles(@PathVariable long id) {
        return jdbcTemplate.query("select role_id from sys_user_role where user_id=? order by role_id",
                (rs, n) -> rs.getLong(1), id);
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasAuthority('system:user:create')")
    public IdResponse create(@Valid @RequestBody CreateUserRequest request, Authentication authentication) {
        Long id = jdbcTemplate.queryForObject("""
                insert into sys_user(username,password_hash,display_name,dept_id,status,password_changed_at)
                values (?,?,?,?,?,current_timestamp) returning id
                """, Long.class, request.username(), passwordEncoder.encode(request.password()),
                request.displayName(), request.deptId(), request.status());
        replaceRoles(id, request.roleIds());
        auditService.record(authentication, "system:user:create", "USER", id, null, request.username());
        return new IdResponse(id);
    }

    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("hasAuthority('system:user:update')")
    public void update(@PathVariable long id, @Valid @RequestBody UpdateUserRequest request,
                       Authentication authentication) {
        String before = jdbcTemplate.queryForObject("select username || ':' || display_name from sys_user where id=? and deleted=false", String.class, id);
        int changed = jdbcTemplate.update("""
                update sys_user set display_name=?, dept_id=?, status=?, updated_at=current_timestamp, version=version+1
                where id=? and deleted=false and version=?
                """, request.displayName(), request.deptId(), request.status(), id, request.version());
        if (changed == 0) throw new IllegalArgumentException("用户不存在或数据已被其他用户修改");
        replaceRoles(id, request.roleIds());
        auditService.record(authentication, "system:user:update", "USER", id, before, request.toString());
    }

    @PutMapping("/{id}/status")
    @Transactional
    @PreAuthorize("hasAuthority('system:user:enable')")
    public void changeStatus(@PathVariable long id, @Valid @RequestBody StatusRequest request,
                             Authentication authentication) {
        int changed = jdbcTemplate.update("update sys_user set status=?,updated_at=current_timestamp,version=version+1 where id=? and deleted=false", request.status(), id);
        if (changed == 0) throw new IllegalArgumentException("用户不存在");
        auditService.record(authentication, "system:user:enable", "USER", id, null, request.status());
    }

    @PutMapping("/{id}/password")
    @Transactional
    @PreAuthorize("hasAuthority('system:user:reset-password')")
    public void resetPassword(@PathVariable long id, @Valid @RequestBody PasswordRequest request,
                              Authentication authentication) {
        int changed = jdbcTemplate.update("""
                update sys_user set password_hash=?,password_changed_at=current_timestamp,
                       updated_at=current_timestamp,version=version+1 where id=? and deleted=false
                """, passwordEncoder.encode(request.password()), id);
        if (changed == 0) throw new IllegalArgumentException("用户不存在");
        auditService.record(authentication, "system:user:reset-password", "USER", id, null, "password reset");
    }

    @PutMapping("/{id}/roles")
    @Transactional
    @PreAuthorize("hasAuthority('system:user:grant')")
    public void assignRoles(@PathVariable long id, @RequestBody RoleIdsRequest request,
                            Authentication authentication) {
        replaceRoles(id, request.roleIds());
        auditService.record(authentication, "system:user:grant", "USER", id, null, request.roleIds().toString());
    }

    @DeleteMapping("/{id}")
    @Transactional
    @PreAuthorize("hasAuthority('system:user:delete')")
    public void delete(@PathVariable long id, Authentication authentication) {
        int changed = jdbcTemplate.update("update sys_user set deleted=true,status='DISABLED',updated_at=current_timestamp where id=? and deleted=false", id);
        if (changed == 0) throw new IllegalArgumentException("用户不存在");
        jdbcTemplate.update("delete from sys_user_role where user_id=?", id);
        auditService.record(authentication, "system:user:delete", "USER", id, null, "deleted");
    }

    private void replaceRoles(long userId, List<Long> roleIds) {
        jdbcTemplate.update("delete from sys_user_role where user_id=?", userId);
        if (roleIds == null) return;
        roleIds.stream().distinct().forEach(roleId -> jdbcTemplate.update(
                "insert into sys_user_role(user_id,role_id) values (?,?)", userId, roleId));
    }

    private static Long nullableLong(java.sql.ResultSet rs, String field) throws java.sql.SQLException {
        long value = rs.getLong(field);
        return rs.wasNull() ? null : value;
    }

    public record UserSummary(long id, String username, String displayName, Long deptId,
                              String departmentName, String roleNames, String status, int version) {}
    public record CreateUserRequest(@NotBlank String username, @NotBlank @Size(min=6) String password,
                                    @NotBlank String displayName, Long deptId, @NotBlank String status,
                                    List<Long> roleIds) {}
    public record UpdateUserRequest(@NotBlank String displayName, Long deptId, @NotBlank String status,
                                    int version, List<Long> roleIds) {}
    public record StatusRequest(@NotBlank String status) {}
    public record PasswordRequest(@NotBlank @Size(min=6) String password) {}
    public record RoleIdsRequest(List<Long> roleIds) {}
    public record IdResponse(Long id) {}
}
