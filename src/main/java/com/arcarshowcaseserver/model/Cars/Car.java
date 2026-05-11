package com.arcarshowcaseserver.model.Cars;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cars")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String brand = "";
    private String model = "";
    private String bodyType = "";
    private String fuelType = "";
    private String transmissionType = "";
    private int seatingCapacity = 0;
    private String priceRange = "";
    private double minPriceLakhs = 0.0;
    private double maxPriceLakhs = 0.0;
    private double rating = 0.0;
    private String modelUrl = "";
    @JsonManagedReference
    @OneToMany(mappedBy = "car", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CarDetail> details = new ArrayList<>();

    @JsonManagedReference
    @OneToMany(mappedBy = "car", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CarVariant> variants = new ArrayList<>();

    @JsonManagedReference
    @OneToMany(mappedBy = "car", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CarImage> images = new ArrayList<>();

    @JsonManagedReference
    @OneToMany(mappedBy = "car", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CarColor> colors = new ArrayList<>();
}
