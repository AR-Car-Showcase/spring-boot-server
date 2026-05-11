package com.arcarshowcaseserver.repository;

import com.arcarshowcaseserver.model.Cars.CarVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarVariantRepository extends JpaRepository<CarVariant,Long> {




    @Query("""
    SELECT v
    FROM CarVariant v
    WHERE v.car.id = :carId
      AND LOWER(v.fuel) = LOWER(:fuel)
""")
    List<CarVariant> findByCarIdAndFuelIgnoreCase(
            @Param("carId") Long carId,
            @Param("fuel") String fuel
    );


    @Query("""
       SELECT v
       FROM CarVariant v
       WHERE v.car.id = :carId
       AND v.variant =:variant
       """)
    Optional<CarVariant> findByCarIdAndVariant(
            @Param("carId") Long carId,
            @Param("variant") String variant
    );

@Query("SELECT v FROM CarVariant v WHERE v.car.id = :carId")
List<CarVariant> findVariantsByCarId(@Param("carId") Long carId);


        //  By engineCC (1984 cc / 17.4 kWh)
        @Query("""
        SELECT v FROM CarVariant v 
        WHERE LOWER(v.engineCc) LIKE LOWER(CONCAT('%', :engine, '%'))
    """)
        List<CarVariant> findByEngineCc(@Param("engine") String engineCC);



        @Query("""
        SELECT v FROM CarVariant v
        WHERE CAST(
            REGEXP_REPLACE(v.price, '[^0-9.]', '', 'g')
            AS double
        ) <= :maxPrice
    """)
        List<CarVariant> findUnderPrice(@Param("maxPrice") double maxPrice);




    @Query(
            value = """
    SELECT *
    FROM car_variants
    WHERE CAST(
        REGEXP_REPLACE(price, '[^0-9.]', '', 'g')
        AS DOUBLE PRECISION
    ) BETWEEN :min AND :max
    ORDER BY CAST(
        REGEXP_REPLACE(price, '[^0-9.]', '', 'g')
        AS DOUBLE PRECISION
    ) ASC
    """,
            nativeQuery = true
    )
    List<CarVariant> findBetweenPrices(
            @Param("min") double min,
            @Param("max") double max
    );




    @Query(
            value = """
        SELECT *
        FROM car_variants v
        WHERE (:fuel IS NULL OR LOWER(v.fuel) = LOWER(:fuel))
          AND (:engine IS NULL OR LOWER(v.engine_cc) LIKE LOWER(CONCAT('%', :engine, '%')))
          AND (:maxPrice IS NULL OR
               CAST(REGEXP_REPLACE(v.price, '[^0-9.]', '', 'g') AS DOUBLE PRECISION)
               <= :maxPrice)
        """,
            nativeQuery = true
    )
    List<CarVariant> filterVariants(
            @Param("fuel") String fuel,
            @Param("engine") String engine,
            @Param("maxPrice") Double maxPrice
    );


    @Query("""
        SELECT v
        FROM CarVariant v
        WHERE v.car.id IN :carIds
          AND LOWER(v.fuel) = LOWER(:fuel)
    """)
    List<CarVariant> findByCarIdsAndFuel(
            @Param("carIds") List<Long> carIds,
            @Param("fuel") String fuel
    );


    @Query("""
       SELECT v
       FROM CarVariant v
       WHERE v.variant =:variant
       """)
    CarVariant findByVariant(@Param("variant") String variant);
}
