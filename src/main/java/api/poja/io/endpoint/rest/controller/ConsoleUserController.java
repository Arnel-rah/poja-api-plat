package api.poja.io.endpoint.rest.controller;

import api.poja.io.endpoint.rest.mapper.ConsoleUserMapper;
import api.poja.io.endpoint.rest.model.ConsoleUser;
import api.poja.io.service.ConsoleUserService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class ConsoleUserController {
  private final ConsoleUserService consoleUserService;
  private final ConsoleUserMapper consoleUserMapper;

  @GetMapping("/orgs/{orgId}/consoleUser")
  public ConsoleUser getConsoleUser(@PathVariable String orgId) {
    var domain = consoleUserService.getConsoleUser(orgId);
    return consoleUserMapper.toRest(orgId, domain);
  }
}
