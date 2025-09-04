package com.sobunsobun.backend.security;

import com.sobunsobun.backend.domain.Role;

public record JwtUserPrincipal(Long id, Role role) {}
