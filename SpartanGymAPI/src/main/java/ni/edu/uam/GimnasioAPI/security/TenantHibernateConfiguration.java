package ni.edu.uam.GimnasioAPI.security;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TenantHibernateConfiguration {

    @Bean
    HibernatePropertiesCustomizer tenantIdentifierResolverCustomizer(
            TenantIdentifierResolver tenantIdentifierResolver) {
        return properties -> properties.put(
                AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER,
                tenantIdentifierResolver
        );
    }
}
