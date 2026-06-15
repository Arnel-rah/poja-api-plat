alter table "console_user_group"
    add column if not exists "archived" boolean not null default false;
alter table if exists "compute_resources"
    add column if not exists "console_user_group_id" varchar;
alter table if exists "compute_resources"
    add constraint fk_compute_resource_user_group foreign key (console_user_group_id) references console_user_group ("id");

update compute_resources cr
set console_user_group_id =(select cug.id
                            from environment e
                                     inner join application a on e.id_application = a.id
                                     inner join organization o on a.org_id = o.id
                                     inner join console_user_group cug on o.id = cug.org_id
                            where e.id = cr.environment_id
                              and cug.current = true)
where cr.console_user_group_id is null;
alter table if exists "console_user_group"
    rename column "current" to "available";