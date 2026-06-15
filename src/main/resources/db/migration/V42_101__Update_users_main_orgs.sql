begin;

update "organization" o
set
    console_user_group_name = cug_subquery.name
    from (
        select u.main_org_id, cug.name
        from "user" u
        inner join console_user_group cug on cug.user_id = u.id
        where cug.current = true
) as cug_subquery
where o.id = cug_subquery.main_org_id;

update "organization" o
set
    console_policy_document_name = owner_user.console_policy_document_name
    from "user" owner_user
where o.owner_id = owner_user.id;

commit;
