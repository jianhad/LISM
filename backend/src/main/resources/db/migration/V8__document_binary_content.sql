alter table doc_version add column file_content bytea;
alter table doc_version add column file_content_type varchar(200);
alter table doc_version add column file_size bigint;
