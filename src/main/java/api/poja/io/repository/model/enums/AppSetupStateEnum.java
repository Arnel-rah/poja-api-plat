package api.poja.io.repository.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AppSetupStateEnum {
  ENV_CREATION_IN_PROGRESS,
  ENV_CREATION_FAILED,
  ENV_CREATION_SUCCESS,
  REPO_CREATION_IN_PROGRESS,
  REPO_CREATION_FAILED,
  REPO_CREATION_SUCCESS,
  ENV_DEPLOYMENT_INITIATION_FAILED,
  ENV_DEPLOYMENT_INITIATION_IN_PROGRESS,
  ENV_DEPLOYMENT_INITIATED;

  public boolean isFinal() {
    return switch (this) {
      case REPO_CREATION_FAILED,
              ENV_CREATION_FAILED,
              ENV_DEPLOYMENT_INITIATION_FAILED,
              ENV_DEPLOYMENT_INITIATED ->
          true;
      default -> false;
    };
  }
}
