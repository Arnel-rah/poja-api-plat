package api.poja.io.endpoint.rest.security.model;

import api.poja.io.model.User;
import api.poja.io.model.UserStatus;
import java.util.Arrays;
import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@AllArgsConstructor
@ToString
public class Principal implements UserDetails {
  private final User user;

  private final String bearer;

  @Override
  public Collection<UserRole> getAuthorities() {
    return Arrays.stream(user.getRoles())
        .map(role -> UserRole.valueOf(String.valueOf(role)))
        .toList();
  }

  @Override
  public String getPassword() {
    return bearer;
  }

  @Override
  public String getUsername() {
    return user.getUsername();
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
    return !user.isArchived();
  }

  public boolean isBetaTester() {
    return user.isBetaTester();
  }

  public UserStatus getStatus() {
    return user.getStatus();
  }

  public String getStatusReason() {
    return user.getStatusReason();
  }
}
