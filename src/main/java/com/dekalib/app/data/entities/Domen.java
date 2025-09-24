package com.dekalib.app.data.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table
@Getter
@Setter
public class Domen {
    @Id
    private String id;

    @Column
    private String domeName;

    @Column
    private boolean isCurrent;

    @PrePersist
    public void prePersist() {
        id = UUID.randomUUID().toString();
    }

}
