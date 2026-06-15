create table if not exists "event_stack_resource_dead_letter_queue_names" (
    event_stack_resource_id varchar
        constraint fk_dl_queue_name_event_stack_resource
        references event_stack_resource(id),
    queue_index integer,
    dead_letter_queue_name varchar,
    constraint pk_dl_queue_names
    primary key (event_stack_resource_id, queue_index)
    );

create table if not exists "event_stack_resource_mailbox_queue_names" (
    event_stack_resource_id varchar
        constraint fk_mailbox_queue_name_event_stack_resource
        references event_stack_resource(id),
    queue_index integer,
    mailbox_queue_name varchar,
    constraint pk_mailbox_queue_names
    primary key (event_stack_resource_id, queue_index)
    );

insert into "event_stack_resource_dead_letter_queue_names"
(event_stack_resource_id, queue_index, dead_letter_queue_name)
select id, 0, dead_letter_queue_1_name
from event_stack_resource
where dead_letter_queue_1_name is not null
    on conflict do nothing;

insert into "event_stack_resource_dead_letter_queue_names"
(event_stack_resource_id, queue_index, dead_letter_queue_name)
select id, 1, dead_letter_queue_2_name
from event_stack_resource
where dead_letter_queue_2_name is not null
    on conflict do nothing;

insert into "event_stack_resource_mailbox_queue_names"
(event_stack_resource_id, queue_index, mailbox_queue_name)
select id, 0, mailbox_queue_1_name
from event_stack_resource
where mailbox_queue_1_name is not null
    on conflict do nothing;

insert into "event_stack_resource_mailbox_queue_names"
(event_stack_resource_id, queue_index, mailbox_queue_name)
select id, 1, mailbox_queue_2_name
from event_stack_resource
where mailbox_queue_2_name is not null
    on conflict do nothing;

alter table if exists "event_stack_resource" drop column if exists dead_letter_queue_1_name;
alter table if exists "event_stack_resource" drop column if exists dead_letter_queue_2_name;
alter table if exists "event_stack_resource" drop column if exists mailbox_queue_1_name;
alter table if exists "event_stack_resource" drop column if exists mailbox_queue_2_name;
