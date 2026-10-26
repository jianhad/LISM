package cn.lims.identity;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseUserDetailsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var users = jdbcTemplate.query("""
                select id, username, password_hash, status
                from sys_user where username = ? and deleted = false
                """, (rs, rowNum) -> new UserRow(
                rs.getLong("id"), rs.getString("username"), rs.getString("password_hash"), rs.getString("status")), username);
        if (users.isEmpty()) throw new UsernameNotFoundException("用户不存在");
        var user = users.getFirst();
        List<SimpleGrantedAuthority> authorities = jdbcTemplate.query("""
                select distinct p.code
                from sys_user_role ur
                join sys_role r on r.id = ur.role_id and r.status = 'ENABLED'
                join sys_role_permission rp on rp.role_id = r.id
                join sys_permission p on p.id = rp.permission_id and p.status = 'ENABLED'
                where ur.user_id = ?
                  and (ur.valid_from is null or ur.valid_from <= current_timestamp)
                  and (ur.valid_to is null or ur.valid_to >= current_timestamp)
                """, (rs, rowNum) -> new SimpleGrantedAuthority(rs.getString(1)), user.id());
        return User.withUsername(user.username())
                .password(user.passwordHash())
                .authorities(authorities)
                .disabled(!"ENABLED".equals(user.status()))
                .build();
    }

    private record UserRow(long id, String username, String passwordHash, String status) {}
}

