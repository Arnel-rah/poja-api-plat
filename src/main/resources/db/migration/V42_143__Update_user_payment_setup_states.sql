begin;

insert into user_payment_setup_state (progression_status, timestamp, execution_type, user_id)
select case
           when u.stripe_id is not null
               then 'PAYMENT_SETUP_COMPLETED'::user_payment_setup_status
           else 'PAYMENT_SETUP_FAILED'::user_payment_setup_status end,
       now(),
       'ASYNCHRONOUS',
       u.id
from "user" u;

commit;
