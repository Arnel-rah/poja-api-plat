alter table if exists "event_stack_resources"
    add column "creation_timestamp" timestamp without time zone;
alter table if exists "storage_bucket_resources"
    add column "creation_timestamp" timestamp without time zone;
