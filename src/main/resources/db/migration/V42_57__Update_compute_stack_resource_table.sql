alter table if exists "compute_resources" add column "stack_id" varchar;
alter table if exists "compute_resources" add constraint fk_compute_resource_parent_stack foreign key (stack_id) references stack(id);
