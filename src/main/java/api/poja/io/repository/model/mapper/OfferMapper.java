package api.poja.io.repository.model.mapper;

import api.poja.io.model.OfferDto;
import api.poja.io.repository.model.Offer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component("DomainOfferMapper")
@AllArgsConstructor
public class OfferMapper {
  public OfferDto toModel(Offer domain) {
    return OfferDto.builder()
        .id(domain.getId())
        .name(domain.getName())
        .price(domain.getPrice())
        .maxApps(domain.getMaxApps())
        .maxSubscribers(0)
        .subscribedUsers(0)
        .build();
  }
}
