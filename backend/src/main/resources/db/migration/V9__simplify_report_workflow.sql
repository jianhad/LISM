-- 报告流程统一为：编制中(DRAFT) -> 待审核(REVIEWING) -> 已归档(ARCHIVED)
update rpt_report
set status = case
    when status = 'REJECTED' then 'DRAFT'
    when status in ('PENDING_APPROVAL', 'APPROVED', 'DISTRIBUTED', 'VOID') then 'ARCHIVED'
    else status
end,
archived_at = case
    when status in ('PENDING_APPROVAL', 'APPROVED', 'DISTRIBUTED', 'VOID') then coalesce(archived_at, current_timestamp)
    else archived_at
end,
updated_at = current_timestamp
where status not in ('DRAFT', 'REVIEWING', 'ARCHIVED');
