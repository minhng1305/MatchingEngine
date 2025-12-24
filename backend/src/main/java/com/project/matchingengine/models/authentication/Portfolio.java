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
    @Id
    private UUID userId;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private int quantity;

    public Portfolio(UUID userId, String symbol, int quantity) {
        this.userId = userId;
        this.symbol = symbol;
        this.quantity = quantity;
    }
}
