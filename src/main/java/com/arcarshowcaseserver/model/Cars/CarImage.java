package com.arcarshowcaseserver.model.Cars;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "car_images")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type = "";
    @Column(columnDefinition = "TEXT")
    private String imageUrl = "";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id")
    @JsonBackReference
    private Car car;

    public CarImage(String type, String imageUrl, Car car) {
        this.type = type;
        this.imageUrl = imageUrl;
        this.car = car;
    }
}
