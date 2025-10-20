package io.zhc1.realworld.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import io.zhc1.realworld.model.Tag;
import io.zhc1.realworld.service.TagService;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("TagController - Tag Management API")
class TagControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    TagService tagService;

    @Test
    @DisplayName("GET /api/tags - Should return all tags successfully")
    void whenGetAllTags_thenShouldReturn200Ok() throws Exception {
        // given
        List<Tag> tags = List.of(new Tag("java"), new Tag("spring"), new Tag("testing"));
        when(tagService.getAllTags()).thenReturn(tags);

        // when & then
        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags").isArray())
                .andExpect(jsonPath("$.tags[0]").value("java"))
                .andExpect(jsonPath("$.tags[1]").value("spring"))
                .andExpect(jsonPath("$.tags[2]").value("testing"));
    }

    @Test
    @DisplayName("GET /api/tags - Should return empty array when no tags exist")
    void whenGetAllTagsWithNoTags_thenShouldReturn200OkWithEmptyArray() throws Exception {
        // given
        when(tagService.getAllTags()).thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags").isArray())
                .andExpect(jsonPath("$.tags").isEmpty());
    }
}
