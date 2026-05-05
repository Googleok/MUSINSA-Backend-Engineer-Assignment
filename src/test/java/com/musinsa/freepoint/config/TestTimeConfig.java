package com.musinsa.freepoint.config;

import com.musinsa.freepoint.common.time.TestTimeProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestTimeConfig {

    @Bean
    @Primary
    public TestTimeProvider testTimeProvider() {
        return new TestTimeProvider();
    }
}
