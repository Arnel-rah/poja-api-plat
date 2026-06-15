package api.poja.io.repository.jpa;

import api.poja.io.model.billing.AggregatedBillingInfoByEnvDTOProjection;
import api.poja.io.model.billing.AggregatedBillingInfoByOrgDTOProjection;
import api.poja.io.model.billing.AggregatedBillingInfoByUserDTOProjection;
import api.poja.io.model.billing.AggregatedBillingInfoWithAwsByUserDTOProjection;
import api.poja.io.model.billing.AggregatedOrgBillingInfoByEnvDTOProjection;
import api.poja.io.repository.model.BillingInfo;
import api.poja.io.repository.model.enums.BillingInfoComputeStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface BillingInfoRepository extends JpaRepository<BillingInfo, String> {
  @Query(
      value =
          """
WITH ranked_billing AS (
    SELECT
        b.user_id,
        b.app_id,
        b.env_id,
        b.org_id,
        b.computation_interval_end,
        b.computed_price,
        b.computed_duration_in_minutes,
        ROW_NUMBER() OVER (
            PARTITION BY b.user_id, b.app_id, b.env_id, DATE(b.computation_interval_end)
            ORDER BY b.computation_interval_end DESC
            ) AS row_num
    FROM billing_info b
    WHERE b.org_id = ?1
      AND b.app_id = ?2
      AND b.env_id = ?3
      AND b.status = 'FINISHED'
      AND b.computation_interval_end BETWEEN ?4 AND ?5
),
     latest_billing_per_env_per_day AS (
         SELECT
             r.user_id,
             r.app_id,
             r.env_id,
             r.org_id,
             r.computation_interval_end,
             r.computed_price,
             r.computed_duration_in_minutes
         FROM ranked_billing r
         WHERE r.row_num = 1
     )
SELECT
    SUM(lb.computed_price)        AS "amount",
    MAX(lb.computation_interval_end)     AS "maxComputeDatetime",
    SUM(lb.computed_duration_in_minutes) AS "computedDurationInMinutes",
    o.id                                 AS "orgId",
    a.id                                 AS "appId",
    e.id                                 AS "envId"
FROM "organization" o
         INNER JOIN application a ON a.org_id = o.id
         INNER JOIN environment e ON e.id_application = a.id
         LEFT JOIN latest_billing_per_env_per_day lb
                   ON lb.org_id = o.id
                       AND lb.app_id = a.id
                       AND lb.env_id = e.id
WHERE o.id = ?1
      AND a.id = ?2
      AND e.id = ?3
GROUP BY e.id, a.id, o.id
""",
      nativeQuery = true)
  Optional<AggregatedOrgBillingInfoByEnvDTOProjection> computeSumByEnv(
      String orgId, String appId, String envId, Instant startTime, Instant endTime);

  @Query(
      value =
          """
WITH ranked_billing AS (
    SELECT
        b.user_id,
        b.app_id,
        b.env_id,
        b.org_id,
        b.computation_interval_end,
        b.computed_price,
        b.computed_duration_in_minutes,
        ROW_NUMBER() OVER (
            PARTITION BY b.user_id, b.app_id, b.env_id, DATE(b.computation_interval_end)
            ORDER BY b.computation_interval_end DESC
            ) AS row_num
    FROM billing_info b
    WHERE b.org_id = ?1
      AND b.app_id = ?2
      AND b.status = 'FINISHED'
      AND b.computation_interval_end BETWEEN ?3 AND ?4
),
     latest_billing_per_env_per_day AS (
         SELECT
             r.user_id,
             r.app_id,
             r.env_id,
             r.org_id,
             r.computation_interval_end,
             r.computed_price,
             r.computed_duration_in_minutes
         FROM ranked_billing r
         WHERE r.row_num = 1
     )
SELECT
    SUM(lb.computed_price)        AS "amount",
    MAX(lb.computation_interval_end)     AS "maxComputeDatetime",
    SUM(lb.computed_duration_in_minutes) AS "computedDurationInMinutes",
    o.id                                 AS "orgId",
    a.id                                 AS "appId",
    e.id                                 AS "envId"
FROM "user" u
         LEFT JOIN "organization" o ON o.owner_id = u.id
         LEFT JOIN application a ON a.org_id = o.id
         LEFT JOIN environment e ON e.id_application = a.id
         LEFT JOIN latest_billing_per_env_per_day lb
                   ON lb.org_id = o.id
                       AND lb.app_id = a.id
                       AND lb.env_id = e.id
WHERE o.id = ?1
      AND a.id = ?2
GROUP BY e.id, a.id, o.id
""",
      nativeQuery = true)
  List<AggregatedOrgBillingInfoByEnvDTOProjection> computeSumForOrgByAppGroupedByEnv(
      String orgId, String appId, Instant startTime, Instant endTime);

  @Query(
      value =
          """
WITH ranked_billing AS (
    SELECT
        b.user_id,
        b.app_id,
        b.env_id,
        b.org_id,
        b.computation_interval_end,
        b.computed_price,
        b.computed_memory_duration_in_mb_minutes,
        b.computed_duration_in_minutes,
        ROW_NUMBER() OVER (
            PARTITION BY b.user_id, b.app_id, b.env_id, DATE(b.computation_interval_end)
            ORDER BY b.computation_interval_end DESC
            ) AS row_num
    FROM billing_info b
    WHERE b.user_id = ?1
      AND b.app_id = ?2
      AND b.status = 'FINISHED'
      AND b.computation_interval_end BETWEEN ?3 AND ?4
),
     latest_billing_per_env_per_day AS (
         SELECT
             r.user_id,
             r.app_id,
             r.env_id,
             r.org_id,
             r.computation_interval_end,
             r.computed_price,
             r.computed_memory_duration_in_mb_minutes,
             r.computed_duration_in_minutes
         FROM ranked_billing r
         WHERE r.row_num = 1
     )
SELECT
    SUM(lb.computed_price)                         AS "amount",
    SUM(lb.computed_memory_duration_in_mb_minutes)        AS "memoryDuration",
    MAX(lb.computation_interval_end)                      AS "maxComputeDatetime",
    SUM(lb.computed_duration_in_minutes)                  AS "computedDurationInMinutes",
    u.id                                                  AS "userId",
    a.id                                                  AS "appId",
    e.id                                                  AS "envId"
FROM "user" u
         INNER JOIN "organization" o ON o.owner_id = u.id
         INNER JOIN application a ON a.org_id = o.id
         LEFT JOIN environment e ON e.id_application = a.id
         LEFT JOIN latest_billing_per_env_per_day lb
                   ON lb.org_id = o.id
                       AND lb.app_id = a.id
                       AND lb.env_id = e.id
WHERE u.id = ?1
      AND a.id = ?2
GROUP BY e.id, a.id, u.id
""",
      nativeQuery = true)
  List<AggregatedBillingInfoByEnvDTOProjection> computeSumForUserByAppGroupedByEnv(
      String userId, String appId, Instant startTime, Instant endTime);

  @Query(
      value =
          """
WITH ranked_billing AS (
    SELECT
        b.user_id,
        b.app_id,
        b.env_id,
        b.org_id,
        b.computation_interval_end,
        b.computed_price,
        b.computed_duration_in_minutes,
        ROW_NUMBER() OVER (
            PARTITION BY b.user_id, b.app_id, b.env_id, DATE(b.computation_interval_end)
            ORDER BY b.computation_interval_end DESC
            ) AS row_num
    FROM billing_info b
    WHERE b.user_id = ?1
          AND b.status = 'FINISHED'
          AND b.computation_interval_end BETWEEN ?2 AND ?3
),
     latest_billing_per_env_per_day AS (
         SELECT
             r.user_id,
             r.app_id,
             r.env_id,
             r.org_id,
             r.computation_interval_end,
             r.computed_price,
             r.computed_duration_in_minutes
         FROM ranked_billing r
         WHERE r.row_num = 1
     )
SELECT
    SUM(lb.computed_price)        AS "amount",
    MAX(lb.computation_interval_end)     AS "maxComputeDatetime",
    SUM(lb.computed_duration_in_minutes) AS "computedDurationInMinutes",
    u.id                                 AS "userId",
    o.id                                 AS "orgId",
    u.pricing_method                     AS "pricingMethod"
FROM "user" u
         LEFT JOIN "organization" o ON o.owner_id = u.id
         LEFT JOIN application a ON a.org_id = o.id
         LEFT JOIN environment e ON e.id_application = a.id
         LEFT JOIN latest_billing_per_env_per_day lb
                   ON lb.org_id = o.id
                       AND lb.app_id = a.id
                       AND lb.env_id = e.id
WHERE u.id = ?1
GROUP BY u.id, o.id, u.pricing_method
ORDER BY COALESCE(SUM(lb.computed_price), 0) DESC NULLS LAST,
         u.id DESC
LIMIT ?4 OFFSET ?5
""",
      nativeQuery = true)
  List<AggregatedBillingInfoByOrgDTOProjection> computeSumByOrgGroupedByOrgAndUser(
      String userId, Instant startTime, Instant endTime, int limit, long offset);

  @Query(
      nativeQuery = true,
      value =
          """
WITH ranked_billing AS (
    SELECT
        b.user_id,
        b.app_id,
        b.env_id,
        b.org_id,
        b.computation_interval_end,
        b.computed_price,
        b.computed_duration_in_minutes,
        ROW_NUMBER() OVER (
            PARTITION BY b.user_id, b.app_id, b.env_id, DATE(b.computation_interval_end)
            ORDER BY b.computation_interval_end DESC
            ) AS row_num
    FROM billing_info b
    WHERE b.org_id = ?1
          AND b.status = 'FINISHED'
          AND b.computation_interval_end BETWEEN ?2 AND ?3
),
     latest_billing_per_env_per_day AS (
         SELECT
             r.user_id,
             r.app_id,
             r.env_id,
             r.org_id,
             r.computation_interval_end,
             r.computed_price,
             r.computed_duration_in_minutes
         FROM ranked_billing r
         WHERE r.row_num = 1
     )
SELECT
    SUM(lb.computed_price)        AS "amount",
    MAX(lb.computation_interval_end)     AS "maxComputeDatetime",
    SUM(lb.computed_duration_in_minutes) AS "computedDurationInMinutes",
    u.id                                 AS "userId",
    o.id                                 AS "orgId"
FROM "user" u
         INNER JOIN "organization" o ON o.owner_id = u.id
         INNER JOIN application a ON a.org_id = o.id
         INNER JOIN environment e ON e.id_application = a.id
         INNER JOIN latest_billing_per_env_per_day lb
                   ON lb.org_id = o.id
                       AND lb.app_id = a.id
                       AND lb.env_id = e.id
WHERE o.id = ?1
GROUP BY o.id, u.id
""")
  Optional<AggregatedBillingInfoByOrgDTOProjection> computeSumByOrgGroupedByOrg(
      String orgId, Instant startTime, Instant endTime);

  @Query(
      nativeQuery = true,
      value =
          """
WITH ranked_billing AS (
    SELECT
        b.user_id,
        b.app_id,
        b.env_id,
        b.org_id,
        b.computation_interval_end,
        b.computed_price,
        b.computed_duration_in_minutes,
        ROW_NUMBER() OVER (
            PARTITION BY b.user_id, b.app_id, b.env_id, DATE(b.computation_interval_end)
            ORDER BY b.computation_interval_end DESC
            ) AS row_num
    FROM billing_info b
    WHERE b.user_id = ?1
      AND b.status = 'FINISHED'
      AND b.computation_interval_end BETWEEN ?2 AND ?3
),
     latest_billing_per_env_per_day AS (
         SELECT
             r.user_id,
             r.app_id,
             r.env_id,
             r.org_id,
             r.computation_interval_end,
             r.computed_price,
             r.computed_duration_in_minutes
         FROM ranked_billing r
         WHERE r.row_num = 1
     )
SELECT
    SUM(lb.computed_price)        AS "amount",
    MAX(lb.computation_interval_end)     AS "maxComputeDatetime",
    SUM(lb.computed_duration_in_minutes) AS "computedDurationInMinutes",
    u.id                                 AS "userId",
    u.pricing_method                     AS "pricingMethod"
FROM "user" u
         INNER JOIN "organization" o ON o.owner_id = u.id
         INNER JOIN application a ON a.org_id = o.id
         INNER JOIN environment e ON e.id_application = a.id
         INNER JOIN latest_billing_per_env_per_day lb
                   ON lb.org_id = o.id
                       AND lb.app_id = a.id
                       AND lb.env_id = e.id
WHERE u.id = ?1
GROUP BY u.id, u.pricing_method
""")
  Optional<AggregatedBillingInfoByUserDTOProjection> computeSumByUserGroupedByUser(
      String userId, Instant startTime, Instant endTime);

  @Query(
      nativeQuery = true,
      value =
          """
WITH ranked_billing AS (
    SELECT
        b.user_id,
        b.app_id,
        b.env_id,
        b.org_id,
        b.computation_interval_end,
        b.computed_price,
        b.computed_duration_in_minutes,
        ROW_NUMBER() OVER (
            PARTITION BY b.user_id, b.app_id, b.env_id, DATE(b.computation_interval_end)
            ORDER BY b.computation_interval_end DESC
            ) AS row_num
    FROM billing_info b
    WHERE b.status = 'FINISHED'
      AND b.computation_interval_end BETWEEN ?1 AND ?2
),
     latest_billing_per_env_per_day AS (
         SELECT
             r.user_id,
             r.app_id,
             r.env_id,
             r.org_id,
             r.computation_interval_end,
             r.computed_price,
             r.computed_duration_in_minutes
         FROM ranked_billing r
         WHERE r.row_num = 1
     )
SELECT
  SUM(COALESCE(lb.computed_price, 0))                                                AS "amount",
  MAX(lb.computation_interval_end)                                                          AS "maxComputeDatetime",
  SUM(lb.computed_duration_in_minutes)                                                      AS "computedDurationInMinutes",
  u.id                                                                                      AS "userId",
  u.pricing_method                                                                          AS "pricingMethod",
  uc.amount                                                                             AS "awsCost",
  uc.updated_at                                                                             AS "awsCostUpdateDatetime",
  SUM(COALESCE(lb.computed_price, 0)) - MAX(COALESCE(uc.amount, 0))              AS "costMargin"
FROM "user" u
         LEFT JOIN "user_cost" uc
             ON uc.user_id = u.id
             AND uc.year = ?10
             AND uc.month = ?11
         LEFT JOIN "organization" o ON o.owner_id = u.id
         LEFT JOIN application a ON a.org_id = o.id
         LEFT JOIN environment e ON e.id_application = a.id
         LEFT JOIN latest_billing_per_env_per_day lb
                   ON lb.org_id = o.id
                       AND lb.app_id = a.id
                       AND lb.env_id = e.id
WHERE (?5 is null or u.archived = ?5)
AND LOWER(u.username) LIKE LOWER(CONCAT('%', ?6, '%'))
AND u.joined_at >= coalesce(?8, u.joined_at)
AND u.joined_at <= coalesce(?9, u.joined_at)
GROUP BY u.id, u.pricing_method, uc.amount, uc.updated_at
ORDER BY
CASE WHEN ?7 = 'ASC' THEN COALESCE(SUM(lb.computed_price), 0) END ASC NULLS LAST,
CASE WHEN ?7 = 'DESC' THEN COALESCE(SUM(lb.computed_price), 0) END DESC NULLS LAST,
         u.id DESC
LIMIT ?3 OFFSET ?4
""")
  List<AggregatedBillingInfoWithAwsByUserDTOProjection>
      computeSumByUserGroupedByUserAndSortedByBilling(
          Instant startTime,
          Instant endTime,
          int limit,
          long offset,
          Boolean archived,
          String username,
          String sortOrder,
          Instant joinedFrom,
          Instant joinedTo,
          int year,
          String month);

  @Query(
      nativeQuery = true,
      value =
          """
WITH ranked_billing AS (
SELECT
    b.user_id,
    b.app_id,
    b.env_id,
    b.org_id,
    b.computation_interval_end,
    b.computed_price,
    b.computed_duration_in_minutes,
    ROW_NUMBER() OVER (
        PARTITION BY b.user_id, b.app_id, b.env_id, DATE(b.computation_interval_end)
        ORDER BY b.computation_interval_end DESC
        ) AS row_num
FROM billing_info b
WHERE b.status = 'FINISHED'
  AND b.computation_interval_end BETWEEN ?1 AND ?2
),
 latest_billing_per_env_per_day AS (
     SELECT
         r.user_id,
         r.app_id,
         r.env_id,
         r.org_id,
         r.computation_interval_end,
         r.computed_price,
         r.computed_duration_in_minutes
     FROM ranked_billing r
     WHERE r.row_num = 1
 )
SELECT
  SUM(COALESCE(lb.computed_price, 0))                                                AS "amount",
  MAX(lb.computation_interval_end)                                                          AS "maxComputeDatetime",
  SUM(lb.computed_duration_in_minutes)                                                      AS "computedDurationInMinutes",
  u.id                                                                                      AS "userId",
  u.pricing_method                                                                          AS "pricingMethod",
  uc.amount                                                                             AS "awsCost",
  uc.updated_at                                                                             AS "awsCostUpdateDatetime",
  SUM(COALESCE(lb.computed_price, 0)) - MAX(COALESCE(uc.amount, 0))              AS "costMargin"
FROM "user" u
     LEFT JOIN "user_cost" uc
         ON uc.user_id = u.id
         AND uc.year = ?11
         AND uc.month = ?12
     LEFT JOIN "organization" o ON o.owner_id = u.id
     LEFT JOIN application a ON a.org_id = o.id
     LEFT JOIN environment e ON e.id_application = a.id
     LEFT JOIN latest_billing_per_env_per_day lb
               ON lb.org_id = o.id
                   AND lb.app_id = a.id
                   AND lb.env_id = e.id
WHERE (?5 is null or u.archived = ?5) AND  LOWER(u.username) LIKE LOWER(CONCAT('%', ?6, '%'))
AND u.joined_at >= coalesce(?8, u.joined_at)
AND u.joined_at <= coalesce(?9, u.joined_at)
GROUP BY u.id, u.pricing_method, uc.amount, uc.updated_at, u.status, u.status_updated_at
ORDER BY
CASE
  WHEN u.archived = false AND u.status = 'SUSPENDED' AND u.status_updated_at IS NULL THEN
    CASE WHEN ?7 = 'ASC' THEN 0 ELSE 1 END
  WHEN u.archived = false AND u.status = 'SUSPENDED' AND u.status_updated_at IS NOT NULL THEN
    CASE WHEN ?7 = 'ASC' THEN 1 ELSE 0 END
  ELSE 2
END,
CASE
  WHEN u.archived = false AND u.status = 'SUSPENDED' AND u.status_updated_at IS NOT NULL THEN
    CASE
      WHEN ?7 = 'ASC' THEN EXTRACT(EPOCH FROM (?10 - u.status_updated_at))
      WHEN ?7 = 'DESC' THEN -EXTRACT(EPOCH FROM (?10 - u.status_updated_at))
    END
  ELSE NULL
END ASC,
u.id DESC
LIMIT ?3 OFFSET ?4
""")
  List<AggregatedBillingInfoWithAwsByUserDTOProjection>
      computeSumByUserGroupedByUserAndSortedBySuspension(
          Instant startTime,
          Instant endTime,
          int limit,
          long offset,
          Boolean archived,
          String username,
          String sortOrder,
          Instant joinedFrom,
          Instant joinedTo,
          Instant now,
          int year,
          String month);

  @Query(
      nativeQuery = true,
      value =
          """
WITH ranked_billing AS (
SELECT
    b.user_id,
    b.app_id,
    b.env_id,
    b.org_id,
    b.computation_interval_end,
    b.computed_price,
    b.computed_duration_in_minutes,
    ROW_NUMBER() OVER (
        PARTITION BY b.user_id, b.app_id, b.env_id, DATE(b.computation_interval_end)
        ORDER BY b.computation_interval_end DESC
        ) AS row_num
FROM billing_info b
WHERE b.status = 'FINISHED'
  AND b.computation_interval_end BETWEEN ?1 AND ?2
),
 latest_billing_per_env_per_day AS (
     SELECT
         r.user_id,
         r.app_id,
         r.env_id,
         r.org_id,
         r.computation_interval_end,
         r.computed_price,
         r.computed_duration_in_minutes
     FROM ranked_billing r
     WHERE r.row_num = 1
 )
SELECT
  SUM(COALESCE(lb.computed_price, 0))                                                AS "amount",
  MAX(lb.computation_interval_end)                                                          AS "maxComputeDatetime",
  SUM(lb.computed_duration_in_minutes)                                                      AS "computedDurationInMinutes",
  u.id                                                                                      AS "userId",
  u.pricing_method                                                                          AS "pricingMethod",
  uc.amount                                                                             AS "awsCost",
  uc.updated_at                                                                             AS "awsCostUpdateDatetime",
  SUM(COALESCE(lb.computed_price, 0)) - MAX(COALESCE(uc.amount, 0))              AS "costMargin"
FROM "user" u
     LEFT JOIN "user_cost" uc
         ON uc.user_id = u.id
         AND uc.year = ?10
         AND uc.month = ?11
     LEFT JOIN "organization" o ON o.owner_id = u.id
     LEFT JOIN application a ON a.org_id = o.id
     LEFT JOIN environment e ON e.id_application = a.id
     LEFT JOIN latest_billing_per_env_per_day lb
               ON lb.org_id = o.id
                   AND lb.app_id = a.id
                   AND lb.env_id = e.id
WHERE (?5 is null or u.archived = ?5)
AND LOWER(u.username) LIKE LOWER(CONCAT('%', ?6, '%'))
AND u.joined_at >= coalesce(?8, u.joined_at)
AND u.joined_at <= coalesce(?9, u.joined_at)
GROUP BY u.id, u.pricing_method, uc.amount, uc.updated_at, u.joined_at
ORDER BY
CASE WHEN ?7 = 'ASC' THEN u.joined_at END ASC,
CASE WHEN ?7 = 'DESC' THEN u.joined_at END DESC,
u.id DESC
LIMIT ?3 OFFSET ?4
""")
  List<AggregatedBillingInfoWithAwsByUserDTOProjection>
      computeSumByUserGroupedByUserAndSortedByJoinDate(
          Instant startTime,
          Instant endTime,
          int limit,
          long offset,
          Boolean archived,
          String username,
          String sortOrder,
          Instant joinedFrom,
          Instant joinedTo,
          int year,
          String month);

  @Query(
      nativeQuery = true,
      value =
          """
WITH ranked_billing AS (
SELECT
    b.user_id,
    b.app_id,
    b.env_id,
    b.org_id,
    b.computation_interval_end,
    b.computed_price,
    b.computed_duration_in_minutes,
    ROW_NUMBER() OVER (
            PARTITION BY b.user_id, b.app_id, b.env_id, DATE(b.computation_interval_end)
            ORDER BY b.computation_interval_end DESC
            ) AS row_num
FROM billing_info b
WHERE b.status = 'FINISHED'
AND b.computation_interval_end BETWEEN ?1 AND ?2
),
 latest_billing_per_env_per_day AS (
    SELECT
        r.user_id,
        r.app_id,
        r.env_id,
        r.org_id,
        r.computation_interval_end,
        r.computed_price,
        r.computed_duration_in_minutes
    FROM ranked_billing r
    WHERE r.row_num = 1
)
SELECT
  SUM(COALESCE(lb.computed_price, 0))                                                AS "amount",
  MAX(lb.computation_interval_end)                                                          AS "maxComputeDatetime",
  SUM(lb.computed_duration_in_minutes)                                                      AS "computedDurationInMinutes",
  u.id                                                                                      AS "userId",
  u.pricing_method                                                                          AS "pricingMethod",
  uc.amount                                                                             AS "awsCost",
  uc.updated_at                                                                             AS "awsCostUpdateDatetime",
  SUM(COALESCE(lb.computed_price, 0)) - MAX(COALESCE(uc.amount, 0))              AS "costMargin"
FROM "user" u
    LEFT JOIN "user_cost" uc
        ON uc.user_id = u.id
        AND uc.year = ?10
        AND uc.month = ?11
    LEFT JOIN "organization" o ON o.owner_id = u.id
    LEFT JOIN application a ON a.org_id = o.id
    LEFT JOIN environment e ON e.id_application = a.id
    LEFT JOIN latest_billing_per_env_per_day lb
        ON lb.org_id = o.id
            AND lb.app_id = a.id
            AND lb.env_id = e.id
WHERE (?5 is null or u.archived = ?5)
AND LOWER(u.username) LIKE LOWER(CONCAT('%', ?6, '%'))
AND u.joined_at >= coalesce(?8, u.joined_at)
AND u.joined_at <= coalesce(?9, u.joined_at)
GROUP BY u.id, u.pricing_method, uc.amount, uc.updated_at, u.last_connection
ORDER BY
CASE WHEN ?7 = 'ASC' THEN u.last_connection END ASC NULLS LAST,
CASE WHEN ?7 = 'DESC' THEN u.last_connection END DESC NULLS LAST,
u.id DESC
LIMIT ?3 OFFSET ?4
""")
  List<AggregatedBillingInfoWithAwsByUserDTOProjection>
      computeSumByUserGroupedByUserAndSortedByLastConnection(
          Instant startTime,
          Instant endTime,
          int limit,
          long offset,
          Boolean archived,
          String username,
          String sortOrder,
          Instant joinedFrom,
          Instant joinedTo,
          int year,
          String month);

  @Query(
      nativeQuery = true,
      value =
          """
WITH ranked_billing AS (
SELECT
b.user_id,
b.app_id,
b.env_id,
b.org_id,
b.computation_interval_end,
b.computed_price,
b.computed_duration_in_minutes,
ROW_NUMBER() OVER (
        PARTITION BY b.user_id, b.app_id, b.env_id, DATE(b.computation_interval_end)
        ORDER BY b.computation_interval_end DESC
        ) AS row_num
FROM billing_info b
WHERE b.status = 'FINISHED'
AND b.computation_interval_end BETWEEN ?1 AND ?2
),
latest_billing_per_env_per_day AS (
SELECT
    r.user_id,
    r.app_id,
    r.env_id,
    r.org_id,
    r.computation_interval_end,
    r.computed_price,
    r.computed_duration_in_minutes
FROM ranked_billing r
WHERE r.row_num = 1
)
SELECT
  SUM(COALESCE(lb.computed_price, 0))                                                AS "amount",
  MAX(lb.computation_interval_end)                                                          AS "maxComputeDatetime",
  SUM(lb.computed_duration_in_minutes)                                                      AS "computedDurationInMinutes",
  u.id                                                                                      AS "userId",
  u.pricing_method                                                                          AS "pricingMethod",
  uc.amount                                                                             AS "awsCost",
  uc.updated_at                                                                             AS "awsCostUpdateDatetime",
  SUM(COALESCE(lb.computed_price, 0)) - MAX(COALESCE(uc.amount, 0))              AS "costMargin"
FROM "user" u
LEFT JOIN "user_cost" uc
    ON uc.user_id = u.id
    AND uc.year = ?10
    AND uc.month = ?11
LEFT JOIN "organization" o ON o.owner_id = u.id
LEFT JOIN application a ON a.org_id = o.id
LEFT JOIN environment e ON e.id_application = a.id
LEFT JOIN latest_billing_per_env_per_day lb
    ON lb.org_id = o.id
        AND lb.app_id = a.id
        AND lb.env_id = e.id
WHERE (?5 is null or u.archived = ?5)
AND LOWER(u.username) LIKE LOWER(CONCAT('%', ?6, '%'))
AND u.joined_at >= coalesce(?8, u.joined_at)
AND u.joined_at <= coalesce(?9, u.joined_at)
GROUP BY u.id, u.pricing_method, uc.amount, uc.updated_at
ORDER BY
CASE WHEN ?7 = 'ASC' THEN MAX(uc.amount) END ASC NULLS LAST,
CASE WHEN ?7 = 'DESC' THEN MAX(uc.amount) END DESC NULLS LAST
LIMIT ?3 OFFSET ?4
""")
  List<AggregatedBillingInfoWithAwsByUserDTOProjection>
      computeSumByUserGroupedByUserAndSortedByUserCost(
          Instant startTime,
          Instant endTime,
          int limit,
          long offset,
          Boolean archived,
          String username,
          String sortOrder,
          Instant joinedFrom,
          Instant joinedTo,
          int year,
          String month);

  @Query(
      nativeQuery = true,
      value =
          """
WITH ranked_billing AS (
SELECT
b.user_id,
b.app_id,
b.env_id,
b.org_id,
b.computation_interval_end,
b.computed_price,
b.computed_duration_in_minutes,
ROW_NUMBER() OVER (
        PARTITION BY b.user_id, b.app_id, b.env_id, DATE(b.computation_interval_end)
        ORDER BY b.computation_interval_end DESC
        ) AS row_num
FROM billing_info b
WHERE b.status = 'FINISHED'
AND b.computation_interval_end BETWEEN ?1 AND ?2
),
latest_billing_per_env_per_day AS (
SELECT
    r.user_id,
    r.app_id,
    r.env_id,
    r.org_id,
    r.computation_interval_end,
    r.computed_price,
    r.computed_duration_in_minutes
FROM ranked_billing r
WHERE r.row_num = 1
)
SELECT
      SUM(COALESCE(lb.computed_price, 0))                                                AS "amount",
      MAX(lb.computation_interval_end)                                                          AS "maxComputeDatetime",
      SUM(lb.computed_duration_in_minutes)                                                      AS "computedDurationInMinutes",
      u.id                                                                                      AS "userId",
      u.pricing_method                                                                          AS "pricingMethod",
      uc.amount                                                                             AS "awsCost",
      uc.updated_at                                                                             AS "awsCostUpdateDatetime",
      SUM(COALESCE(lb.computed_price, 0)) - MAX(COALESCE(uc.amount, 0))              AS "costMargin"
FROM "user" u
LEFT JOIN "user_cost" uc
    ON uc.user_id = u.id
    AND uc.year = ?10
    AND uc.month = ?11
LEFT JOIN "organization" o ON o.owner_id = u.id
LEFT JOIN application a ON a.org_id = o.id
LEFT JOIN environment e ON e.id_application = a.id
LEFT JOIN latest_billing_per_env_per_day lb
    ON lb.org_id = o.id
        AND lb.app_id = a.id
        AND lb.env_id = e.id
WHERE (?5 is null or u.archived = ?5)
AND LOWER(u.username) LIKE LOWER(CONCAT('%', ?6, '%'))
AND u.joined_at >= coalesce(?8, u.joined_at)
AND u.joined_at <= coalesce(?9, u.joined_at)
GROUP BY u.id, u.pricing_method, uc.amount, uc.updated_at
ORDER BY
CASE
  WHEN uc.amount IS NULL THEN NULL
  WHEN ?7 = 'ASC' THEN
    COALESCE(SUM(lb.computed_price), 0) - uc.amount
END ASC NULLS LAST,

CASE
  WHEN uc.amount IS NULL THEN NULL
  WHEN ?7 = 'DESC' THEN
    COALESCE(SUM(lb.computed_price), 0) - uc.amount
END DESC NULLS LAST,
u.id DESC
LIMIT ?3 OFFSET ?4
""")
  List<AggregatedBillingInfoWithAwsByUserDTOProjection>
      computeSumByUserGroupedByUserAndSortedByUserCostMargin(
          Instant startTime,
          Instant endTime,
          int limit,
          long offset,
          Boolean archived,
          String username,
          String sortOrder,
          Instant joinedFrom,
          Instant joinedTo,
          int year,
          String month);

  Optional<BillingInfo> findByQueryId(String queryId);

  @Modifying
  @Query(
      "UPDATE BillingInfo b SET b.status = ?1, b.computeDatetime = ?2, b.computedDurationInMinutes"
          + " = ?3, b.computedPrice = ?4, b.computedMemoryDurationInMbMinutes = ?5 WHERE b.id"
          + " = ?6")
  void updateBillingInfoAttributes(
      BillingInfoComputeStatus status,
      Instant computeDatetime,
      double computedDurationInMinutes,
      BigDecimal computedPrice,
      BigDecimal computedMemoryDurationInMbMinutes,
      String id);

  Optional<BillingInfo> findByAppIdAndEnvIdAndId(String appId, String envId, String id);
}
