package com.project.matchingengine.models.authentication;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class PortfolioId implements Serializable {
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "symbol", nullable = false)
    private String symbol;

    public PortfolioId(UUID userId, String symbol) {
        this.userId = userId;
        this.symbol = symbol;
    }
}
