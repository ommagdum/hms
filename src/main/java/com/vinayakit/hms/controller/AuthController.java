package com.vinayakit.hms.controller;

import com.vinayakit.hms.dto.ApiResponse;
import com.vinayakit.hms.dto.LoginRequest;
import com.vinayakit.hms.dto.LoginResponseDto;
import com.vinayakit.hms.security.UserDetailsImpl;
import com.vinayakit.hms.security.jwt.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> authenticateUser(
            @Valid @RequestBody LoginRequest loginRequest
            ) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String role = userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");

        LoginResponseDto response = new LoginResponseDto(jwt, "Bearer", userDetails.getId(), userDetails.getUsername(), role);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }
}
