package com.pabom.backend.species.domain.entity;

import com.pabom.backend.species.domain.type.ReptileCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "species")
public class Species {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String code;

    @Column(name = "common_name", nullable = false, length = 100)
    private String commonName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReptileCategory category;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected Species() {
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getCommonName() {
        return commonName;
    }

    public ReptileCategory getCategory() {
        return category;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}
