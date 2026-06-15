alter table if exists "console_user_group" add column if not exists org_id varchar;

begin;

update "console_user_group" c
set org_id = u.main_org_id
    from "user" u
where c.user_id = u.id;

commit;