alter type deployment_progression_status add value 'CODE_GENERATION_IN_PROGRESS';
alter type deployment_progression_status add value 'CODE_PUSH_IN_PROGRESS';
alter type deployment_progression_status add value 'CODE_PUSH_FAILED';
alter type deployment_progression_status add value 'CODE_PUSH_SUCCESS';
alter type deployment_progression_status add value 'DEPLOYMENT_WORKFLOW_IN_PROGRESS';
alter type deployment_progression_status add value 'DEPLOYMENT_WORKFLOW_FAILED';