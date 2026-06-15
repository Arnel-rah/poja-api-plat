begin;

update "application" a
    set org_id = u.main_org_id
from "user" u
where a.id_user = u.id;

commit;