package io.zhc1.realworld.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import io.zhc1.realworld.model.User;
import io.zhc1.realworld.model.UserRegistry;
import io.zhc1.realworld.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("UserController - User Authentication and Profile Management API")
class UserControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserService userService;

    @Test
    @DisplayName("POST /api/users/login - Should login successfully with valid credentials")
    void whenLoginWithValidCredentials_thenShouldReturn201Created() throws Exception {
        // given
        String requestBody =
                """
                {
                    "user": {
                        "email": "test@example.com",
                        "password": "password123"
                    }
                }
                """;

        User user = new User("test@example.com", "testuser", "password123");
        when(userService.login("test@example.com", "password123")).thenReturn(user);

        // when & then
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andExpect(jsonPath("$.user.token").exists());
    }

    @Test
    @DisplayName("POST /api/users/login - Should fail with invalid email")
    void whenLoginWithInvalidEmail_thenShouldReturn400BadRequest() throws Exception {
        // given
        String requestBody =
                """
                {
                    "user": {
                        "email": "invalid@example.com",
                        "password": "password123"
                    }
                }
                """;

        when(userService.login("invalid@example.com", "password123"))
                .thenThrow(new IllegalArgumentException("Invalid email or password"));

        // when & then
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/user - Should return current user with valid token")
    void whenGetUserWithValidToken_thenShouldReturn200Ok() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        User user = new User("test@example.com", "testuser", "password123");

        when(userService.getUser(userId)).thenReturn(user);

        // when & then
        mockMvc.perform(get("/api/user").with(jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.user.username").value("testuser"));
    }

    @Test
    @DisplayName("PUT /api/user - Should update user successfully with valid data")
    void whenUpdateUserWithValidData_thenShouldReturn200Ok() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        String requestBody =
                """
                {
                    "user": {
                        "email": "updated@example.com",
                        "username": "updateduser",
                        "password": "newpassword",
                        "bio": "Updated bio",
                        "image": "https://example.com/image.jpg"
                    }
                }
                """;

        User updatedUser = new User("updated@example.com", "updateduser", "newpassword");
        updatedUser.setBio("Updated bio");
        updatedUser.setImageUrl("https://example.com/image.jpg");

        when(userService.updateUserDetails(
                        any(UUID.class),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString()))
                .thenReturn(updatedUser);

        // when & then
        mockMvc.perform(put("/api/user")
                        .with(jwt().jwt(jwt -> jwt.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("updated@example.com"))
                .andExpect(jsonPath("$.user.username").value("updateduser"))
                .andExpect(jsonPath("$.user.bio").value("Updated bio"))
                .andExpect(jsonPath("$.user.image").value("https://example.com/image.jpg"));
    }

    @Test
    @DisplayName("GET /api/user - Should fail without authentication token")
    void whenGetUserWithoutToken_thenShouldReturn401Unauthorized() throws Exception {
        // when & then
        mockMvc.perform(get("/api/user")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/user - Should fail without authentication token")
    void whenUpdateUserWithoutToken_thenShouldReturn401Unauthorized() throws Exception {
        // given
        String requestBody =
                """
                {
                    "user": {
                        "email": "updated@example.com",
                        "username": "updateduser"
                    }
                }
                """;

        // when & then
        mockMvc.perform(put("/api/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }
}
