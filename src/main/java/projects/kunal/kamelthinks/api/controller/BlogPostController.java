package projects.kunal.kamelthinks.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import projects.kunal.kamelthinks.api.model.BlogPost;
import projects.kunal.kamelthinks.api.service.BlogPostService;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "*")
public class BlogPostController {

    @Autowired
    private BlogPostService blogPostService;

    @PostMapping
    public ResponseEntity<BlogPost> create(@RequestBody BlogPost post) {
        BlogPost created = blogPostService.createPost(post);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<BlogPost> getAll() {
        return blogPostService.getAllPosts();
    }

    @GetMapping("/{slug}")
    public BlogPost getOne(@PathVariable String slug) {
        return blogPostService.getPost(slug);
    }

    @PutMapping("/{slug}")
    public BlogPost update(@PathVariable String slug, @RequestBody BlogPost post) {
        return blogPostService.updatePost(slug, post);
    }

    @DeleteMapping("/{slug}")
    public ResponseEntity<?> delete(@PathVariable String slug) {
        blogPostService.deletePost(slug);
        return ResponseEntity.noContent().build();
    }
}
