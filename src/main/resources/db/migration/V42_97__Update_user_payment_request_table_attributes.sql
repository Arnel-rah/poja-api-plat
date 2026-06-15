alter table if exists "user_payment_request" add column if not exists org_id varchar;

begin;

update "user_payment_request" u_r
set org_id = u.main_org_id
    from "user" u
where u_r.user_id = u.id;

commit;