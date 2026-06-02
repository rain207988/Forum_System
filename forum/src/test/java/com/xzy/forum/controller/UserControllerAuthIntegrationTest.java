package com.xzy.forum.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class UserControllerAuthIntegrationTest {

    private static final String TEST_PASSWORD = "Pass123456";

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void shouldLoginWithAccessAndRefreshTokens() throws Exception {
        TestUser testUser = registerTestUser();
        JsonNode authData = loginAndGetAuthData(testUser.username(), TEST_PASSWORD);

        assertThat(authData.path("token").asText()).isNotBlank();
        assertThat(authData.path("refreshToken").asText()).isNotBlank();
        assertThat(authData.path("tokenType").asText()).isEqualTo("Bearer");
        assertThat(authData.path("expiresAt").asText()).isNotBlank();
        assertThat(authData.path("refreshExpiresAt").asText()).isNotBlank();
    }

    @Test
    void shouldRotateRefreshTokenAndRejectOldOne() throws Exception {
        TestUser testUser = registerTestUser();
        JsonNode loginData = loginAndGetAuthData(testUser.username(), TEST_PASSWORD);
        String accessToken = loginData.path("token").asText();
        String refreshToken = loginData.path("refreshToken").asText();

        MvcResult refreshResult = mockMvc.perform(post("/user/refreshToken")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("refreshToken", refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("刷新成功"))
                .andReturn();

        JsonNode refreshData = readData(refreshResult);
        String newAccessToken = refreshData.path("token").asText();
        String newRefreshToken = refreshData.path("refreshToken").asText();

        assertThat(newAccessToken).isNotBlank().isNotEqualTo(accessToken);
        assertThat(newRefreshToken).isNotBlank().isNotEqualTo(refreshToken);

        mockMvc.perform(get("/user/info")
                        .header("Authorization", bearerToken(newAccessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value(testUser.username()));

        mockMvc.perform(post("/user/refreshToken")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("refreshToken", refreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void shouldInvalidateRefreshTokenAfterLogout() throws Exception {
        TestUser testUser = registerTestUser();
        JsonNode loginData = loginAndGetAuthData(testUser.username(), TEST_PASSWORD);
        String accessToken = loginData.path("token").asText();
        String refreshToken = loginData.path("refreshToken").asText();

        mockMvc.perform(get("/user/logout")
                        .header("Authorization", bearerToken(accessToken))
                        .param("refreshToken", refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/user/refreshToken")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("refreshToken", refreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void shouldInvalidateRefreshTokenAfterPasswordChange() throws Exception {
        TestUser testUser = registerTestUser();
        JsonNode loginData = loginAndGetAuthData(testUser.username(), TEST_PASSWORD);
        String accessToken = loginData.path("token").asText();
        String refreshToken = loginData.path("refreshToken").asText();

        mockMvc.perform(post("/user/modifyPwd")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("oldPassword", TEST_PASSWORD)
                        .param("newPassword", "Pass987654")
                        .param("passwordRepeat", "Pass987654")
                        .param("refreshToken", refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("密码修改成功，请重新登录"));

        mockMvc.perform(post("/user/refreshToken")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("refreshToken", refreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void shouldReturnUnauthorizedForMalformedAccessToken() throws Exception {
        mockMvc.perform(get("/user/info")
                        .header("Authorization", bearerToken("invalid-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.message").value("登录状态已失效，请重新登录"));
    }

    private JsonNode loginAndGetAuthData(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andReturn();
        return readData(result);
    }

    private JsonNode readData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private String bearerToken(String token) {
        return "Bearer " + token;
    }

    private TestUser registerTestUser() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String username = "user_" + suffix;
        String nickname = "nick_" + suffix;

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", username)
                        .param("nickname", nickname)
                        .param("password", TEST_PASSWORD)
                        .param("passwordRepeat", TEST_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("注册成功"));

        return new TestUser(username, nickname);
    }

    private record TestUser(String username, String nickname) {
    }
}
