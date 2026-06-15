package api.poja.io.endpoint.rest.controller;

import api.poja.io.endpoint.rest.mapper.PojaVersionMapper;
import api.poja.io.endpoint.rest.model.CrupdatePojaVersionChangelogRequestBody;
import api.poja.io.endpoint.rest.model.PojaVersion;
import api.poja.io.endpoint.rest.model.PojaVersionsResponse;
import api.poja.io.endpoint.validator.CrupdatePojaVersionChangelogValidator;
import api.poja.io.service.PojaVersionService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class PojaVersionsController {
  private final PojaVersionService service;
  private final PojaVersionMapper mapper;
  private final CrupdatePojaVersionChangelogValidator crupdatePojaVersionChangelogValidator;

  @GetMapping("/poja-versions")
  public PojaVersionsResponse getPojaVersions() {
    var data = service.findAll().stream().map(mapper::toRest).toList();
    return new PojaVersionsResponse().data(data);
  }

  @PutMapping("/poja-versions/{pojaVersion}/changelog")
  public PojaVersion crupdatePojaVersionChangelog(
      @PathVariable String pojaVersion,
      @RequestBody CrupdatePojaVersionChangelogRequestBody requestBody) {
    crupdatePojaVersionChangelogValidator.accept(pojaVersion, requestBody);
    return mapper.toRest(service.updateChangelog(pojaVersion, requestBody.getChangelogMd()));
  }
}
