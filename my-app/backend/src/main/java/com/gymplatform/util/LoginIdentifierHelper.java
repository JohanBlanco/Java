package com.gymplatform.util;

import com.gymplatform.domain.entity.User;
import com.gymplatform.repository.UserRepository;

import java.util.List;
import java.util.Optional;

public final class LoginIdentifierHelper {

    private LoginIdentifierHelper() {}

    public static Optional<User> resolveUser(UserRepository userRepository, String login) {
        if (login == null) {
            return Optional.empty();
        }
        String trimmed = login.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        if (trimmed.contains("@")) {
            return userRepository.findByEmail(trimmed);
        }

        String nationalId = NationalIdHelper.normalize(trimmed);
        if (!NationalIdHelper.isValid(nationalId)) {
            return Optional.empty();
        }

        List<User> users = userRepository.findAllByNationalId(nationalId);
        if (users.isEmpty()) {
            return Optional.empty();
        }
        if (users.size() > 1) {
            throw new IllegalStateException(
                    "Varias cuentas usan esa cédula. Inicia sesión con tu correo electrónico.");
        }

        User user = users.get(0);
        return user.isActive() ? Optional.of(user) : Optional.empty();
    }
}
