package com.dekalib.app.data.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table
@Getter
@Setter
public class Address {
    @Id
    private String id;
    @Column(nullable = false)
    private String addressName;
    @Column(nullable = false)
    private String promo;

    @PrePersist
    public void prePersist() {
        id = UUID.randomUUID().toString();
    }
}
