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
public class Tarif {
    @Id
    private String promo;
    private int standart = 2900;
    private int plus = 3500;
    private int vip = 4900;
}