package com.project.matchingengine.models.authentication;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Entity
@Table(name = "portfolios")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Portfolio {
    @EmbeddedId
    private PortfolioId id;

    @Column(nullable = false)
    private int quantity;

    // Convenience getters for userId and symbol
    public UUID getUserId() {
        return id != null ? id.getUserId() : null;
    }

    public String getSymbol() {
        return id != null ? id.getSymbol() : null;
    }

    // Convenience setters
    public void setUserId(UUID userId) {
        if (this.id == null) {
            this.id = new PortfolioId();
        }
        this.id.setUserId(userId);
    }

    public void setSymbol(String symbol) {
        if (this.id == null) {
            this.id = new PortfolioId();
        }
        this.id.setSymbol(symbol);
    }

    public Portfolio(UUID userId, String symbol, int quantity) {
        this.id = new PortfolioId(userId, symbol);
        this.quantity = quantity;
    }

    public Portfolio(PortfolioId id, int quantity) {
        this.id = id;
        this.quantity = quantity;
    }
}
