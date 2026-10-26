package cn.lims.audit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final JdbcTemplate jdbcTemplate;

    public AuditService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void record(Authentication authentication, String action, String objectType,
                       Object objectId, String beforeSummary, String afterSummary) {
        Long actorId = authentication == null ? null : jdbcTemplate.query("""
                select id from sys_user where username = ? and deleted = false
                """, rs -> rs.next() ? rs.getLong(1) : null, authentication.getName());
        jdbcTemplate.update("""
                insert into audit_operation(actor_id, action, object_type, object_id, result,
                                            before_summary, after_summary, occurred_at)
                values (?, ?, ?, ?, 'SUCCESS', ?, ?, current_timestamp)
                """, actorId, action, objectType, objectId == null ? null : objectId.toString(),
                beforeSummary, afterSummary);
    }
}

