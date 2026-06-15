package api.poja.io.endpoint.rest.security.model;

import static api.poja.io.endpoint.rest.security.model.ApplicationRole.GITHUB_APPLICATION;
import static api.poja.io.model.UserStatus.SUSPENDED;

import api.poja.io.model.User;
import api.poja.io.model.UserStatus;
import api.poja.io.repository.model.Application;
import api.poja.io.repository.model.Organization;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@AllArgsConstructor
@ToString
public class ApplicationPrincipal implements UserDetails {
  private final Application application;
  private final Organization applicationOwner;
  private final User applicationOrgOwner;
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

  public String getInstallationId() {
    return application.getInstallationId();
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
    return !applicationOrgOwner.isArchived() || !application.isArchived();
  }

  public UserStatus getOwnerStatus() {
    return applicationOrgOwner.getStatus();
  }

  public boolean isSuspended() {
    return getApplication().isSuspended() || SUSPENDED.equals(getOwnerStatus());
  }
}
