package com.pabom.backend.notification.infrastructure.expo;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExpoPushConfig {

    /** 기존 공용 RestTemplate 빈과 섞이지 않도록 Expo 전용 RestTemplate은 빈으로 등록하지 않는다. */
    @Bean
    public ExpoPushClient expoPushClient(RestTemplateBuilder builder, ExpoPushProperties properties) {
        return new ExpoPushClient(
                builder.connectTimeout(properties.connectTimeout())
                        .readTimeout(properties.readTimeout())
                        .build(),
                properties,
                duration -> Thread.sleep(duration.toMillis())
        );
    }
}
