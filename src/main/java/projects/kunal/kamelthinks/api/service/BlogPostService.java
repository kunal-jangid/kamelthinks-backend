package projects.kunal.kamelthinks.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import projects.kunal.kamelthinks.api.model.BlogPost;
import projects.kunal.kamelthinks.api.repository.BlogPostRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BlogPostService {
    @Autowired
    private BlogPostRepository blogPostRepository;

    public BlogPost createPost(BlogPost post) {
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        return blogPostRepository.save(post);
    }

    public BlogPost getPost(String slug) {
        return blogPostRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Post not found"));
    }

    public List<BlogPost> getAllPosts() {
        return blogPostRepository.findAll();
    }

    public BlogPost updatePost(String slug, BlogPost newPost) {
        BlogPost post = getPost(slug);
        post.setTitle(newPost.getTitle());
        post.setMarkdown(newPost.getMarkdown());
        post.setUpdatedAt(LocalDateTime.now());
        return blogPostRepository.save(post);
    }

    public void deletePost(String slug) {
        BlogPost post = getPost(slug);
        blogPostRepository.delete(post);
    }
}