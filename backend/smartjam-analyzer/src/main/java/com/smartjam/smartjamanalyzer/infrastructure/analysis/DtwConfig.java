package com.smartjam.smartjamanalyzer.infrastructure.analysis;

import com.smartjam.smartjamanalyzer.domain.port.PerformanceEvaluator;
import com.smartjam.smartjamanalyzer.domain.service.DtwPerformanceEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * Infrastructure configuration responsible for instantiating the DTW mathematical core. It bridges Spring-specific
 * environment settings (Profiles) with the pure Java domain logic.
 */
@Configuration
public class DtwConfig {

    /**
     * Creates the {@link PerformanceEvaluator} bean. Injects the debug flag into the domain service based on whether
     * the "debug" Spring profile is active.
     *
     * @param env The Spring environment used to check for active profiles.
     * @return A configured instance of {@link DtwPerformanceEvaluator}.
     */
    @Bean
    public PerformanceEvaluator performanceEvaluator(Environment env) {
        boolean isDebug = env.acceptsProfiles(Profiles.of("debug"));

        return new DtwPerformanceEvaluator(isDebug);
    }
}
