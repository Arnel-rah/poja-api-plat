create table if not exists "event_stack_resource"
(
    id          varchar
        constraint pk_event_stack_resource primary key default uuid_generate_v4(),
    dead_letter_queue_1_name varchar,
    dead_letter_queue_2_name varchar,
    mailbox_queue_1_name varchar,
    mailbox_queue_2_name varchar,
    event_stack_policy_document_name varchar,
    stack_id    varchar not null,
    constraint fk_event_stack_parent_stack foreign key (stack_id) references "stack" (id)
)
