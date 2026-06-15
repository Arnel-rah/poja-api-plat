package api.poja.io.model;

import java.math.BigDecimal;

public interface OfferDTOProjection {
  default OfferDto getOffer() {
    return new OfferDto(getId(), getName(), getMaxApps(), getPrice(), 0, getSubscribedUsersCount());
  }

  String getId();

  String getName();

  long getMaxApps();

  BigDecimal getPrice();

  long getSubscribedUsersCount();
}
