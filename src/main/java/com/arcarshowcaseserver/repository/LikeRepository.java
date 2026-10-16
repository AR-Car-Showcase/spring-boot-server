package com.arcarshowcaseserver.repository;

import com.arcarshowcaseserver.dto.CarDTO;
import com.arcarshowcaseserver.model.Cars.Car;
import com.arcarshowcaseserver.model.Cars.Like;
import com.arcarshowcaseserver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    /**
     * Deletes the like in a single statement and reports how many rows went, so an
     * unlike never has to read the row first. The read-then-delete it replaces could
     * find the row, lose it to a concurrent unlike, and then fail with a 500 - either
     * an optimistic-locking error or a null entity, depending on the timing. It also
     * clears any duplicate rows an earlier like race already left behind.
     */
    @Modifying
    @Query("DELETE FROM Like l WHERE l.car.id = :carId AND l.user.id = :userId")
    int deleteByCarIdAndUserId(@Param("carId") Long carId, @Param("userId") Long userId);


    @Query("""
        SELECT new com.arcarshowcaseserver.dto.CarDTO(
            c.id, 
            c.brand, 
            c.model, 
            c.bodyType, 
            c.fuelType, 
            c.priceRange, 
            c.rating,
            (SELECT ci.imageUrl FROM CarImage ci WHERE ci.car.id = c.id ORDER BY CASE WHEN LOWER(ci.type) = 'main' THEN 1 WHEN LOWER(ci.type) = 'exterior' THEN 2 WHEN LOWER(ci.type) = 'primary' THEN 3 ELSE 4 END ASC, ci.id ASC LIMIT 1)
        )
        FROM Like l
        JOIN l.car c
        WHERE l.user.id = :userId
        ORDER BY l.likedAt DESC
        """)
    // Declared as a List because the result is ordered. A Set-returning query happens
    // to arrive as a LinkedHashSet today, so the order survived, but the type said
    // "unordered" while the query promised otherwise.
    List<CarDTO> findLikedCarsByUserId(@Param("userId") Long userId);

    boolean existsByCarAndUser(Car car, User user);

    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM Like l WHERE l.car.id = :carId AND l.user.id = :userId")
    boolean existsByCarIdAndUserId(@Param("carId") Long carId, @Param("userId") Long userId);

    long countByUser(User user);
}
