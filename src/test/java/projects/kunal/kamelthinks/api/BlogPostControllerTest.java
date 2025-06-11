package projects.kunal.kamelthinks.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import projects.kunal.kamelthinks.api.controller.BlogPostController;
import projects.kunal.kamelthinks.api.model.BlogPost;
import projects.kunal.kamelthinks.api.repository.UserRepository;
import projects.kunal.kamelthinks.api.security.JwtUtil;
import projects.kunal.kamelthinks.api.service.BlogPostService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BlogPostController.class)
@AutoConfigureMockMvc(addFilters = false)
class BlogPostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BlogPostService blogPostService;

    // --- ADD THESE MOCKBEANS ---
    // Even if BlogPostController doesn't directly use them, the test context
    // tries to load security-related beans like JwtFilter, which have these dependencies.
    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository; // UserDetailsService depends on this

    @MockBean
    private PasswordEncoder passwordEncoder; // Potentially part of the security chain

    @MockBean
    private AuthenticationManager authenticationManager; // Also part of the security chain
    // --- END ADDITIONS ---

    private BlogPost blogPost1;
    private BlogPost blogPost2;

    @BeforeEach
    void setUp() {
        blogPost1 = new BlogPost();
        blogPost1.setId(1L);
        blogPost1.setTitle("First Post");
        blogPost1.setSlug("first-post");
        blogPost1.setMarkdown("Content of the first post.");
        blogPost1.setCreatedAt(LocalDateTime.now().minusDays(2));
        blogPost1.setUpdatedAt(LocalDateTime.now().minusDays(2));

        blogPost2 = new BlogPost();
        blogPost2.setId(2L);
        blogPost2.setTitle("Second Post");
        blogPost2.setSlug("second-post");
        blogPost2.setMarkdown("Content of the second post.");
        blogPost2.setCreatedAt(LocalDateTime.now().minusDays(1));
        blogPost2.setUpdatedAt(LocalDateTime.now().minusDays(1));
    }

    @Test
    void createBlogPost_success() throws Exception {
        when(blogPostService.createPost(any(BlogPost.class))).thenReturn(blogPost1);

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blogPost1))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is(blogPost1.getTitle())))
                .andExpect(jsonPath("$.slug", is(blogPost1.getSlug())));

        verify(blogPostService, times(1)).createPost(any(BlogPost.class));
    }

    @Test
    void getAllBlogPosts_success() throws Exception {
        List<BlogPost> allPosts = Arrays.asList(blogPost1, blogPost2);
        when(blogPostService.getAllPosts()).thenReturn(allPosts);

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title", is(blogPost1.getTitle())))
                .andExpect(jsonPath("$[1].title", is(blogPost2.getTitle())));

        verify(blogPostService, times(1)).getAllPosts();
    }

    @Test
    void getOneBlogPost_success() throws Exception {
        when(blogPostService.getPost(blogPost1.getSlug())).thenReturn(blogPost1);

        mockMvc.perform(get("/api/posts/{slug}", blogPost1.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is(blogPost1.getTitle())))
                .andExpect(jsonPath("$.slug", is(blogPost1.getSlug())));

        verify(blogPostService, times(1)).getPost(blogPost1.getSlug());
    }

    @Test
    void getOneBlogPost_notFound() throws Exception {
        when(blogPostService.getPost("non-existent-slug"))
                .thenThrow(new RuntimeException("Post not found")); // Or a more specific custom exception if you have one

        mockMvc.perform(get("/api/posts/{slug}", "non-existent-slug"))
                .andExpect(status().isInternalServerError()); // RuntimeException results in 500 by default

        verify(blogPostService, times(1)).getPost("non-existent-slug");
    }

    @Test
    void updateBlogPost_success() throws Exception {
        BlogPost updatedPost = new BlogPost();
        updatedPost.setTitle("Updated Title");
        updatedPost.setMarkdown("Updated Content");
        updatedPost.setSlug(blogPost1.getSlug());

        when(blogPostService.updatePost(eq(blogPost1.getSlug()), any(BlogPost.class)))
                .thenReturn(updatedPost);

        mockMvc.perform(put("/api/posts/{slug}", blogPost1.getSlug())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedPost))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is(updatedPost.getTitle())))
                .andExpect(jsonPath("$.markdown", is(updatedPost.getMarkdown())));

        verify(blogPostService, times(1)).updatePost(eq(blogPost1.getSlug()), any(BlogPost.class));
    }

    @Test
    void updateBlogPost_notFound() throws Exception {
        when(blogPostService.updatePost(eq("non-existent-slug"), any(BlogPost.class)))
                .thenThrow(new RuntimeException("Post not found"));

        mockMvc.perform(put("/api/posts/{slug}", "non-existent-slug")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BlogPost()))
                        .with(csrf()))
                .andExpect(status().isInternalServerError());

        verify(blogPostService, times(1)).updatePost(eq("non-existent-slug"), any(BlogPost.class));
    }

    @Test
    void deleteBlogPost_success() throws Exception {
        doNothing().when(blogPostService).deletePost(blogPost1.getSlug());

        mockMvc.perform(delete("/api/posts/{slug}", blogPost1.getSlug())
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(blogPostService, times(1)).deletePost(blogPost1.getSlug());
    }

    @Test
    void deleteBlogPost_notFound() throws Exception {
        doThrow(new RuntimeException("Post not found"))
                .when(blogPostService).deletePost("non-existent-slug");

        mockMvc.perform(delete("/api/posts/{slug}", "non-existent-slug")
                        .with(csrf()))
                .andExpect(status().isInternalServerError());

        verify(blogPostService, times(1)).deletePost("non-existent-slug");
    }
}