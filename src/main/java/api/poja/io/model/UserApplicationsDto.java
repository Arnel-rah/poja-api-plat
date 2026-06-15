package api.poja.io.model;

import api.poja.io.model.page.Page;
import api.poja.io.repository.model.Application;

public record UserApplicationsDto(Page<Application> apps, String userId, long userAppsNb) {}
