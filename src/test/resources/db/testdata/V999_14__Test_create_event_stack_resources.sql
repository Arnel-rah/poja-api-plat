insert into event_stack_resource (id, event_stack_policy_document_name, stack_id, creation_timestamp,
                                  env_id, app_env_depl_id)
values ('other_poja_application_event_1_resource_id', null,
        'event_stack_1_id', '2023-07-18T10:15:30.00Z', 'other_poja_application_environment_id', 'deployment_1_id'),
       ('other_poja_application_env2_event_1_resource_id', null,
        'event_stack_env_2_1_id', '2024-08-02 14:30:00', 'other_poja_application_environment_2_id', 'deployment_2_id'),
       ('other_poja_application_event_2_resource_id', null,
        'event_stack_1_id', '2024-08-07 12:15:00', 'other_poja_application_environment_id', 'deployment_12_id'),
       ('other_poja_application_event_3_resource_id', null,
        'event_stack_1_id', '2024-08-08 12:30:00', 'other_poja_application_environment_id', 'deployment_7_id');

insert into "event_stack_resource_dead_letter_queue_names"
(event_stack_resource_id, queue_index, dead_letter_queue_name)
values ('other_poja_application_event_1_resource_id', 0, 'deadQueue1'),
       ('other_poja_application_env2_event_1_resource_id', 0, 'deadQueue1Env2'),
       ('other_poja_application_env2_event_1_resource_id', 1, 'deadQueue2Env2'),
       ('other_poja_application_event_2_resource_id', 0, 'deadQueue1'),
       ('other_poja_application_event_2_resource_id', 1, 'deadQueue2'),
       ('other_poja_application_event_3_resource_id', 0, 'deadQueue1');

insert into "event_stack_resource_mailbox_queue_names"
(event_stack_resource_id, queue_index, mailbox_queue_name)
values ('other_poja_application_event_1_resource_id', 0, 'mailboxQueue1'),
       ('other_poja_application_env2_event_1_resource_id', 0, 'mailboxQueue1Env2'),
       ('other_poja_application_event_2_resource_id', 0, 'mailboxQueue1'),
       ('other_poja_application_event_2_resource_id', 1, 'mailboxQueue2'),
       ('other_poja_application_event_3_resource_id', 0, 'mailboxQueue1');