alter table if exists "user" add column if not exists main_org_id varchar;

begin;

with create_users_main_org as (
    insert into "organization" (owner_id, name)
    select id, 'org-' || username from "user"
    returning id, owner_id
), updated_users as (
    update "user" u
    set main_org_id = o.id
    from create_users_main_org o
    where u.id = o.owner_id
    returning u.id as u_id, o.id as o_id
)
insert into "organization_invite" (invited_user, inviter_org, status)
    select u_id, o_id, 'ACCEPTED' from updated_users;

commit;