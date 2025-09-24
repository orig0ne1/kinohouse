package com.dekalib.app.data.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table
@Getter
@Setter
public class Card {
    @Id
    private String id;
    private String bank;
    private String country;
}
