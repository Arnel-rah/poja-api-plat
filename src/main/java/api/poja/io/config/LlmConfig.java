// src/main/java/api/poja/io/config/LlmConfig.java

package api.poja.io.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@Data
public class LlmConfig {

    @Value("${llm.provider:openrouter}")
    private String provider;

    @Value("${llm.api.key:}")
    private String apiKey;

    @Value("${llm.model:openrouter/free}")
    private String model;

    @Value("${llm.url:https://openrouter.ai/api/v1/chat/completions}")
    private String url;

    @Value("${llm.timeout:30000}")
    private int timeout;

}