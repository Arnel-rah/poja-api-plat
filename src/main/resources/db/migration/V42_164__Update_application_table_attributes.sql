alter table if exists "application"
    add column if not exists import_id varchar;

begin;
update "application" a
set import_id = ai.id
    from (
    select distinct on (app_name, org_id)
        id,
        app_name,
        org_id
    from application_import
    order by app_name, org_id, creation_datetime desc
) ai
where ai.app_name = a.name
  and ai.org_id = a.org_id
  and a.imported = true;
commit;

alter table "application" drop column if exists imported;