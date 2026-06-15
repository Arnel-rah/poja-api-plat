INSERT INTO "app_setup_state" (id, progression_status, "timestamp", execution_type, app_id, org_id)
SELECT
    uuid_generate_v4(),
    'ENV_DEPLOYMENT_INITIATED'::app_setup_state_status,
    now(),
    'ASYNCHRONOUS'::execution_type,
    t.app_id,
    t.org_id
FROM (
         SELECT DISTINCT d.app_id, a.org_id
         FROM app_environment_deployment d
                  JOIN application a ON a.id = d.app_id
         WHERE a.archived = false
           AND NOT EXISTS (
             SELECT 1
             FROM app_setup_state s
             WHERE s.app_id = d.app_id
               AND s.progression_status = 'ENV_DEPLOYMENT_INITIATED'::app_setup_state_status
         )
     ) t;