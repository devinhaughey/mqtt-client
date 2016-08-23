package org.haughey.mqtt;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * ConfigDefaultVariables class to inject default variables for Spring Boot
 *
 * @author dhaugh
 */
@org.springframework.context.annotation.Configuration
@ComponentScan(basePackages = {"org.haughey.mqtt.*"})
public class ConfigDefaultVariables {

    /**
     * placeholderConfigurer method creates a new property sources
     * configurer for setting the default values for the variables.
     *
     * @return PropertySourcesPlaceholderConfigurer for setting variables
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        placeholderConfigurer().setIgnoreUnresolvablePlaceholders( true );
        placeholderConfigurer().setIgnoreResourceNotFound(true);
        return new PropertySourcesPlaceholderConfigurer();
    }
}

