package api.poja.io.model;

import java.time.Instant;

public record OrganizationDTO(
    String id,
    String name,
    Instant creationDatetime,
    String ownerId,
    long acceptedAndPendingMembersCount) {}
