package com.becommerce.crm.shared.security.authorization;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Tela consumidora → permissões donas cujo LOOKUP ela pode ler. Só leitura, não transitivo, opt-in. */
public final class PermissionDependencies {

    // Piloto: selects de serviços do catálogo em orçamento (contact:read) e agenda (appointment:read).
    private static final Map<String, Set<String>> SCREEN_TO_OWNERS = Map.ofEntries(
            Map.entry("appointment:read", Set.of("catalog:read")),
            Map.entry("contact:read", Set.of("catalog:read"))
    );

    private PermissionDependencies() {}

    public static Map<String, Set<String>> all() {
        return SCREEN_TO_OWNERS;
    }

    public static Set<String> screensDependingOn(String owner) {
        return SCREEN_TO_OWNERS.entrySet().stream()
                .filter(entry -> entry.getValue().contains(owner))
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }
}
