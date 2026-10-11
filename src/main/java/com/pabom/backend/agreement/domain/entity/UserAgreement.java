package com.pabom.backend.agreement.domain.entity;

import com.pabom.backend.agreement.domain.type.AgreementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "user_agreements")
public class UserAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "agreement_type", nullable = false, length = 30)
    private AgreementType type;

    @Column(nullable = false, length = 30)
    private String version;

    @Column(nullable = false)
    private boolean agreed;

    @Column(name = "agreed_at", nullable = false)
    private Instant agreedAt;

    protected UserAgreement() {
    }

    public UserAgreement(
            Long userId,
            AgreementType type,
            String version,
            boolean agreed,
            Instant agreedAt
    ) {
        this.userId = userId;
        this.type = type;
        this.version = version;
        this.agreed = agreed;
        this.agreedAt = agreedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public AgreementType getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public boolean isAgreed() {
        return agreed;
    }

    public Instant getAgreedAt() {
        return agreedAt;
    }
}
