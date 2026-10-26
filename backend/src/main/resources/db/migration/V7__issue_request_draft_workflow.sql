drop index if exists uq_inv_issue_request_pending_batch;

alter table inv_issue_request drop constraint if exists ck_inv_issue_request_status;
alter table inv_issue_request alter column status drop default;
update inv_issue_request set status='ISSUED' where status='APPROVED';
update inv_issue_request set status='DRAFT' where status='RETURNED';
alter table inv_issue_request alter column status set default 'DRAFT';
alter table inv_issue_request add column if not exists stock_txn_id bigint references inv_stock_txn(id);
alter table inv_issue_request add column if not exists version integer not null default 0;
alter table inv_issue_request add column if not exists deleted boolean not null default false;
alter table inv_issue_request alter column review_comment type varchar(1000);
alter table inv_issue_request add constraint ck_inv_issue_request_status check (status in ('DRAFT','SUBMITTED','ISSUED'));

create index if not exists idx_inv_issue_request_status on inv_issue_request(status, created_at desc);
