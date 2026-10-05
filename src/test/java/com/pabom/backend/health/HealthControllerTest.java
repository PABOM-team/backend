package com.pabom.backend.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private HealthEndpoint healthEndpoint;

    private HealthController healthController;

    @BeforeEach
    void setUp() {
        healthController = new HealthController(healthEndpoint);
    }

    @Test
    void returnsOkWhenHealthIsUp() {
        given(healthEndpoint.health()).willReturn(Health.up().build());

        ResponseEntity<HealthResponse> response = healthController.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new HealthResponse("UP"));
    }

    @Test
    void returnsServiceUnavailableWhenHealthIsDown() {
        given(healthEndpoint.health()).willReturn(Health.down().build());

        ResponseEntity<HealthResponse> response = healthController.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isEqualTo(new HealthResponse("DOWN"));
    }
}
