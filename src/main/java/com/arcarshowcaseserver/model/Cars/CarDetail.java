package com.arcarshowcaseserver.model.Cars;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "car_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spec_key", columnDefinition = "TEXT")
    private String key = "";

    @Column(name = "spec_value", columnDefinition = "TEXT")
    private String value = "";

    @Column(name = "category", columnDefinition = "TEXT")
    private String category = "";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id")
    @JsonBackReference
    private Car car;

    public CarDetail(String category, String key, String value, Car car) {
        this.category = category;
        this.key = key;
        this.value = value;
        this.car = car;
    }
}
