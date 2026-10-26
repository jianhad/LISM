package cn.lims.platform;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdmin implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public BootstrapAdmin(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder,
                          @Value("${lims.bootstrap-admin.username:admin}") String username,
                          @Value("${lims.bootstrap-admin.password:}") String password) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (password == null || password.isBlank()) return;
        Integer count = jdbcTemplate.queryForObject("select count(*) from sys_user where username = ?", Integer.class, username);
        if (count != null && count > 0) return;
        Long deptId = jdbcTemplate.queryForObject("select id from sys_department where code = 'ROOT'", Long.class);
        Long userId = jdbcTemplate.queryForObject("""
                insert into sys_user(tenant_id, username, password_hash, display_name, dept_id, status)
                values (1, ?, ?, '系统管理员', ?, 'ENABLED') returning id
                """, Long.class, username, passwordEncoder.encode(password), deptId);
        Long roleId = jdbcTemplate.queryForObject("select id from sys_role where code = 'SYSTEM_ADMIN'", Long.class);
        jdbcTemplate.update("insert into sys_user_role(user_id, role_id) values (?, ?)", userId, roleId);
    }
}

