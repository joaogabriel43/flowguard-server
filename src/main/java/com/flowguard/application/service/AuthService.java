package com.flowguard.application.service;

import com.flowguard.application.dto.LoginCommand;
import com.flowguard.application.dto.RegisterCommand;
import com.flowguard.application.dto.TokenResponse;
import com.flowguard.domain.exception.ConflictException;
import com.flowguard.domain.exception.UnauthorizedException;
import com.flowguard.domain.model.Tenant;
import com.flowguard.domain.model.User;
import com.flowguard.domain.repository.TenantRepository;
import com.flowguard.domain.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(TenantRepository tenantRepository,
                       UserRepository userRepository,
                       TokenService tokenService,
                       PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public TokenResponse register(RegisterCommand command) {
        if (userRepository.findByEmail(command.email()).isPresent()) {
            throw new ConflictException("User with email " + command.email() + " already exists");
        }
        if (tenantRepository.findByName(command.tenantName()).isPresent()) {
            throw new ConflictException("Tenant with name " + command.tenantName() + " already exists");
        }

        // Create Tenant
        String apiKey = "fg_" + UUID.randomUUID().toString().replace("-", "");
        Tenant tenant = Tenant.builder()
                .name(command.tenantName())
                .apiKey(apiKey)
                .build();
        tenant = tenantRepository.save(tenant);

        // Create User (Admin role as requested)
        User user = User.builder()
                .tenantId(tenant.getId())
                .email(command.email())
                .passwordHash(passwordEncoder.encode(command.password()))
                .role("ADMIN")
                .build();
        userRepository.save(user);

        // Generate Token immediately or return OK? The requirements say:
        // "POST /auth/register para cadastro inicial do tenant e de um usuário com role de admin."
        // We will generate the JWT token and return it as TokenResponse to make registration seamless!
        return loginUser(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginCommand command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        return loginUser(user);
    }

    private TokenResponse loginUser(User user) {
        Map<String, Object> claims = Map.of(
            "userId", user.getId().toString(),
            "tenantId", user.getTenantId().toString(),
            "role", user.getRole()
        );

        String token = tokenService.generateToken(user.getEmail(), claims);
        return new TokenResponse(token);
    }
}
