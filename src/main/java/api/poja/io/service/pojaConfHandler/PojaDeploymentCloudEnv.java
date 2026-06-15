package api.poja.io.service.pojaConfHandler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record PojaDeploymentCloudEnv(@Value("${env}") String env) {}
