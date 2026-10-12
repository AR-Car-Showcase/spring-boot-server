package com.arcarshowcaseserver;

import com.arcarshowcaseserver.model.Cars.Car;
import com.arcarshowcaseserver.model.User;
import com.arcarshowcaseserver.repository.CarRepository;
import com.arcarshowcaseserver.repository.RecommendationHistoryRepository;
import com.arcarshowcaseserver.repository.UserRepository;
import com.arcarshowcaseserver.service.RecommendationService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The recommendation service is a non-critical enhancement running in its own
 * process, but {@code RecommendationService} used to call it through a bare
 * {@code new RestTemplate()}, which has no connect or read timeout whatsoever.
 * A wedged ML service therefore pinned a Tomcat worker thread forever, and enough
 * of them would take the entire API down with a dependency nothing depends on.
 *
 * <p>Every test here points the service at a server that accepts the TCP connection
 * and then never replies - the failure mode that hangs rather than erroring - and
 * bounds how long the API is allowed to take. Without a read timeout these tests
 * do not fail slowly, they never finish, so the bound is enforced preemptively.
 */
@SpringBootTest(properties = {
        "security.public-endpoints=/api/auth/signup,/api/cars/recommendations/**"
})
@AutoConfigureMockMvc
class RecommendationResilienceIntegrationTest {

    /**
     * Generous enough to never flake, far below both the 180s Blender budget and
     * the unbounded wait this test exists to catch.
     */
    private static final Duration API_MUST_ANSWER_WITHIN = Duration.ofSeconds(45);

    private static HttpServer blackHole;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private RecommendationHistoryRepository historyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    @Qualifier("mlServiceRestTemplate")
    private RestTemplate mlServiceRestTemplate;

    @Autowired
    private RestTemplate primaryRestTemplate;

    private Long carId;
    private Long userId;

    @BeforeAll
    static void startBlackHoleServer() throws IOException {
        blackHole = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        blackHole.createContext("/", exchange -> {
            // Accept the request, then stall well past any sane client budget.
            try {
                Thread.sleep(Duration.ofMinutes(5).toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write(new byte[0]);
            }
        });
        blackHole.setExecutor(Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "ml-black-hole");
            thread.setDaemon(true);
            return thread;
        }));
        blackHole.start();
    }

    @AfterAll
    static void stopBlackHoleServer() {
        if (blackHole != null) {
            blackHole.stop(0);
        }
    }

    @DynamicPropertySource
    static void pointMlServiceAtBlackHole(DynamicPropertyRegistry registry) {
        registry.add("ml.service.url", () -> "http://127.0.0.1:" + blackHole.getAddress().getPort());
    }

    @BeforeEach
    void seedCarAndUser() {
        historyRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();

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

        // A real user with real preferences, otherwise getPersonalizedRecommendations
        // short-circuits on the missing user and never calls the ML service at all -
        // the test would then pass even with an unbounded HTTP client.
        User user = new User();
        user.setUsername("rec_user");
        user.setEmail("rec_user@example.com");
        user.setPassword("irrelevant-for-this-test");
        user.setEmailVerified(true);
        user.setFavBrands(new HashSet<>(Set.of("porsche")));
        user.setPreferredBodyTypes(new HashSet<>(Set.of("suv")));
        user.setPreferredFuelTypes(new HashSet<>(Set.of("petrol")));
        user.setPreferredTransmissions(new HashSet<>(Set.of("automatic")));
        user.setDrivingCondition("city");
        user.setMaxBudget(60.0);
        userId = userRepository.save(user).getId();
    }

    @Test
    void mlServiceClientIsSeparateFromTheLongRunningBlenderClient() {
        // Two downstream services, two timeout budgets. Sharing one template would
        // make a recommendation lookup wait out Blender's multi-minute budget.
        assertThat(mlServiceRestTemplate).isNotSameAs(primaryRestTemplate);
    }

    @Test
    void similarCarLookupGivesUpOnAHangingMlService() {
        assertTimeoutPreemptively(API_MUST_ANSWER_WITHIN, () ->
                mockMvc.perform(get("/api/cars/recommendations/" + carId))
                        .andExpect(status().isOk())
                        .andExpect(content().json("[]")));
    }

    @Test
    void personalizedRecommendationsGiveUpOnAHangingMlService() {
        assertTimeoutPreemptively(API_MUST_ANSWER_WITHIN, () ->
                assertThat(recommendationService.getPersonalizedRecommendations(userId)).isEmpty());
    }

    @Test
    void feedbackIsStoredEvenWhenTheMlRelayHangs() {
        assertTimeoutPreemptively(API_MUST_ANSWER_WITHIN, () ->
                recommendationService.recordInteraction(7L, carId, "like"));

        // The interaction must survive locally; only the online-learning signal is lost.
        assertThat(historyRepository.findAll())
                .singleElement()
                .satisfies(history -> {
                    assertThat(history.getUserId()).isEqualTo(7L);
                    assertThat(history.getCarId()).isEqualTo(carId);
                    assertThat(history.getAction()).isEqualTo("like");
                });
    }
}
