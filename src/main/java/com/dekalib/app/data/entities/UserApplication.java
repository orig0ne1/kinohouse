package com.dekalib.app.data.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table()
@Getter
@Setter
public class UserApplication {
    @Id
    private Long userId;
    private String username;
    private String sourceInfo;
    private String experience;
    private int profitsCount = 0;
    private Long lastProfitDate;
    private Long registrationDate;
    private double profitsSum = 0.0;
    private String role;
    private String promo;

    @PrePersist
    public void prePersist() {
        promo = UUID.randomUUID().toString();
    }
}