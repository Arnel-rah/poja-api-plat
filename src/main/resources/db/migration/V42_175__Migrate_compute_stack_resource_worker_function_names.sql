create table if not exists "compute_stack_resource_worker_function_names" (
    compute_stack_resource_id varchar
        constraint fk_worker_function_name_compute_resource
        references compute_resources(id),
    worker_index integer,
    worker_function_name varchar,
    worker_function_deleted boolean default false,
    constraint pk_worker_function_names
    primary key (compute_stack_resource_id, worker_index)
    );

alter table "compute_resources"
    add column if not exists frontal_function_deleted boolean default false;

insert into "compute_stack_resource_worker_function_names"
(compute_stack_resource_id, worker_index, worker_function_name, worker_function_deleted)
select id, 0, worker_1_function_name, worker_1_function_deleted
from compute_resources
where worker_1_function_name is not null
    on conflict do nothing;

insert into "compute_stack_resource_worker_function_names"
(compute_stack_resource_id, worker_index, worker_function_name, worker_function_deleted)
select id, 1, worker_2_function_name, worker_2_function_deleted
from compute_resources
where worker_2_function_name is not null
    on conflict do nothing;

alter table if exists "compute_resources" drop column if exists worker_1_function_name;
alter table if exists "compute_resources" drop column if exists worker_2_function_name;
alter table if exists "compute_resources" drop column if exists worker_1_function_deleted;
alter table if exists "compute_resources" drop column if exists worker_2_function_deleted;
