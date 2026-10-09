package com.pabom.backend.agreement.presentation;

import com.pabom.backend.agreement.application.command.CompleteAgreementsCommand;
import com.pabom.backend.agreement.application.result.AgreementCompletionResult;
import com.pabom.backend.agreement.application.service.AgreementCompletionService;
import com.pabom.backend.agreement.presentation.docs.AgreementControllerDocs;
import com.pabom.backend.agreement.presentation.request.AgreementCompletionRequest;
import com.pabom.backend.agreement.presentation.response.AgreementCompletionResponse;
import com.pabom.backend.agreement.presentation.response.AgreementCompletionResponse.UserInfo;
import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.global.error.BusinessException;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/agreements")
public class AgreementController implements AgreementControllerDocs {

    private static final String BEARER_PREFIX = "bearer ";

    private final AgreementCompletionService service;

    public AgreementController(AgreementCompletionService service) {
        this.service = service;
    }

    @PostMapping
    @Override
    public ResponseEntity<AgreementCompletionResponse> complete(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization,
            @Valid @RequestBody AgreementCompletionRequest request
    ) {
        AgreementCompletionResult result = service.complete(
                extractBearerToken(authorization),
                new CompleteAgreementsCommand(request.agreements().stream()
                        .map(item -> new CompleteAgreementsCommand.Agreement(
                                item.type(),
                                item.version(),
                                item.agreed()
                        ))
                        .toList())
        );
        return ResponseEntity.ok(new AgreementCompletionResponse(
                new UserInfo(result.userId(), result.status()),
                result.accessToken(),
                result.expiresIn()
        ));
    }

    private String extractBearerToken(String authorization) {
        if (!StringUtils.hasText(authorization)
                || !authorization.toLowerCase(Locale.ROOT).startsWith(BEARER_PREFIX)) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
        }
        return token;
    }
}
