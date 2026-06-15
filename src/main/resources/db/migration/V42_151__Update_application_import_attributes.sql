alter table "application_import" add column if not exists "github_repository_default_branch" varchar;
alter table "application_import" alter column "status" set not null;