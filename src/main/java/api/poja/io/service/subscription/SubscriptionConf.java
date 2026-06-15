package api.poja.io.service.subscription;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record SubscriptionConf(@Value("${max.premium.subscribers}") long maxPremiumSubscribersNb) {}
