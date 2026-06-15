do
$$
begin
        if not exists(select from pg_type where typname = 'application_import_state_status') then
            create type application_import_state_status as enum (
                'APPLICATION_LANGUAGE_VERIFICATION_IN_PROGRESS',
                'APPLICATION_LANGUAGE_VERIFICATION_FAILED',
                'APPLICATION_LANGUAGE_VERIFICATION_SUCCESSFUL',
                'BUILD_TOOL_VERIFICATION_IN_PROGRESS',
                'BUILD_TOOL_VERIFICATION_FAILED',
                'BUILD_TOOL_VERIFICATION_SUCCESSFUL',
                'CONVERSION_TO_GRADLE_IN_PROGRESS',
                'CONVERSION_TO_GRADLE_FAILED',
                'CONVERSION_TO_GRADLE_SUCCESSFUL',
                'GRADLE_BUILD_FILE_COMPATIBILITY_CHECK_IN_PROGRESS',
                'GRADLE_BUILD_FILE_COMPATIBILITY_CHECK_SUCCESSFUL',
                'GRADLE_BUILD_FILE_COMPATIBILITY_CHECK_FAILED',
                'GRADLE_BUILD_FILE_CONFLICTS_RESOLUTION_IN_PROGRESS',
                'GRADLE_BUILD_FILE_CONFLICTS_RESOLUTION_FAILED',
                'GRADLE_BUILD_FILE_CONFLICTS_RESOLVED',
                'CODE_GENERATION_IN_PROGRESS',
                'GENERATED_CODE_INTEGRATION_IN_PROGRESS',
                'GENERATED_CODE_INTEGRATION_FAILED',
                'GENERATED_CODE_INTEGRATED',
                'CODE_PUSH_IN_PROGRESS',
                'CODE_PUSH_FAILED',
                'CODE_PUSH_SUCCESSFUL',
                'TEST_RUN_VERIFICATION_IN_PROGRESS',
                'TEST_RUN_VERIFICATION_FAILED',
                'TEST_RUN_VERIFICATION_SUCCESSFUL',
                'TEST_PING_ENDPOINT_IN_PROGRESS',
                'TEST_PING_ENDPOINT_FAILED',
                'TEST_PING_ENDPOINT_SUCCESSFUL'
            );
        end if;
end;
$$;

create table if not exists "application_import_state"(
    id varchar constraint pk_app_import_state primary key default uuid_generate_v4(),
    progression_status application_import_state_status not null,
    "timestamp" timestamp without time zone,
    execution_type execution_type not null,
    import_id varchar not null,
    constraint fk_app_import foreign key (import_id) references "application_import"(id)
);