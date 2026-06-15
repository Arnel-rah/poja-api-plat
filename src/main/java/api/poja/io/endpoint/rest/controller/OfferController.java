package api.poja.io.endpoint.rest.controller;

import api.poja.io.endpoint.rest.mapper.OfferMapper;
import api.poja.io.endpoint.rest.model.GetOffersResponse;
import api.poja.io.endpoint.rest.model.Offer;
import api.poja.io.service.OfferService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class OfferController {
  private final OfferService service;
  private final OfferMapper mapper;

  @GetMapping("/offers")
  public GetOffersResponse getOffers() {
    return new GetOffersResponse().data(service.findAll().stream().map(mapper::toRest).toList());
  }

  @GetMapping("/offers/{offer_id}")
  public Offer getOfferById(@PathVariable(name = "offer_id") String offerId) {
    return mapper.toRest(service.getById(offerId));
  }
}
