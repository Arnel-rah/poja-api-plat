INSERT INTO user_state (user_id, timestamp, execution_type, progression_status, description)
VALUES ('joe-doe-id', '2024-03-25T12:00:00.00Z', 'SYNCHRONOUS', 'ACTIVE', null),
       ('jane_doe_id', '2024-03-25T12:00:00.00Z', 'SYNCHRONOUS', 'ACTIVE', null),
       ('denis_ritchie_id', '2024-03-25T12:00:00.00Z', 'SYNCHRONOUS', 'ACTIVE', null),
       ('lorem_ipsum_id', '2024-03-25T12:00:00.00Z', 'SYNCHRONOUS', 'ACTIVE', null),

       ('admin_id', '2024-03-25T12:00:00.00Z', 'SYNCHRONOUS', 'ACTIVE', null),
       ('to_suspend_id', '2024-03-25T12:00:00.00Z', 'SYNCHRONOUS', 'ACTIVE', null),
       ('recsus_id', '2024-03-25T12:00:00.00Z', 'SYNCHRONOUS', 'ACTIVE', null),
       ('to_suspend_id', '2024-03-25T12:00:00.00Z', 'SYNCHRONOUS', 'ACTIVE', null),
       ('to_activate_id', '2024-03-25T12:00:00.00Z', 'SYNCHRONOUS', 'ACTIVE', null),
       ('to_archive_id', '2024-03-25T12:00:00.00Z', 'SYNCHRONOUS', 'ACTIVE', null),
       ('archived_id', '2024-03-25T12:00:00.00Z', 'SYNCHRONOUS', 'ACTIVE', null),
       ('to_upsert_id', '2025-03-25T12:00:00.00Z', 'SYNCHRONOUS', 'ACTIVE', null),

       ('suspended_id', '2025-03-25T12:00:00.00Z', 'SYNCHRONOUS', 'SUSPENDED', null),

       ('suspended_2_id', '2025-03-30T12:00:00.00Z', 'SYNCHRONOUS', 'UNDER_MODIFICATION', null),
       ('suspended_2_id', '2025-03-30T12:00:00.00Z', 'SYNCHRONOUS', 'SUSPENDED', null),

       ('suspended_3_id', '2025-03-30T14:00:00.00Z', 'SYNCHRONOUS', 'SUSPENDED', null),
       ('suspended_3_id', '2025-03-30T17:00:00.00Z', 'SYNCHRONOUS', 'UNDER_MODIFICATION', null);

INSERT INTO user_state (user_id, id, timestamp, execution_type, progression_status, description)
VALUES ('noobie_id', 'ns_1_id', '2024-03-25T12:00:00.00Z', 'SYNCHRONOUS', 'ACTIVE', null),
       ('noobie_id', 'ns_2_id', '2024-03-25T13:00:00.00Z', 'SYNCHRONOUS', 'UNDER_MODIFICATION',
        'computing user status'),
       ('noobie_id', 'ns_3_id', '2024-03-25T14:00:00.00Z', 'SYNCHRONOUS', 'SUSPENDED', 'admin: suspicious activity'),
       ('noobie_id', 'ns_4_id', '2024-03-25T15:00:00.00Z', 'SYNCHRONOUS', 'UNDER_MODIFICATION',
        'computing user status'),
       ('noobie_id', 'ns_5_id', '2024-03-25T16:00:00.00Z', 'SYNCHRONOUS', 'ACTIVE', 'admin: confirmed usual activity');

