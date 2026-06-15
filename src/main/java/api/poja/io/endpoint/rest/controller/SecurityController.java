package api.poja.io.endpoint.rest.controller;

import static java.time.Instant.now;

import api.poja.io.endpoint.rest.mapper.UserMapper;
import api.poja.io.endpoint.rest.model.RefreshToken;
import api.poja.io.endpoint.rest.model.Token;
import api.poja.io.endpoint.rest.model.Whoami;
import api.poja.io.endpoint.rest.security.model.Principal;
import api.poja.io.service.UserService;
import api.poja.io.service.github.GithubService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class SecurityController {
  private final GithubService githubService;
  private final UserService userService;
  private final UserMapper userMapper;

  @GetMapping("/whoami")
  public Whoami whoami(@AuthenticationPrincipal Principal principal) {
    var user = principal.getUser();
    userService.updateLastConnection(user, now());
    return new Whoami().user(userMapper.toRest(user));
  }

  @GetMapping("/token")
  public Token exchangeCodeToToken(@RequestParam String code) {
    return githubService.exchangeCodeToToken(code);
  }

  @PostMapping("/token")
  public Token refreshToken(@RequestBody RefreshToken refreshToken) {
    return githubService.refreshToken(refreshToken);
  }
}
