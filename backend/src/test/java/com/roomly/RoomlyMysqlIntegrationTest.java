package com.roomly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Map;
import java.util.UUID;

/** Opt-in destructive schema setup, restricted to a dedicated roomly_test/roomly_ci schema. */
@EnabledIfEnvironmentVariable(named = "ROOMLY_TEST_MYSQL_URL", matches = ".+")
@SpringBootTest(
        properties = {
            "roomly.jwt-secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
            "roomly.media-directory=${java.io.tmpdir}/roomly-mysql-test",
            "roomly.mail-enabled=false",
            "roomly.push-enabled=false"
        })
@AutoConfigureMockMvc
class RoomlyMysqlIntegrationTest {
    @Autowired private MockMvc http;
    @Autowired private JdbcTemplate database;
    @Autowired private ObjectMapper json;
    @MockBean private TaskScheduler taskScheduler;

    @DynamicPropertySource
    static void mysql(DynamicPropertyRegistry properties) throws Exception {
        String url = System.getenv("ROOMLY_TEST_MYSQL_URL");
        if (!url.matches("jdbc:mysql://[^/]+/roomly_(?:test|ci)(?:_[a-zA-Z0-9]+)*(?:\\?.*)?")) {
            throw new IllegalArgumentException(
                    "Use a dedicated roomly_test or roomly_ci MySQL schema.");
        }
        String user = System.getenv().getOrDefault("ROOMLY_TEST_MYSQL_USER", "root");
        String password = System.getenv().getOrDefault("ROOMLY_TEST_MYSQL_PASSWORD", "");
        String setupUrl = url + (url.contains("?") ? "&" : "?") + "allowMultiQueries=true";
        try (var connection = DriverManager.getConnection(setupUrl, user, password);
                var statement = connection.createStatement()) {
            statement.execute(Files.readString(Path.of("../Infra/roomly/init/001_schema.sql")));
        }
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.username", () -> user);
        properties.add("spring.datasource.password", () -> password);
    }

    @Test
    void importsAllRoomlyTables() {
        assertEquals(
                14,
                database.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()",
                        Integer.class));
    }

    @Test
    void servesAnEmptyCatalogFromTheActualSchema() throws Exception {
        http.perform(get("/api/v1/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.total_elements").value(0));
    }

    @Test
    void refusesEmailCodesWhenMailIsDisabled() throws Exception {
        http.perform(
                        post("/api/v1/auth/verification-code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"mail-test@example.invalid\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.statusCode").value(503))
                .andExpect(jsonPath("$.data.code").value("REQUEST_FAILED"))
                .andExpect(jsonPath("$.data.verification_code").doesNotExist());
        assertEquals(
                0,
                database.queryForObject(
                        "SELECT COUNT(*) FROM auth_tokens WHERE purpose IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET')",
                        Integer.class));
    }

    @Test
    void registersLogsInUpdatesProfileAndEnablesHost() throws Exception {
        String email = "roomly-" + UUID.randomUUID() + "@example.invalid";
        String password = "Roomly-test-password-123!";
        http.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json.writeValueAsString(
                                                Map.of(
                                                        "email",
                                                        email,
                                                        "password",
                                                        password,
                                                        "full_name",
                                                        "ROOMLY test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email));
        database.update("UPDATE users SET email_verified = 1 WHERE email = ?", email);
        String response =
                http.perform(
                                post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                json.writeValueAsString(
                                                        Map.of(
                                                                "email",
                                                                email,
                                                                "password",
                                                                password,
                                                                "installation_id",
                                                                UUID.randomUUID().toString(),
                                                                "device_name",
                                                                "CI"))))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String token = json.readTree(response).path("data").path("access_token").asText();
        assertFalse(token.isBlank());
        http.perform(
                        put("/api/v1/profile")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"full_name\":\"Updated ROOMLY\",\"phone\":\"0900000000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.full_name").value("Updated ROOMLY"));
        http.perform(post("/api/v1/auth/enable-host").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertEquals(
                1,
                database.queryForObject(
                        "SELECT is_host FROM users WHERE email = ?", Integer.class, email));
    }
}
