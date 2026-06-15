alter table "application_import"
    add column github_repository_id varchar,
    add column github_repository_description varchar,
    add column github_repository_private boolean default false,
    add column user_id varchar not null,
        add constraint fk_application_import_user_id foreign key (user_id) references "user"(id),
    add column created_app_id varchar,
        add constraint fk_application_import_application_id foreign key (created_app_id) references "application"(id);