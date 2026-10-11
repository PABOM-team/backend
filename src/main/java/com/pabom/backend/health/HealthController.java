package com.pabom.backend.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health", description = "서버와 필수 의존성의 상태를 확인하는 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HealthController {

    private final HealthEndpoint healthEndpoint;

    @Operation(
            summary = "서버 상태 확인",
            description = "애플리케이션과 데이터베이스 등 필수 의존성이 정상적으로 동작하는지 확인합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "서비스 정상: status=UP",
                    content = @Content(
                            schema = @Schema(implementation = HealthResponse.class),
                            examples = @ExampleObject(value = "{\"status\":\"UP\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "서비스 또는 필수 의존성 비정상: status=DOWN",
                    content = @Content(
                            schema = @Schema(implementation = HealthResponse.class),
                            examples = @ExampleObject(value = "{\"status\":\"DOWN\"}")
                    )
            )
    })
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        HealthComponent health = healthEndpoint.health();
        HttpStatus responseStatus = Status.UP.equals(health.getStatus())
                ? HttpStatus.OK
                : HttpStatus.SERVICE_UNAVAILABLE;

        return ResponseEntity
                .status(responseStatus)
                .body(new HealthResponse(health.getStatus().getCode()));
    }
}
