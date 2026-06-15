insert into "organization_setup_state"(id, progression_status, timestamp, execution_type, org_id)
values
    ('joe_doe_org_setup_state_1', 'ORGANIZATION_SETUP_IN_PROGRESS',
     '2024-03-25T12:00:00.00Z', 'ASYNCHRONOUS', 'org_1_id'),
    ('joe_doe_org_setup_state_2', 'ORGANIZATION_SETUP_COMPLETED',
     '2024-03-25T12:01:00.00Z', 'ASYNCHRONOUS', 'org_1_id'),
    ('joe_doe_org_setup_state_3', 'ORGANIZATION_SETUP_COMPLETED',
    '2024-03-25T12:01:00.00Z', 'ASYNCHRONOUS', 'org-JoeDoe-id'),
    ('jane_doe_org_setup_state_1', 'ORGANIZATION_SETUP_COMPLETED',
    '2024-03-25T12:01:00.00Z', 'ASYNCHRONOUS', 'org-JaneDoe-id'),
    ('lorem_ipsum_org_setup_state_2', 'ORGANIZATION_SETUP_COMPLETED',
    '2024-03-25T12:01:00.00Z', 'ASYNCHRONOUS', 'org-LoremIpsum-id'),
    ('joe_doe_org_setup_state_4', 'ORGANIZATION_SETUP_FAILED',
     '2024-03-25T12:01:00.00Z', 'ASYNCHRONOUS', 'org_3_id');