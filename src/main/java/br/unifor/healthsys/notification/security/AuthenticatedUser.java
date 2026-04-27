package br.unifor.healthsys.notification.security;

public record AuthenticatedUser(
        Long userId,
        String username,
        String role,
        String email,
        String nome
) {
}
