insert into "application_import_state"(id, progression_status, timestamp, execution_type, import_id)
values
    ('import_1_state_01', 'APPLICATION_LANGUAGE_VERIFICATION_IN_PROGRESS',            '2024-09-01 08:43:12', 'ASYNCHRONOUS', 'import_1'),
    ('import_1_state_02', 'APPLICATION_LANGUAGE_VERIFICATION_SUCCESSFUL',             '2024-09-01 08:44:03', 'ASYNCHRONOUS', 'import_1'),

    ('import_1_state_03', 'BUILD_TOOL_VERIFICATION_IN_PROGRESS',                      '2024-09-01 08:45:10', 'ASYNCHRONOUS', 'import_1'),
    ('import_1_state_04', 'BUILD_TOOL_VERIFICATION_SUCCESSFUL',                       '2024-09-01 08:46:01', 'ASYNCHRONOUS', 'import_1'),

    ('import_1_state_05', 'PRE_TRANSFORMATION_TEST_RUN_VERIFICATION_IN_PROGRESS',     '2024-09-01 08:46:50', 'ASYNCHRONOUS', 'import_1'),
    ('import_1_state_06', 'PRE_TRANSFORMATION_TEST_RUN_VERIFICATION_SUCCESSFUL',      '2024-09-01 08:47:30', 'ASYNCHRONOUS', 'import_1'),

    ('import_1_state_07', 'CONVERSION_TO_GRADLE_IN_PROGRESS',                         '2024-09-01 08:47:19', 'ASYNCHRONOUS', 'import_1'),
    ('import_1_state_08', 'CONVERSION_TO_GRADLE_SUCCESSFUL',                          '2024-09-01 08:48:02', 'ASYNCHRONOUS', 'import_1'),

    ('import_1_state_09', 'GRADLE_BUILD_FILE_CONFLICTS_RESOLUTION_IN_PROGRESS',       '2024-09-01 08:50:55', 'ASYNCHRONOUS', 'import_1'),
    ('import_1_state_10', 'GRADLE_BUILD_FILE_CONFLICTS_RESOLVED',                     '2024-09-01 08:52:12', 'ASYNCHRONOUS', 'import_1'),

    ('import_1_state_11', 'POJA_CONF_GENERATION_IN_PROGRESS',                          '2024-09-01 08:52:45', 'ASYNCHRONOUS', 'import_1'),
    ('import_1_state_12', 'POJA_CONF_GENERATED',                                       '2024-09-01 08:53:20', 'ASYNCHRONOUS', 'import_1'),

    ('import_1_state_13', 'CODE_GENERATION_IN_PROGRESS',                                '2024-09-01 08:53:40', 'ASYNCHRONOUS', 'import_1'),
    ('import_1_state_14', 'GENERATED_CODE_INTEGRATION_IN_PROGRESS',                     '2024-09-01 08:54:18', 'ASYNCHRONOUS', 'import_1'),
    ('import_1_state_15', 'GENERATED_CODE_INTEGRATED',                                  '2024-09-01 08:55:33', 'ASYNCHRONOUS', 'import_1'),

    ('import_1_state_18', 'TEST_PING_ENDPOINT_IN_PROGRESS',                             '2024-09-01 09:00:14', 'ASYNCHRONOUS', 'import_1'),
    ('import_1_state_19', 'TEST_PING_ENDPOINT_SUCCESSFUL',                              '2024-09-01 09:01:09', 'ASYNCHRONOUS', 'import_1');

insert into "application_import_state"(id, progression_status, timestamp, execution_type, import_id)
values
    ('import_6_state_01', 'APPLICATION_LANGUAGE_VERIFICATION_IN_PROGRESS',            '2024-09-01 10:12:05', 'ASYNCHRONOUS', 'import_6'),
    ('import_6_state_02', 'APPLICATION_LANGUAGE_VERIFICATION_SUCCESSFUL',             '2024-09-01 10:12:44', 'ASYNCHRONOUS', 'import_6'),
    ('import_6_state_03', 'BUILD_TOOL_VERIFICATION_IN_PROGRESS',                      '2024-09-01 10:14:01', 'ASYNCHRONOUS', 'import_6'),
    ('import_6_state_04', 'BUILD_TOOL_VERIFICATION_SUCCESSFUL',                       '2024-09-01 10:14:47', 'ASYNCHRONOUS', 'import_6'),
    ('import_6_state_05', 'CONVERSION_TO_GRADLE_IN_PROGRESS',                         '2024-09-01 10:16:12', 'ASYNCHRONOUS', 'import_6'),
    ('import_6_state_06', 'CONVERSION_TO_GRADLE_FAILED',                              '2024-09-01 10:16:29', 'ASYNCHRONOUS', 'import_6');