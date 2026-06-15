create table if not exists "storage_bucket_stack_resource"
(
    id          varchar
        constraint pk_storage_bucket_stack_resource primary key default uuid_generate_v4(),
    stack_id    varchar not null,
    bucket_name varchar,
    constraint fk_storage_bucket_stack_parent_stack foreign key (stack_id) references "stack" (id)
)
