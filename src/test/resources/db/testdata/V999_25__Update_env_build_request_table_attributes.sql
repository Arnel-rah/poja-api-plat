alter table if exists "env_build_request" add column if not exists org_id varchar;

begin;

update "env_build_request" e
set org_id = u.main_org_id
    from "user" u
where e.user_id = u.id;

commit;