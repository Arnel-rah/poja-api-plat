alter table if exists "organization" add column console_username varchar;
alter table if exists "organization" add column console_account_id varchar;
alter table if exists "organization" add column console_password varchar;
alter table if exists "organization" add column console_user_policy_document_name varchar;
alter table if exists "organization" rename column console_policy_document_name to console_user_group_policy_document_name;
