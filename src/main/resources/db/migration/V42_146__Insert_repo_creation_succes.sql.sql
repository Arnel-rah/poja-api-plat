INSERT INTO app_setup_state (id, progression_status, "timestamp", execution_type, app_id, org_id)
SELECT DISTINCT
    uuid_generate_v4(),
    'REPO_CREATION_SUCCESS'::app_setup_state_status,
    now(),
    'ASYNCHRONOUS'::execution_type,
    a.id,
    a.org_id
FROM application a
WHERE a.archived = false
  AND a.repo_http_url IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM app_setup_state s
    WHERE s.app_id = a.id
);
