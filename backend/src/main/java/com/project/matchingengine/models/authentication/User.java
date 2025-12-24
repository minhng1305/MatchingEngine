package com.project.matchingengine.models.authentication;

import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "ledger_balance",nullable = false)
    private double ledgerBalance = 1000000.0;


    @Column(name = "available_balance",nullable = false)
    private double availableBalance = 1000000.0;


    @Column(name = "current_esg_points", nullable = false)
    private int currEsgPoints = 0;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public double getLedgerBalance() {
        return ledgerBalance;
    }

    public void setLedgerBalance(double ledgerBalance) {
        this.ledgerBalance = ledgerBalance;
    }

    public double getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(double availableBalance) {
        this.availableBalance = availableBalance;
    }

    public int getCurrEsgPoints() {
        return currEsgPoints;
    }

    public void setCurrEsgPoints(int currEsgPoints) {
        this.currEsgPoints = currEsgPoints;
    }
}