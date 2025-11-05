package io.zhc1.realworld.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import io.zhc1.realworld.config.AuthTokenProvider;
import io.zhc1.realworld.model.Article;
import io.zhc1.realworld.model.ArticleComment;
import io.zhc1.realworld.model.User;
import io.zhc1.realworld.service.ArticleCommentService;
import io.zhc1.realworld.service.ArticleService;
import io.zhc1.realworld.service.UserRelationshipService;
import io.zhc1.realworld.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ArticleCommentController - Article Comment Management API")
class ArticleCommentControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserService userService;

    @MockBean
    UserRelationshipService userRelationshipService;

    @MockBean
    ArticleService articleService;

    @MockBean
    ArticleCommentService articleCommentService;

    @Autowired
    AuthTokenProvider authTokenProvider;

    private User createUserWithId(UUID id, String email, String username, String password) throws Exception {
        User user = new User(email, username, password);
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, id);
        return user;
    }

    private Article createArticleWithSlug(User author, String slug, String title) throws Exception {
        Article article = new Article(author, title, "description", "body");
        Field slugField = Article.class.getDeclaredField("slug");
        slugField.setAccessible(true);
        slugField.set(article, slug);
        Field idField = Article.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(article, 1);
        return article;
    }

    private ArticleComment createCommentWithId(int id, Article article, User commenter, String body)
            throws Exception {
        ArticleComment comment = new ArticleComment(article, commenter, body);
        Field idField = ArticleComment.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(comment, id);
        return comment;
    }

    @Test
    @DisplayName("POST /api/articles/{slug}/comments - Should create comment successfully")
    void whenPostComment_thenShouldReturn200Ok() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        User user = createUserWithId(userId, "test@example.com", "testuser", "password123");
        String token = authTokenProvider.createAuthToken(user);

        String slug = "test-article";
        Article article = createArticleWithSlug(user, slug, "Test Article");
        ArticleComment comment = createCommentWithId(1, article, user, "Great article!");

        String requestBody =
                """
                {
                    "comment": {
                        "body": "Great article!"
                    }
                }
                """;

        when(articleService.getArticle(slug)).thenReturn(article);
        when(userService.getUser(userId)).thenReturn(user);
        when(articleCommentService.write(any(ArticleComment.class))).thenReturn(comment);

        // when & then
        mockMvc.perform(post("/api/articles/{slug}/comments", slug)
                        .header("Authorization", "Token " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment.body").value("Great article!"));
    }

    @Test
    @DisplayName("GET /api/articles/{slug}/comments - Should return comments for authenticated user")
    void whenGetCommentsWithAuthentication_thenShouldReturn200Ok() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        User user = createUserWithId(userId, "test@example.com", "testuser", "password123");
        String token = authTokenProvider.createAuthToken(user);

        String slug = "test-article";
        Article article = createArticleWithSlug(user, slug, "Test Article");

        User commenter = createUserWithId(UUID.randomUUID(), "commenter@example.com", "commenter", "password123");
        ArticleComment comment = createCommentWithId(1, article, commenter, "Great article!");

        when(articleService.getArticle(slug)).thenReturn(article);
        when(articleCommentService.getComments(article)).thenReturn(List.of(comment));
        when(userService.getUser(userId)).thenReturn(user);
        when(userRelationshipService.isFollowing(user, commenter)).thenReturn(true);

        // when & then
        mockMvc.perform(get("/api/articles/{slug}/comments", slug)
                        .header("Authorization", "Token " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments[0].body").value("Great article!"));
    }

    @Test
    @DisplayName("GET /api/articles/{slug}/comments - Should return comments for anonymous user")
    void whenGetCommentsWithoutAuthentication_thenShouldReturn200Ok() throws Exception {
        // given
        String slug = "test-article";
        User author = createUserWithId(UUID.randomUUID(), "author@example.com", "author", "password123");
        Article article = createArticleWithSlug(author, slug, "Test Article");

        User commenter = createUserWithId(UUID.randomUUID(), "commenter@example.com", "commenter", "password123");
        ArticleComment comment = createCommentWithId(1, article, commenter, "Great article!");

        when(articleService.getArticle(slug)).thenReturn(article);
        when(articleCommentService.getComments(article)).thenReturn(List.of(comment));

        // when & then
        mockMvc.perform(get("/api/articles/{slug}/comments", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments[0].body").value("Great article!"));
    }

    @Test
    @DisplayName("DELETE /api/articles/{slug}/comments/{id} - Should delete comment successfully")
    void whenDeleteComment_thenShouldReturn200Ok() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        User user = createUserWithId(userId, "test@example.com", "testuser", "password123");
        String token = authTokenProvider.createAuthToken(user);

        String slug = "test-article";
        Article article = createArticleWithSlug(user, slug, "Test Article");
        ArticleComment comment = createCommentWithId(1, article, user, "Great article!");

        when(userService.getUser(userId)).thenReturn(user);
        when(articleCommentService.getComment(anyInt())).thenReturn(comment);
        doNothing().when(articleCommentService).delete(any(User.class), any(ArticleComment.class));

        // when & then
        mockMvc.perform(delete("/api/articles/{slug}/comments/{id}", slug, 1)
                        .header("Authorization", "Token " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/articles/{slug}/comments - Should fail without authentication token")
    void whenPostCommentWithoutToken_thenShouldReturn401Unauthorized() throws Exception {
        // given
        String requestBody =
                """
                {
                    "comment": {
                        "body": "Great article!"
                    }
                }
                """;

        // when & then
        mockMvc.perform(post("/api/articles/{slug}/comments", "test-article")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/articles/{slug}/comments/{id} - Should fail without authentication token")
    void whenDeleteCommentWithoutToken_thenShouldReturn401Unauthorized() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/articles/{slug}/comments/{id}", "test-article", 1))
                .andExpect(status().isUnauthorized());
    }
}
