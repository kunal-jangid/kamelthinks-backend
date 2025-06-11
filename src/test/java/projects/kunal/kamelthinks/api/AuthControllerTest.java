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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import projects.kunal.kamelthinks.api.controller.AuthController;
import projects.kunal.kamelthinks.api.dto.AuthRequest;
import projects.kunal.kamelthinks.api.model.User;
import projects.kunal.kamelthinks.api.repository.UserRepository;
import projects.kunal.kamelthinks.api.security.JwtUtil;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Add AutoConfigureMockMvc with addFilters = false
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // <--- THIS IS THE KEY CHANGE
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtUtil jwtUtil;

    private AuthRequest authRequest;
    private User user;

    @BeforeEach
    void setUp() {
        authRequest = new AuthRequest("testuser", "password"); // Assuming you added this constructor
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("encodedPassword");
    }

    @Test
    void registerUser_success() throws Exception {
        when(userRepository.findByUsername(authRequest.getUsername())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(authRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));

        verify(userRepository, times(1)).findByUsername(authRequest.getUsername());
        verify(passwordEncoder, times(1)).encode(authRequest.getPassword());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void registerUser_usernameConflict() throws Exception {
        when(userRepository.findByUsername(authRequest.getUsername())).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(content().string("Username already exists"));

        verify(userRepository, times(1)).findByUsername(authRequest.getUsername());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginUser_success() throws Exception {
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtil.generateToken(authRequest.getUsername())).thenReturn("mocked_jwt_token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"token\":\"mocked_jwt_token\"}"));

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil, times(1)).generateToken(authRequest.getUsername());
    }

    @Test
    void loginUser_invalidCredentials() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest))
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid credentials"));

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    void validateToken_success() throws Exception {
        String token = "mocked_jwt_token";
        String username = "testuser";
        when(jwtUtil.getUsernameFromToken(token)).thenReturn(username);

        // For GET requests, CSRF is generally not required by Spring Security itself
        // unless you explicitly configure it. However, if addFilters=false, then
        // CSRF filter isn't even active.
        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("User "+username+" is valid"));

        verify(jwtUtil, times(1)).getUsernameFromToken(token);
    }
}