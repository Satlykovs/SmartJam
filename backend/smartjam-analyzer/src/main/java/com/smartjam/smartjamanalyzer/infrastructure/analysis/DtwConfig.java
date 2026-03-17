package com.smartjam.smartjamanalyzer.infrastructure.analysis;

import com.smartjam.smartjamanalyzer.domain.port.PerformanceEvaluator;
import com.smartjam.smartjamanalyzer.domain.service.DtwPerformanceEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Configuration
public class DtwConfig {

    @Bean
    public PerformanceEvaluator performanceEvaluator(Environment env) {
        boolean isDebug = env.acceptsProfiles(Profiles.of("debug"));

        return new DtwPerformanceEvaluator(isDebug);
    }
}
