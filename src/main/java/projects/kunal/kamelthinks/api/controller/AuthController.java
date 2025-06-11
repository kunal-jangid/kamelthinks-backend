package projects.kunal.kamelthinks.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import projects.kunal.kamelthinks.api.dto.AuthRequest;
import projects.kunal.kamelthinks.api.dto.AuthResponse;
import projects.kunal.kamelthinks.api.model.User;
import projects.kunal.kamelthinks.api.repository.UserRepository;
import projects.kunal.kamelthinks.api.security.JwtUtil;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            // If authentication is successful, generate JWT
            String token = jwtUtil.generateToken(request.getUsername());

            // --- CRITICAL PART: Ensure AuthResponse is correctly formed and serialized ---
            AuthResponse authResponse = new AuthResponse(token);
//            System.out.println("Generated Token: " + token); // For debugging
//            System.out.println("AuthResponse Object: " + authResponse); // For debugging
            return ResponseEntity.ok(authResponse); // Returns HTTP 200 OK with AuthResponse JSON
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        } catch (Exception e) {
            // Catch any other unexpected exceptions and log them
            e.printStackTrace(); // Log the full stack trace for unexpected errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during login.");
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validate(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.getUsernameFromToken(token);
        return ResponseEntity.ok("User "+username+" is valid");
    }
}
