alter table if exists "billing_info" add column if not exists org_id varchar;

begin;

update "billing_info" b
set org_id = u.main_org_id
    from "user" u
where b.user_id = u.id;

commit;