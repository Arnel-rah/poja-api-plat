alter table "application" drop constraint if exists unique_app_name;

create unique index unique_non_archived_app_name on "application" (name) where archived = false;