package org.haughey.mqtt;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * Values to be injected by Spring Boot at the start of the application.
 *
 * @author dhaugh
 */
@Configuration
@ComponentScan(basePackages = {"org.haughey.mqtt"})
//@PropertySource("classpath:application.properties")
// TODO: Refactor code to use application.properties file
public class ConfigDefaultVariables {

    @Bean
    public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() throws Exception {
        placeholderConfigurer().setIgnoreUnresolvablePlaceholders(true);
        placeholderConfigurer().setIgnoreResourceNotFound(true);
        return new PropertySourcesPlaceholderConfigurer();
    }
}

