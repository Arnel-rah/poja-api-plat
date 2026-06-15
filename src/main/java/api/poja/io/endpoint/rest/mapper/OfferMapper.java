package api.poja.io.endpoint.rest.mapper;

import api.poja.io.endpoint.rest.model.Offer;
import api.poja.io.model.OfferDto;
import org.springframework.stereotype.Component;

@Component
public class OfferMapper {
  public Offer toRest(OfferDto domain) {
    return new Offer()
        .id(domain.id())
        .name(domain.name())
        .maxApps(domain.maxApps())
        .nbMaxSubscribers(domain.maxSubscribers())
        .remainingPlaces(domain.maxSubscribers() - domain.subscribedUsers())
        .price(domain.price());
  }
}
