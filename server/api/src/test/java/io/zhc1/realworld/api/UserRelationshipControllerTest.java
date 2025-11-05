package io.zhc1.realworld.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Field;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import io.zhc1.realworld.config.AuthTokenProvider;
import io.zhc1.realworld.model.User;
import io.zhc1.realworld.service.UserRelationshipService;
import io.zhc1.realworld.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("UserRelationshipController - User Follow/Unfollow API")
class UserRelationshipControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserService userService;

    @MockBean
    UserRelationshipService userRelationshipService;

    @Autowired
    AuthTokenProvider authTokenProvider;

    private User createUserWithId(UUID id, String email, String username, String password) throws Exception {
        User user = new User(email, username, password);
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, id);
        return user;
    }

    @Test
    @DisplayName("GET /api/profiles/{username} - Should return user profile for authenticated user")
    void whenGetProfileWithAuthentication_thenShouldReturn200Ok() throws Exception {
        // given
        UUID viewerId = UUID.randomUUID();
        User viewer = createUserWithId(viewerId, "viewer@example.com", "viewer", "password123");
        String token = authTokenProvider.createAuthToken(viewer);

        User targetUser = createUserWithId(UUID.randomUUID(), "target@example.com", "targetuser", "password123");

        when(userService.getUser(viewerId)).thenReturn(viewer);
        when(userService.getUser("targetuser")).thenReturn(targetUser);
        when(userRelationshipService.isFollowing(viewer, targetUser)).thenReturn(true);

        // when & then
        mockMvc.perform(get("/api/profiles/{username}", "targetuser")
                        .header("Authorization", "Token " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value("targetuser"))
                .andExpect(jsonPath("$.profile.following").value(true));
    }

    @Test
    @DisplayName("GET /api/profiles/{username} - Should return user profile for anonymous user")
    void whenGetProfileWithoutAuthentication_thenShouldReturn200Ok() throws Exception {
        // given
        User targetUser = createUserWithId(UUID.randomUUID(), "target@example.com", "targetuser", "password123");
        when(userService.getUser("targetuser")).thenReturn(targetUser);

        // when & then
        mockMvc.perform(get("/api/profiles/{username}", "targetuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value("targetuser"))
                .andExpect(jsonPath("$.profile.following").value(false));
    }

    @Test
    @DisplayName("POST /api/profiles/{username}/follow - Should follow user successfully")
    void whenFollowUser_thenShouldReturn200Ok() throws Exception {
        // given
        UUID followerId = UUID.randomUUID();
        User follower = createUserWithId(followerId, "follower@example.com", "follower", "password123");
        String token = authTokenProvider.createAuthToken(follower);

        User following = createUserWithId(UUID.randomUUID(), "following@example.com", "followinguser", "password123");

        when(userService.getUser(followerId)).thenReturn(follower);
        when(userService.getUser("followinguser")).thenReturn(following);
        doNothing().when(userRelationshipService).follow(any(User.class), any(User.class));

        // when & then
        mockMvc.perform(post("/api/profiles/{username}/follow", "followinguser")
                        .header("Authorization", "Token " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value("followinguser"))
                .andExpect(jsonPath("$.profile.following").value(true));
    }

    @Test
    @DisplayName("DELETE /api/profiles/{username}/follow - Should unfollow user successfully")
    void whenUnfollowUser_thenShouldReturn200Ok() throws Exception {
        // given
        UUID followerId = UUID.randomUUID();
        User follower = createUserWithId(followerId, "follower@example.com", "follower", "password123");
        String token = authTokenProvider.createAuthToken(follower);

        User following = createUserWithId(UUID.randomUUID(), "following@example.com", "followinguser", "password123");

        when(userService.getUser(followerId)).thenReturn(follower);
        when(userService.getUser("followinguser")).thenReturn(following);
        doNothing().when(userRelationshipService).unfollow(any(User.class), any(User.class));

        // when & then
        mockMvc.perform(delete("/api/profiles/{username}/follow", "followinguser")
                        .header("Authorization", "Token " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value("followinguser"))
                .andExpect(jsonPath("$.profile.following").value(false));
    }

    @Test
    @DisplayName("POST /api/profiles/{username}/follow - Should fail without authentication token")
    void whenFollowUserWithoutToken_thenShouldReturn401Unauthorized() throws Exception {
        // when & then
        mockMvc.perform(post("/api/profiles/{username}/follow", "targetuser"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/profiles/{username}/follow - Should fail without authentication token")
    void whenUnfollowUserWithoutToken_thenShouldReturn401Unauthorized() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/profiles/{username}/follow", "targetuser"))
                .andExpect(status().isUnauthorized());
    }
}
