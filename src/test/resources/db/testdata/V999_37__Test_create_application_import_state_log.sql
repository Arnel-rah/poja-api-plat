insert into "application_import_log"(type, message, timestamp, state_id)
values

-- GRADLE_BUILD_FILE_CONFLICTS_RESOLUTION_IN_PROGRESS (import_1_state_09)
('INFO', 'Resolved dependency version conflict: spring-boot-starter-web 3.2.0 → 3.2.1', '2024-09-01 08:50:56',
 'import_1_state_09'),
('INFO', 'Merged build.gradle blocks: repositories + dependencies', '2024-09-01 08:50:57', 'import_1_state_09'),

-- CODE_GENERATION_IN_PROGRESS (import_1_state_13)
('INFO', 'Starting code generation using Poja template v4.1', '2024-09-01 08:53:41', 'import_1_state_13'),
('WARNING', 'Unused user-defined controller detected; skipping Poja override', '2024-09-01 08:53:42',
 'import_1_state_13'),

-- GENERATED_CODE_INTEGRATION_IN_PROGRESS (import_1_state_14)
('INFO', 'Integrating generated code into user repository structure', '2024-09-01 08:54:19', 'import_1_state_14'),
('INFO', 'Resolved file overwrite rules for src/main/java/api package', '2024-09-01 08:54:20', 'import_1_state_14');


insert into "application_import_log"(type, message, timestamp, state_id)
values ('ERROR', 'Failed to detect a supported build tool automatically.', '2024-09-01 10:16:13', 'import_6_state_06'),
       ('ERROR', 'Unable to identify a supported build tool from project files; aborting Gradle conversion.',
        '2024-09-01 10:16:14',
        'import_6_state_06'),
       ('INFO', 'Stopping process and marking import as failed.', '2024-09-01 10:16:15', 'import_6_state_06');
