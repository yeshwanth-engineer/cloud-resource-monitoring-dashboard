package com.yeshwanth.cloudmonitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the whole application in demo mode and exercises the HTTP contract the dashboard relies on.
 */
@SpringBootTest(properties = {
        "app.aws.demo-mode=true",
        "app.demo-auth.username=admin",
        "app.demo-auth.password=admin123",
        "app.jwt.secret=integration-test-secret-that-is-long-enough-1234567890"
})
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("health endpoints stay public")
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("monitoring endpoints reject anonymous callers with a JSON body")
    void monitoringRequiresAToken() throws Exception {
        mockMvc.perform(get("/api/monitoring/instances"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("a forged token is rejected")
    void invalidTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/monitoring/instances").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("wrong credentials produce 401, blank ones produce 400")
    void loginValidatesInput() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("login, list instances and read a CPU series")
    void demoFlowEndToEnd() throws Exception {
        String token = login();

        MvcResult instancesResult = mockMvc.perform(get("/api/monitoring/instances")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode instances = objectMapper.readTree(instancesResult.getResponse().getContentAsString());
        assertThat(instances).hasSize(3);
        String instanceId = instances.get(0).get("instanceId").asText();

        mockMvc.perform(get("/api/monitoring/instances/" + instanceId + "/cpu")
                        .param("hours", "3")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("DEMO"))
                .andExpect(jsonPath("$.metric").value("CPUUtilization"))
                .andExpect(jsonPath("$.points[0].value").isNumber())
                .andExpect(jsonPath("$.points[0].timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("bad query parameters and unknown instance id shapes are reported as 400")
    void validatesRequestParameters() throws Exception {
        String token = login();

        mockMvc.perform(get("/api/monitoring/instances/i-0a1b2c3d4e5f60710/cpu")
                        .param("hours", "0")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("hours")));

        mockMvc.perform(get("/api/monitoring/instances/not-an-instance-id/cpu")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
