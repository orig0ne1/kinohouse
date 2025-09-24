package com.dekalib.app.data.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table
@Getter
@Setter
public class YooKassa {
    @Id
    private String shopId;
    private String secretKey;
    private String idempotenceKey;
}
