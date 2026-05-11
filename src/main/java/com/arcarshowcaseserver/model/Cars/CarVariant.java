package com.arcarshowcaseserver.model.Cars;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "car_variants",
        indexes = {
                @Index(name = "idx_variant", columnList = "variant")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String variant = "";
    @Column(columnDefinition = "TEXT")
    private String price = "";
    @Column(columnDefinition = "TEXT")
    private String engineCc = "";
    @Column(columnDefinition = "TEXT")
    private String fuel = "";
    @Column(columnDefinition = "TEXT")
    private String transmission = "";
    @Column(columnDefinition = "TEXT")
    private String mileage = "";

    @ElementCollection
    @CollectionTable(
            name = "variant_key_specifications",
            joinColumns = @JoinColumn(name = "variant_id")
    )
    @Column(name = "specification", columnDefinition = "TEXT")
    private List<String> keySpecifications = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id")
    @JsonBackReference
    private Car car;
}
