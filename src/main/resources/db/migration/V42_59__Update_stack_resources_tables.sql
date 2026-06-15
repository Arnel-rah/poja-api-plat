alter table if exists "event_stack_resource"
    add column "creation_timestamp" timestamp without time zone;
alter table if exists "storage_bucket_stack_resource"
    add column "creation_timestamp" timestamp without time zone;
