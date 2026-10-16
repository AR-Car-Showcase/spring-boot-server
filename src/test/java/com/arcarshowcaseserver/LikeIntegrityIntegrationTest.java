package com.arcarshowcaseserver;

import com.arcarshowcaseserver.dto.CarDTO;
import com.arcarshowcaseserver.exceptions.DuplicateLikeException;
import com.arcarshowcaseserver.model.Cars.Car;
import com.arcarshowcaseserver.model.User;
import com.arcarshowcaseserver.repository.CarRepository;
import com.arcarshowcaseserver.repository.LikeRepository;
import com.arcarshowcaseserver.repository.UserRepository;
import com.arcarshowcaseserver.security.CurrentAuthenticatedUserService;
import com.arcarshowcaseserver.service.LikeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Liking was a check-then-insert with no unique constraint behind it and no
 * transaction around it, which the mobile client could trigger with a double tap:
 * both requests saw "not liked", both inserted, and the duplicate rows then broke
 * unliking permanently because the lookup expected a single row.
 *
 * <p>These tests run the real service against the real schema and force the
 * requests to overlap with a barrier, so they exercise the race rather than
 * describing it.
 */
@SpringBootTest
class LikeIntegrityIntegrationTest {

    private static final int CONCURRENT_REQUESTS = 8;

    @Autowired
    private LikeService likeService;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private CurrentAuthenticatedUserService currentAuthenticatedUserService;

    private ExecutorService pool;
    private Long carId;
    private final List<Long> extraCarIds = new ArrayList<>();

    @BeforeEach
    void seed() {
        likeRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();
        extraCarIds.clear();

        User user = new User();
        user.setUsername("like_user");
        user.setEmail("like_user@example.com");
        user.setPassword("irrelevant-for-this-test");
        user.setEmailVerified(true);
        Long userId = userRepository.save(user).getId();
        when(currentAuthenticatedUserService.requireCurrentUserIdAsLong()).thenReturn(userId);

        carId = saveCar("porsche", "macan");
        pool = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
    }

    @AfterEach
    void shutdownPool() {
        if (pool != null) {
            pool.shutdownNow();
        }
    }

    private Long saveCar(String brand, String model) {
        Car car = new Car();
        car.setBrand(brand);
        car.setModel(model);
        car.setBodyType("suv");
        car.setFuelType("petrol");
        car.setTransmissionType("automatic");
        car.setSeatingCapacity(5);
        car.setPriceRange("50-60");
        car.setMinPriceLakhs(50.0);
        car.setMaxPriceLakhs(60.0);
        car.setRating(4.8);
        car.setModelUrl("/api/models/" + brand + "-" + model + ".glb");
        return carRepository.save(car).getId();
    }

    /**
     * Fires the same action from every thread at once and reports how many succeeded.
     * Anything other than a success or a DuplicateLikeException is rethrown: a race
     * that ends in a 500 is still a bug, even if the row count comes out right.
     */
    private int runConcurrently(Runnable action) throws Exception {
        CyclicBarrier startTogether = new CyclicBarrier(CONCURRENT_REQUESTS);
        AtomicInteger succeeded = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            futures.add(pool.submit(() -> {
                startTogether.await();
                try {
                    action.run();
                    succeeded.incrementAndGet();
                } catch (DuplicateLikeException expected) {
                    // The losing request; this is the contract.
                }
                return null;
            }));
        }
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        return succeeded.get();
    }

    @Test
    void simultaneousLikesStoreExactlyOneRow() throws Exception {
        int succeeded = runConcurrently(() -> likeService.likeCar(carId));

        assertThat(succeeded).isEqualTo(1);
        assertThat(likeRepository.count()).isEqualTo(1);
    }

    @Test
    void simultaneousUnlikesRemoveTheRowExactlyOnce() throws Exception {
        likeService.likeCar(carId);

        int succeeded = runConcurrently(() -> likeService.unlikeCar(carId));

        assertThat(succeeded).isEqualTo(1);
        assertThat(likeRepository.count()).isZero();
    }

    @Test
    void likingStaysUsableAfterLosingTheRace() throws Exception {
        runConcurrently(() -> likeService.likeCar(carId));

        // The old read-then-delete blew up on duplicate rows, leaving the car
        // permanently stuck in the liked state.
        likeService.unlikeCar(carId);
        assertThat(likeRepository.count()).isZero();
        assertThat(likeService.hasUserLikedCar(carId)).isFalse();
    }

    @Test
    void unlikingSomethingNeverLikedIsRejectedNotCrashed() {
        assertThatThrownBy(() -> likeService.unlikeCar(carId))
                .isInstanceOf(DuplicateLikeException.class);
    }

    @Test
    void likedCarsComeBackMostRecentFirst() throws Exception {
        List<Long> likedInOrder = new ArrayList<>();
        likedInOrder.add(carId);
        likeService.likeCar(carId);

        for (String model : List.of("cayenne", "taycan", "panamera", "cayman", "boxster")) {
            Thread.sleep(5); // distinct likedAt values, so the ordering is unambiguous
            Long id = saveCar("porsche", model);
            extraCarIds.add(id);
            likedInOrder.add(id);
            likeService.likeCar(id);
        }

        List<Long> expectedNewestFirst = new ArrayList<>(likedInOrder);
        java.util.Collections.reverse(expectedNewestFirst);

        // Pins the newest-first contract the Saved screen relies on.
        assertThat(likeService.getUserLikedCars())
                .extracting(CarDTO::getId)
                .containsExactlyElementsOf(expectedNewestFirst);
    }
}
