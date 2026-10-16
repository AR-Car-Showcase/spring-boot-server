package com.arcarshowcaseserver.model.Cars;

import com.arcarshowcaseserver.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "car_likes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_car_likes_user_car",
                columnNames = {"user_id", "car_id"}
        )
)
public class Like {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "car_id")
    private Car car;

    private LocalDateTime likedAt;
}
