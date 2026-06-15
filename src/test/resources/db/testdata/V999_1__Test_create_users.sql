insert into "user"(id, first_name, last_name, username, email, roles, github_id, avatar, pricing_method, stripe_id,
                   joined_at, status_updated_at)
values ('joe-doe-id', 'Joe', 'Doe', 'JoeDoe', 'joe@email.com', '{USER}', '1234', 'https://github.com/images/JoeDoe.png',
        'TEN_MICRO'::pricing_method, 'joe_stripe_id', '2024-03-25T12:00:00.00Z', '2024-03-25T12:00:00.00Z'),
       ('jane_doe_id', 'Jane', 'Doe', 'JaneDoe', 'jane@email.com', '{USER}', '4321',
        'https://github.com/images/JaneDoe.png',
        'TEN_MICRO'::pricing_method, 'jane_stripe_id', '2024-03-25T12:00:00.00Z', '2024-03-25T16:00:00.00Z'),
       ('denis_ritchie_id', 'Denis', 'Ritchie', 'DenisRitchie', 'denis@email.com', '{USER}', '1010',
        'https://github.com/images/DenisRitchie.png',
        'TEN_MICRO'::pricing_method, 'denis_ritchie_stripe_id', '2025-03-24T12:00:00.00Z', '2024-03-25T12:00:00.00Z'),
       ('lorem_ipsum_id', 'lorem', 'ipsum', 'LoremIpsum', 'lorem@email.com', '{USER}', '4567',
        'https://github.com/images/LoremIpsum.png',
        'TEN_MICRO'::pricing_method, 'lorem_ipsum_stripe_id', '2025-02-25T12:00:00.00Z', '2024-03-25T12:00:00.00Z'),
       ('noobie_id', 'noobie', 'noobie', 'Noobie', 'noobie@email.com', '{USER}', '1902',
        'https://github.com/images/noobie.png',
        'TEN_MICRO'::pricing_method, 'noobie_stripe_id', '2024-02-25T12:00:00.00Z', '2024-03-25T12:00:00.00Z'),
       ('admin_id', 'admin', 'istrateur', 'Admin', 'admin@email.com', '{ADMIN}', '1007',
        'https://github.com/images/Admin.png',
        'TEN_MICRO'::pricing_method, 'admin_stripe_id', '2024-02-20T12:00:00.00Z', '2024-03-25T12:00:00.00Z'),
       ('to_suspend_id', 'to_suspend', 'haha', 'sus', 'sus@email.com', '{USER}', '1232',
        'https://github.com/images/sus.png',
        'TEN_MICRO'::pricing_method, 'sus_stripe_id', '2025-01-01T12:00:00.00Z', '2024-03-25T12:00:00.00Z'),
       ('to_activate_id', 'to_activate', 'haha', 'act', 'act@email.com', '{USER}', '1214',
        'https://github.com/images/act.png',
        'TEN_MICRO'::pricing_method, 'act_stripe_id', '2024-03-25T12:00:00.00Z', '2024-03-25T12:00:00.00Z'),
       ('to_archive_id', 'to_archive', 'haha', 'arch', 'arch@email.com', '{USER}', '3142',
        'https://github.com/images/arch.png',
        'TEN_MICRO'::pricing_method, 'arch_stripe_id', '2024-03-25T12:00:00.00Z', '2024-03-25T12:00:00.00Z'),
       ('recsus_id', 'rec', 'sus', 'recsus', 'recsus@email.com', '{USER}', '1030',
        'https://github.com/images/recsus.png',
        'TEN_MICRO'::pricing_method, 'recsus_stripe_id', '2023-01-01T12:00:00.00Z', '2024-03-25T12:00:00.00Z');

insert into "user"(id, first_name, last_name, username, email, roles, github_id, avatar, pricing_method, stripe_id,
                   status, status_updated_at, joined_at)
values ('suspended_id', 'sus', 'pended', 'Suspended', 'suspended@email.com', '{USER}', '1008',
        'https://github.com/images/Suspended.png',
        'TEN_MICRO'::pricing_method, 'suspended_stripe_id', 'SUSPENDED', '2025-03-25T12:00:00.00Z', '2024-03-25t12:00:00.00Z'),
       ('suspended_2_id', 'sus2', 'pended2', 'Suspended2', 'suspended2@email.com', '{USER}', '1020',
        'https://github.com/images/Suspended2.png',
        'TEN_MICRO'::pricing_method, 'suspended_2_stripe_id', 'SUSPENDED', '2025-03-30T12:00:00.00Z', '2024-03-25t12:00:00.00Z'),
       ('suspended_3_id', 'sus3', 'pended3', 'Suspended3', 'suspended3@email.com', '{USER}', '1021',
        'https://github.com/images/Suspended2.png',
        'TEN_MICRO'::pricing_method, 'suspended_3_stripe_id', 'SUSPENDED', null, '2024-03-25t12:00:00.00Z');

insert into "user"(id, first_name, last_name, username, email, roles, github_id, avatar, pricing_method, stripe_id,
                   joined_at,
                   archived, archived_at, status_updated_at)
values ('archived_id', 'ar', 'chived', 'Archived', 'archived@email.com', '{USER}', '1009',
        'https://github.com/images/Archived.png',
        'TEN_MICRO'::pricing_method, 'archived_stripe_id', '2024-03-25T12:00:00.00Z', TRUE, '2025-03-25T12:00:00.00Z',
        '2025-03-25T12:00:00.00Z');

insert into "user"(id, first_name, last_name, username, email, roles, github_id, avatar, pricing_method,
                   joined_at, status_updated_at)
values ('to_upsert_id', 'to', 'upsert', 'ToUpsert', 'to@upsert.com', '{USER}', '1011',
        'https://github.com/images/ToUpsert.png',
        'TEN_MICRO'::pricing_method, '2024-03-25T12:00:00.00Z',
        '2025-03-25T12:00:00.00Z'),
       ('to_upsert_id_2', 'to', 'upsert_2', 'ToUpsert2', 'to.2@upsert.com', '{USER}', '1012',
        'https://github.com/images/ToUpsert.png',
        'TEN_MICRO'::pricing_method, '2024-03-25T12:00:00.00Z',
        '2025-03-25T12:00:00.00Z');

insert into "user"(id, first_name, last_name, username, email, roles, github_id, avatar, pricing_method, stripe_id,
                   status, status_updated_at, archived, archived_at, joined_at)
values ('suspended_4_id', 'sus4', 'pended4', 'Suspended4', 'suspended4@email.com', '{USER}', '1022',
        'https://github.com/images/Suspended4.png',
        'TEN_MICRO'::pricing_method, 'suspended_stripe_id', 'SUSPENDED', '2025-09-18T12:00:00.00Z', TRUE, '2025-09-18T13:00:00.00Z', '2024-03-25T12:00:00.00Z');

-- DEFAULT
update "user" set last_connection = '2025-10-01T14:00:00.00Z';

update "user" set last_connection = '2025-12-22T08:00:00.00Z' where id = 'recsus_id';
update "user" set last_connection = '2025-12-21T08:00:00.00Z' where id = 'admin_id';
update "user" set last_connection = '2025-12-20T08:00:00.00Z' where id = 'denis_ritchie_id';

update "user" set last_connection = '2023-11-20T08:00:00.00Z' where id = 'lorem_ipsum_id';
update "user" set last_connection = '2023-11-21T08:00:00.00Z' where id = 'noobie_id';
update "user" set last_connection = '2023-11-22T08:00:00.00Z' where id = 'to_suspend_id';