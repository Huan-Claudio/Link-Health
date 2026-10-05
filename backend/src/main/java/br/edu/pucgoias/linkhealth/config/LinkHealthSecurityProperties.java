package br.edu.pucgoias.linkhealth.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurações de segurança mantidas fora do código-fonte.
 */
@ConfigurationProperties(prefix = "link-health.security")
public class LinkHealthSecurityProperties {

    private boolean enabled = true;
    private String apiKey = "";
    private List<String> allowedOrigins = new ArrayList<>();
    private boolean requireHttps;
    private int rateLimitPerMinute = 120;

    public void validate() {
        if (apiKey == null || apiKey.strip().length() < 32) {
            throw new IllegalStateException(
                    "Defina LINK_HEALTH_API_KEY com uma chave aleatória de pelo menos 32 caracteres.");
        }
        if (allowedOrigins.stream().anyMatch("*"::equals)) {
            throw new IllegalStateException("LINK_HEALTH_ALLOWED_ORIGINS não pode usar o curinga *.");
        }
        if (rateLimitPerMinute < 1 || rateLimitPerMinute > 10_000) {
            throw new IllegalStateException("LINK_HEALTH_RATE_LIMIT_PER_MINUTE deve estar entre 1 e 10000.");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins == null
                ? new ArrayList<>()
                : allowedOrigins.stream()
                        .map(String::trim)
                        .filter(origin -> !origin.isEmpty())
                        .toList();
    }

    public boolean isRequireHttps() {
        return requireHttps;
    }

    public void setRequireHttps(boolean requireHttps) {
        this.requireHttps = requireHttps;
    }

    public int getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(int rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }
}
