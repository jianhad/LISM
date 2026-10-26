package cn.lims.audit;

import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationAuditListener {
    private final JdbcTemplate jdbcTemplate;

    public AuthenticationAuditListener(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        jdbcTemplate.update("""
                insert into sys_login_log(username, result, occurred_at)
                values (?, 'SUCCESS', current_timestamp)
                """, username);
        jdbcTemplate.update("""
                update sys_user set last_login_at = current_timestamp, updated_at = current_timestamp
                where username = ? and deleted = false
                """, username);
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        jdbcTemplate.update("""
                insert into sys_login_log(username, result, failure_reason, occurred_at)
                values (?, 'FAILURE', ?, current_timestamp)
                """, event.getAuthentication().getName(), event.getException().getClass().getSimpleName());
    }
}

