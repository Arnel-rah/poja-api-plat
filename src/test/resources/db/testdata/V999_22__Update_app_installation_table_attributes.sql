alter table if exists "app_installation" add column if not exists org_id varchar;

begin;

update "app_installation" a
set org_id = u.main_org_id
    from "user" u
where a.user_id = u.id;

commit;