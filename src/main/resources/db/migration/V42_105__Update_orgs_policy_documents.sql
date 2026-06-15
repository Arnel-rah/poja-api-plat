UPDATE "organization"
SET console_policy_document_name = name || '-logPolicies'
WHERE console_policy_document_name IS NULL;
