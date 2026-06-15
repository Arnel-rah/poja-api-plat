alter table application_import
    add column if not exists archived boolean default false not null,
    add column if not exists archived_at timestamp;

update application_import ai
set archived = true,
    archived_at = a.archived_at
    from application a
where ai.created_app_id = a.id
  and a.archived = true
  and ai.archived = false;
