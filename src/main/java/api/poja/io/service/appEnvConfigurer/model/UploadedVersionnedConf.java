package api.poja.io.service.appEnvConfigurer.model;

import api.poja.io.endpoint.rest.model.OneOfPojaConf;
import api.poja.io.model.PojaVersion;

public record UploadedVersionnedConf(
    OneOfPojaConf conf, PojaVersion pojaVersion, String filename) {}
