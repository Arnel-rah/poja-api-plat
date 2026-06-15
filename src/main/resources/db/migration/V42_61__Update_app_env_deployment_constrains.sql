alter table app_environment_deployment alter column env_depl_conf_id drop not null;
alter table app_environment_deployment alter column gh_commit_message drop not null;
alter table app_environment_deployment alter column gh_commit_sha drop not null;