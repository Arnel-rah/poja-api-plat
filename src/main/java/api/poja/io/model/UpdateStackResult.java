package api.poja.io.model;

import static api.poja.io.model.UpdateStackResult.UpdateStatus.NO_UPDATE_NEEDED;
import static api.poja.io.model.UpdateStackResult.UpdateStatus.UPDATE_SUCCESS;

public record UpdateStackResult(UpdateStatus status, String stackId) {
  public boolean isUpdated() {
    return UPDATE_SUCCESS.equals(status);
  }

  public boolean isSuccess() {
    return isUpdated() || NO_UPDATE_NEEDED.equals(status);
  }

  public enum UpdateStatus {
    UPDATE_SUCCESS,
    UPDATE_FAILED,
    NO_UPDATE_NEEDED;
  }
}
