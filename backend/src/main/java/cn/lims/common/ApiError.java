package cn.lims.common;

import java.time.OffsetDateTime;

public record ApiError(String code, String message, String traceId, OffsetDateTime timestamp) {
}

