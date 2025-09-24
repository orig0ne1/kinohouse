package com.dekalib.app.data.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table
@Getter
@Setter
public class Addon {
    @Id
    private long id;
    private String promo;
    private String addonId;
    private String addonName;
    private int price = 0;  // Default price
}