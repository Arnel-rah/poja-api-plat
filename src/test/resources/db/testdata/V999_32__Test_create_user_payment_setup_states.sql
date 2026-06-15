insert into "user_payment_setup_state"(id, progression_status, timestamp, execution_type, user_id)
values
    ('joe_doe_payment_setup_state_1', 'PAYMENT_SETUP_IN_PROGRESS',
     '2024-03-25T12:00:00.00Z', 'ASYNCHRONOUS', 'joe-doe-id'),
    ('joe_doe_payment_setup_state_2', 'PAYMENT_SETUP_COMPLETED',
     '2024-03-25T12:01:00.00Z', 'ASYNCHRONOUS', 'joe-doe-id');