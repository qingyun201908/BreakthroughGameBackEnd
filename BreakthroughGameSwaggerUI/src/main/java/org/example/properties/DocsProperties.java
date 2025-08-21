package org.example.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "docs")
public class DocsProperties {
    private List<Service> services;

    public List<Service> getServices() { return services; }
    public void setServices(List<Service> services) { this.services = services; }

    public static class Service {
        private String id;
        private String name;
        private URI baseUrl;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public URI getBaseUrl() { return baseUrl; }
        public void setBaseUrl(URI baseUrl) { this.baseUrl = baseUrl; }
    }
}
