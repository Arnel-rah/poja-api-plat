insert into "compute_resources"
(id, frontal_function_name, environment_id, creation_datetime, app_env_depl_id, stack_id)
values ('poja_application_compute_1_resources_id', 'prod-compute-frontal-function',
        'poja_application_environment_id', '2024-07-18T10:15:30.00Z', 'poja_deployment_1_id', 'compute_stack_2_id'),
       ('poja_application_compute_5_resources_id', 'prod-compute-frontal-function',
        'poja_application_environment_id', '2024-07-18T10:15:29.00Z', 'poja_deployment_2_id', 'compute_stack_2_id'),
       ('poja_application_compute_6_resources_id', 'prod-compute-frontal-function-3',
        'poja_application_environment_id', '2024-07-18T10:15:28.00Z', 'poja_deployment_3_id', 'compute_stack_2_id'),
       ('poja_application_compute_2_resources_id', 'preprod-compute-frontal-function',
        'other_poja_application_environment_id', '2023-07-18T10:15:30.00Z', 'deployment_1_id', 'compute_stack_1_id'),
       ('poja_application_compute_3_resources_id', 'preprod-compute-frontal-function',
        'other_poja_application_environment_id', '2024-08-07T12:15:00Z', 'deployment_12_id', 'compute_stack_1_id'),
       ('poja_application_compute_4_resources_id', 'preprod-compute-frontal-function',
        'other_poja_application_environment_id', '2024-08-08T12:30:00Z', 'deployment_7_id', 'compute_stack_1_id');

insert into "compute_stack_resource_worker_function_names"
(compute_stack_resource_id, worker_index, worker_function_name, worker_function_deleted)
values ('poja_application_compute_1_resources_id', 0, 'prod-compute-worker-1-function', false),
       ('poja_application_compute_1_resources_id', 1, 'prod-compute-worker-2-function', false),
       ('poja_application_compute_5_resources_id', 0, 'prod-compute-worker-1-function', false),
       ('poja_application_compute_5_resources_id', 1, 'prod-compute-worker-2-function-3', true),
       ('poja_application_compute_6_resources_id', 0, 'prod-compute-worker-1-function', false),
       ('poja_application_compute_6_resources_id', 1, 'prod-compute-worker-2-function-2', true),
       ('poja_application_compute_2_resources_id', 0, 'preprod-compute-worker-1-function', false),
       ('poja_application_compute_2_resources_id', 1, 'preprod-compute-worker-2-function', false),
       ('poja_application_compute_3_resources_id', 0, 'preprod-compute-worker-1-function', false),
       ('poja_application_compute_3_resources_id', 1, 'preprod-compute-worker-2-function-2', false),
       ('poja_application_compute_4_resources_id', 0, 'preprod-compute-worker-1-function-2', false),
       ('poja_application_compute_4_resources_id', 1, 'preprod-compute-worker-2-function', false);