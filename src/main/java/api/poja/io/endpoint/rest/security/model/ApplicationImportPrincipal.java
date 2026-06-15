package api.poja.io.endpoint.rest.security.model;

import static api.poja.io.endpoint.rest.security.model.ApplicationRole.GITHUB_APPLICATION;

import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.userdetails.UserDetails;

@AllArgsConstructor
@ToString
@Getter
public class ApplicationImportPrincipal implements UserDetails {
  private final String repoId;
  private final String bearer;

  @Override
  public Collection<ApplicationRole> getAuthorities() {
    return List.of(GITHUB_APPLICATION);
  }

  @Override
  public String getPassword() {
    return bearer;
  }

  @Override
  public String getUsername() {
    return bearer;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
