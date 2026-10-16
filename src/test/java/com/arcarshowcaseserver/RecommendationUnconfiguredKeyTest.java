package com.arcarshowcaseserver;

import com.arcarshowcaseserver.model.Cars.Car;
import com.arcarshowcaseserver.model.User;
import com.arcarshowcaseserver.repository.CarRepository;
import com.arcarshowcaseserver.repository.RecommendationHistoryRepository;
import com.arcarshowcaseserver.repository.UserRepository;
import com.arcarshowcaseserver.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * The shared secret used to have a committed default, so the API always had
 * "a" key. Now that the default is gone, a deployment that forgets to set
 * ML_SERVICE_KEY would otherwise send an unauthenticated request on every
 * recommendation view and collect a 403 for it.
 */
@SpringBootTest(properties = {"ml.service.key="})
class RecommendationUnconfiguredKeyTest {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecommendationHistoryRepository historyRepository;

    @MockBean(name = "mlServiceRestTemplate")
    private RestTemplate mlServiceRestTemplate;

    private Long carId;
    private Long userId;

    @BeforeEach
    void seed() {
        historyRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();

        // Seeded for real, so the only thing stopping the call is the missing key.
        Car car = new Car();
        car.setBrand("porsche");
        car.setModel("macan");
        car.setBodyType("suv");
        car.setFuelType("petrol");
        car.setTransmissionType("automatic");
        car.setSeatingCapacity(5);
        car.setPriceRange("50-60");
        car.setMinPriceLakhs(50.0);
        car.setMaxPriceLakhs(60.0);
        car.setRating(4.8);
        car.setModelUrl("/api/models/porsche-macan.glb");
        carId = carRepository.save(car).getId();

        User user = new User();
        user.setUsername("nokey_user");
        user.setEmail("nokey_user@example.com");
        user.setPassword("irrelevant-for-this-test");
        user.setEmailVerified(true);
        user.setFavBrands(new HashSet<>(Set.of("porsche")));
        user.setMaxBudget(60.0);
        userId = userRepository.save(user).getId();
    }

    @Test
    void similarCarsAreSkippedRatherThanRequestedWithoutAKey() {
        assertThat(recommendationService.getRecommendedCars(carId)).isEmpty();
        verifyNoInteractions(mlServiceRestTemplate);
    }

    @Test
    void personalizedRecommendationsAreSkippedRatherThanRequestedWithoutAKey() {
        assertThat(recommendationService.getPersonalizedRecommendations(userId)).isEmpty();
        verifyNoInteractions(mlServiceRestTemplate);
    }

    @Test
    void feedbackIsStillRecordedLocallyWithoutAKey() {
        recommendationService.recordInteraction(userId, carId, "like");

        // The interaction is ours to keep; only the relay needs the secret.
        assertThat(historyRepository.findAll()).hasSize(1);
        verifyNoInteractions(mlServiceRestTemplate);
    }
}
