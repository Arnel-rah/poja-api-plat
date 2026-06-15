begin;

insert into "user_state" (progression_status, timestamp, execution_type, user_id)
select u.status::text::user_progression_status, u.status_updated_at,
       'SYNCHRONOUS',
       u.id
from "user" u
where u.status != 'UNDER_MODIFICATION'
  and not exists (
select 1
from user_state us
where us.user_id = u.id);

commit;