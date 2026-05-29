package com.arcarshowcaseserver;

import com.arcarshowcaseserver.repository.CarRepository;
import com.arcarshowcaseserver.repository.CustomizationRepository;
import com.arcarshowcaseserver.repository.LikeRepository;
import com.arcarshowcaseserver.repository.UserRepository;
import com.arcarshowcaseserver.service.CarImportService;
import com.arcarshowcaseserver.service.verification.EmailVerificationSender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:securitytest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "security.auth-mode=INTERNAL",
        "security.jwt.secret=01234567890123456789012345678901",
        "security.jwt.expiration-ms=3600000",
        "security.jwt.refresh-expiration-ms=604800000",
        "security.jwt.issuer=ar-car-showcase-server-test",
        "security.public-endpoints=/api/auth/signup,/api/auth/verify-email,/api/auth/resend-verification",
        "app.signup.verification.smtp-enabled=false",
        "app.signup.verification.store-mode=INMEMORY",
        "app.cors.allowed-origins=http://localhost:8081",
        "spring.ai.openai.api-key=test-key",
        "blender.service.url=http://localhost:5000",
        "ml.service.url=http://localhost:8000",
        "ml.service.key=test-ml-key"
})
@AutoConfigureMockMvc
class SecurityFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private CustomizationRepository customizationRepository;

    @MockBean
    private CarImportService carImportService;

    @MockBean
    private org.springframework.web.client.RestTemplate restTemplate;

    @MockBean
    private EmailVerificationSender emailVerificationSender;

    private Long seededCarId;

    @BeforeEach
    void skipCarSeed() {
        likeRepository.deleteAll();
        customizationRepository.deleteAll();
        userRepository.deleteAll();
        carRepository.deleteAll();

        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("model_url", "generated/customized-model.glb")));

        var car = new com.arcarshowcaseserver.model.Cars.Car();
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
        seededCarId = carRepository.save(car).getId();
    }

    @Test
    void starterAuthFlowAndProtectedEndpointsWork() throws Exception {
        var otpCaptor = org.mockito.ArgumentCaptor.forClass(String.class);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "security_user",
                                  "email": "security_user@example.com",
                                  "password": "Pass@1234"
                                }
                                """))
                .andExpect(status().isOk());

        verify(emailVerificationSender).sendVerificationCode(
                eq("security_user@example.com"),
                eq("security_user"),
                otpCaptor.capture(),
                eq(Duration.ofMinutes(10))
        );

        String verificationCode = otpCaptor.getValue();
        assertThat(verificationCode).isNotBlank();

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "security_user@example.com"
                                }
                                """))
                .andExpect(status().isTooManyRequests());

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "security_user@example.com",
                                  "code": "%s"
                                }
                                """.formatted(verificationCode)))
                .andExpect(status().isOk());

        JsonNode loginResponse = objectMapper.readTree(
                mockMvc.perform(post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "username": "security_user",
                                          "password": "Pass@1234"
                                        }
                                        """))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
        );

        String accessToken = loginResponse.get("accessToken").asText();
        String refreshToken = loginResponse.get("refreshToken").asText();

        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();
        assertThat(loginResponse.get("tokenType").asText()).isEqualTo("Bearer");

        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isUnauthorized());

        String profileBody = mockMvc.perform(get("/api/user/profile")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode profileJson = objectMapper.readTree(profileBody);
        assertThat(profileJson.get("username").asText()).isEqualTo("security_user");
        assertThat(profileJson.get("savedCount").asLong()).isZero();
        assertThat(profileJson.get("customizedCount").asLong()).isZero();

        mockMvc.perform(put("/api/user/profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "favBrands": ["porsche"],
                                  "preferredFuelTypes": ["petrol"],
                                  "preferredTransmissions": ["automatic"],
                                  "drivingCondition": "city",
                                  "maxBudget": 60.0
                                }
                                """))
                .andExpect(status().isOk());

        JsonNode updatedProfile = objectMapper.readTree(
                mockMvc.perform(get("/api/user/profile")
                                .header("Authorization", "Bearer " + accessToken))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
        );

        assertThat(updatedProfile.get("favBrands").toString()).contains("porsche");
        assertThat(updatedProfile.get("preferredFuelTypes").toString()).contains("petrol");
        assertThat(updatedProfile.get("drivingCondition").asText()).isEqualTo("city");

        mockMvc.perform(post("/api/likes/car/" + seededCarId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/likes/check/" + seededCarId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isEqualTo("true"));

        mockMvc.perform(delete("/api/likes/car/" + seededCarId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/likes/check/" + seededCarId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isEqualTo("false"));

        JsonNode customizationResponse = objectMapper.readTree(
                mockMvc.perform(post("/api/customizations")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "vehicleId": "%s",
                                          "materials": {
                                            "seat": "leather",
                                            "body": "matte black"
                                          }
                                        }
                                        """.formatted(seededCarId)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
        );

        assertThat(customizationResponse.get("vehicleId").asText()).isEqualTo(String.valueOf(seededCarId));
        assertThat(customizationResponse.get("modelUrl").asText()).contains("generated/customized-model.glb");

        JsonNode customizations = objectMapper.readTree(
                mockMvc.perform(get("/api/customizations")
                                .header("Authorization", "Bearer " + accessToken))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
        );

        assertThat(customizations.isArray()).isTrue();
        assertThat(customizations.size()).isEqualTo(1);

        mockMvc.perform(post("/api/customizations")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleId": "%s",
                                  "materials": {
                                    "seat": "suede",
                                    "body": "deep red"
                                  }
                                }
                                """.formatted(seededCarId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/customizations")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleId": "%s",
                                  "materials": {
                                    "seat": "alcantara",
                                    "body": "chrome"
                                  }
                                }
                                """.formatted(seededCarId)))
                .andExpect(status().isTooManyRequests());

        JsonNode refreshed = objectMapper.readTree(
                mockMvc.perform(post("/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "refreshToken": "%s"
                                        }
                                        """.formatted(refreshToken)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
        );

        String refreshedAccessToken = refreshed.get("accessToken").asText();
        String refreshedRefreshToken = refreshed.get("refreshToken").asText();

        assertThat(refreshedAccessToken).isNotBlank();
        assertThat(refreshedRefreshToken).isNotBlank();
        assertThat(refreshedRefreshToken).isNotEqualTo(refreshToken);

        mockMvc.perform(post("/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshedRefreshToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshedRefreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disposableEmailIsRejectedBeforeAccountCreation() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "temp_user",
                                  "email": "temp_user@mailinator.com",
                                  "password": "Pass@1234"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
