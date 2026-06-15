alter table if exists "user_app_log_query" add column if not exists org_id varchar;

begin;

update "user_app_log_query" u_q
set org_id = u.main_org_id
    from "user" u
where u_q.user_id = u.id;

commit;