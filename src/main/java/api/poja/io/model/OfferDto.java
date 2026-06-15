package api.poja.io.model;

import api.poja.io.repository.model.Offer;
import java.math.BigDecimal;
import lombok.Builder;

@Builder(toBuilder = true)
public record OfferDto(
    String id,
    String name,
    long maxApps,
    BigDecimal price,
    long maxSubscribers,
    long subscribedUsers) {
  public boolean canBeSubscribed() {
    return subscribedUsers() < maxSubscribers();
  }

  public Offer getOffer() {
    return new Offer(id, name, maxApps, price);
  }
}
