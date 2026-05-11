package com.arcarshowcaseserver.model.Cars;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "car_colors")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarColor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name = "";
    @Column(columnDefinition = "TEXT")
    private String imageUrl = "";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id")
    @JsonBackReference
    private Car car;
}
