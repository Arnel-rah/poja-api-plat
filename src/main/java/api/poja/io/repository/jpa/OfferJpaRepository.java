package api.poja.io.repository.jpa;

import api.poja.io.model.OfferDTOProjection;
import api.poja.io.repository.model.Offer;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OfferJpaRepository extends JpaRepository<Offer, String> {
  @Query(
      value =
          // todo: map with invoice and verify invoice status in count
          """
       with active_subscriptions as (
  select s1.offer_id, count(distinct s1.user_id) as count from "user_subscription" s1 left join "invoice" i on s1.invoice_id = i.id where
  extract('year' from s1.subscription_begin_datetime) = extract('year' from cast(?1 as date))
  and extract('month' from s1.subscription_begin_datetime) = extract('month' from cast(?1 as date))
  and i.status = 'PAID'
  and s1.subscription_begin_datetime <= ?1
  and (s1.subscription_end_datetime is null or (s1.subscription_end_datetime is not null and s1.subscription_end_datetime > ?1))
  group by offer_id
)
select
                  o.id as id,
                  o.name as name,
                  o.max_apps as maxApps,
                  o.price as price,
                  coalesce(s.count,0) as subscribedUsersCount
              FROM "offer" o
              LEFT JOIN active_subscriptions s ON s.offer_id = o.id
              where o.id = ?2
""",
      nativeQuery = true)
  Optional<OfferDTOProjection> findByIdWithCount(Instant now, String id);

  @Query(
      value = // todo: map with invoice and verify invoice status in count
          """
with active_subscriptions as (
  select s1.offer_id, count(distinct s1.user_id) as count from "user_subscription" s1 left join "invoice" i on s1.invoice_id = i.id where
  extract('year' from s1.subscription_begin_datetime) = extract('year' from cast(?1 as date))
  and extract('month' from s1.subscription_begin_datetime) = extract('month' from cast(?1 as date))
  and i.status = 'PAID'
  and s1.subscription_begin_datetime <= ?1
  and (s1.subscription_end_datetime is null or (s1.subscription_end_datetime is not null and s1.subscription_end_datetime > ?1))
  group by offer_id
)
select
                  o.id as id,
                  o.name as name,
                  o.max_apps as maxApps,
                  o.price as price,
                  coalesce(s.count,0) as subscribedUsersCount
              FROM "offer" o
              LEFT JOIN active_subscriptions s ON s.offer_id = o.id
""",
      nativeQuery = true)
  List<OfferDTOProjection> findAllWithCount(Instant now);
}
