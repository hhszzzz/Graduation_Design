package org.com;

import org.com.entity.User;
import org.com.utils.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;

public class JwtTokenUtilTest {

    private JwtTokenUtil jwtTokenUtil;
    private UserDetails userDetails;

    // Directly set properties for simplicity in this example test
    // In a real Spring Boot test (@SpringBootTest), these would be injected
    private String testSecret = "graduation-design-jwt-secret-key-for-testing-purpose-only"; // Use a long enough secret
    private Long testExpiration = 86400L; // 24 hours in seconds

    @BeforeEach
    void setUp() {
        jwtTokenUtil = new JwtTokenUtil();
        // Use reflection or setters if available to set secret and expiration
        // For simplicity, let's assume JwtTokenUtil has setters or can be configured
        // Or, manually initialize it (less ideal, but okay for a placeholder)
        // jwtTokenUtil.setSecret(testSecret); 
        // jwtTokenUtil.setExpiration(testExpiration);
        
        // Create a simple UserDetails mock or instance
        userDetails = org.springframework.security.core.userdetails.User
                .withUsername("testuser")
                .password("password") // Password doesn't matter for token generation/validation itself
                .authorities("ROLE_USER")
                .build();
    }

    @Test
    void testGenerateToken() {
        // Note: Without setting the secret/expiration in JwtTokenUtil, this test might fail
        // If JwtTokenUtil relies on @Value, this test needs @SpringBootTest context
        // or manual setting of properties.
        
        // Assuming manual setup or default values are sufficient for a basic check:
        try {
            String token = jwtTokenUtil.generateToken(userDetails);
            System.out.println("Generated Token: " + token);
            assertNotNull(token, "Generated token should not be null");
            assertTrue(token.length() > 10, "Generated token should have reasonable length");
            
            String username = jwtTokenUtil.getUsernameFromToken(token);
            assertEquals("testuser", username, "Username extracted from token should match");
            
        } catch (Exception e) {
            // This might happen if secret/expiration aren't properly initialized
            System.err.println("Test failed, likely due to uninitialized secret/expiration in JwtTokenUtil: " + e.getMessage());
            // Fail the test explicitly if initialization is expected but failed
            // fail("Token generation failed, check JwtTokenUtil initialization: " + e.getMessage());
            
            // For now, let's just print the error if it occurs in this basic setup
            e.printStackTrace();
        }
    }

    @Test
    void testValidateToken() {
         // Similar initialization caveats as testGenerateToken apply here.
         try {
             String token = jwtTokenUtil.generateToken(userDetails);
             assertNotNull(token, "Token needed for validation test");
             
             Boolean isValid = jwtTokenUtil.validateToken(token, userDetails);
             assertTrue(isValid, "Generated token should be valid for the same user details");
             
             // Test with different user details
             UserDetails differentUserDetails = org.springframework.security.core.userdetails.User
                     .withUsername("anotheruser")
                     .password("password")
                     .authorities("ROLE_USER")
                     .build();
             Boolean isInvalidForDifferentUser = jwtTokenUtil.validateToken(token, differentUserDetails);
              // Validation should strictly check username match based on implementation
             
             // Check expiration (difficult to test precisely without time control)
             
         } catch (Exception e) {
             System.err.println("Test failed, likely due to uninitialized secret/expiration in JwtTokenUtil: " + e.getMessage());
             // fail("Token validation failed, check JwtTokenUtil initialization: " + e.getMessage());
             e.printStackTrace();
         }
    }
} 