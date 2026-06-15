create table if not exists "application_template" (
    id varchar
       constraint pk_application_template primary key default uuid_generate_v4(),
    "name" varchar,
    description varchar,
    repository_url varchar,
    demo_url varchar
);

