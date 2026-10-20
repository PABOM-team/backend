package com.pabom.backend.record.domain.entity;

import com.pabom.backend.record.domain.type.CareItemInputType;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "care_items")
public class CareItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 50) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Enumerated(EnumType.STRING) @Column(name = "input_type", nullable = false, length = 20)
    private CareItemInputType inputType;
    @Column(length = 20) private String unit;
    @Column(name = "min_value", precision = 19, scale = 4) private BigDecimal minValue;
    @Column(name = "max_value", precision = 19, scale = 4) private BigDecimal maxValue;
    @Column(name = "outcome_allowed", nullable = false) private boolean outcomeAllowed;
    @Column(nullable = false) private boolean active;

    protected CareItem() {}
    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public CareItemInputType getInputType() { return inputType; }
    public String getUnit() { return unit; }
    public BigDecimal getMinValue() { return minValue; }
    public BigDecimal getMaxValue() { return maxValue; }
    public boolean isOutcomeAllowed() { return outcomeAllowed; }
    public boolean isActive() { return active; }
}
