package api.poja.io.model;

import api.poja.io.repository.model.Application;
import api.poja.io.repository.model.Environment;

public record CreatedApplicationDTO(Application application, Environment environment) {}
