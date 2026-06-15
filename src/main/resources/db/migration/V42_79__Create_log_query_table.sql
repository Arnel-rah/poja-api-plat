DROP INDEX IF EXISTS unique_existing_stack_type_per_env;
CREATE UNIQUE INDEX unique_existing_stack_type_depl_per_env
ON "stack" (type, id_environment, app_env_depl_id)
WHERE archived = false;