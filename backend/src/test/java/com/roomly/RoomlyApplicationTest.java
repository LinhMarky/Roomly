package com.roomly;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:roomly;MODE=MySQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "ROOMLY_JWT_SECRET=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
            "roomly.media-directory=${java.io.tmpdir}/roomly-application-test",
            "roomly.mail-enabled=false",
            "roomly.push-enabled=false"
        })
@AutoConfigureMockMvc
class RoomlyApplicationTest {
    @Autowired private MockMvc http;

    // Background jobs are isolated from the HTTP/startup checks.
    @MockBean private TaskScheduler taskScheduler;

    @Test
    void startsWithoutASeparateProfile() throws Exception {
        http.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void servesTheRoomlyAmenitiesResource() throws Exception {
        http.perform(get("/api/v1/amenities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").exists());
    }

    @Test
    void protectsAccountEndpoints() throws Exception {
        http.perform(get("/api/v1/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(jsonPath("$.data.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsAnInvalidAccessToken() throws Exception {
        http.perform(get("/api/v1/profile").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.code").value("UNAUTHORIZED"));
    }

    @Test
    void exposesTheRoomlyApiDefinition() throws Exception {
        http.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("ROOMLY API"))
                .andExpect(jsonPath("$.paths['/api/v1/profile']").exists());
    }
}
