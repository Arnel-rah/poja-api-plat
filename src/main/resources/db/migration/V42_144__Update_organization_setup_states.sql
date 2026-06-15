begin;

insert into organization_setup_state (progression_status, timestamp, execution_type, org_id)
select case
           when o.console_username is not null
               and o.console_password is not null
               and o.console_account_id is not null
               then 'ORGANIZATION_SETUP_COMPLETED'::organization_setup_state_status
           else 'ORGANIZATION_SETUP_FAILED'::organization_setup_state_status end,
       now(),
       'ASYNCHRONOUS',
       o.id
from "organization" o;

commit;
