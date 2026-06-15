begin;

-- Exclude user with ID "to_upsert_id" whose main org will be created by a separate test
with create_users_main_org as (
    insert into "organization"
        (
         id,
         owner_id,
         name,
         console_account_id,
         console_username,
         console_password,
         console_user_policy_document_name
        )
        select 'org-' || username || '-id',
               id,
               'org-' || username ,
               '101',
               'org-' || username,
               'org-' || username || '-password',
               'org-' || username || '-user-logPolicies'
        from "user"
        where id <> 'to_upsert_id'
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

update "organization"
set
    console_username = 'AdminAdmin',
    console_user_policy_document_name = 'Admin-Policies',
    console_password = 'AdminPassword',
    console_account_id = '1007'
where owner_id = 'org-Admin-id';

update "organization"
set
    console_username = 'SuspendedSuspended',
    console_user_policy_document_name = 'Suspended-Policies',
    console_password = 'SuspendedPassword',
    console_account_id = '1008'
where owner_id = 'org-Suspended-id';

commit;