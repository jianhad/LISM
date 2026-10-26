create table inv_issue_request (
    id bigserial primary key,
    request_no varchar(64) not null unique,
    batch_id bigint not null references inv_stock_batch(id),
    quantity decimal(18,6) not null check (quantity > 0),
    purpose varchar(500) not null,
    status varchar(32) not null default 'SUBMITTED',
    applicant_id bigint not null references sys_user(id),
    reviewed_by bigint references sys_user(id),
    review_comment varchar(500),
    reviewed_at timestamp,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint ck_inv_issue_request_status check (status in ('SUBMITTED','APPROVED','RETURNED'))
);

create unique index uq_inv_issue_request_pending_batch
    on inv_issue_request(batch_id) where status = 'SUBMITTED';
create index idx_inv_issue_request_batch_time
    on inv_issue_request(batch_id, created_at desc);
