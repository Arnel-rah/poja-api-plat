begin;

delete from "user_payment_setup_state" s where s.user_id = 'to_upsert_id' or s.user_id = 'to_upsert_id_2';

commit;