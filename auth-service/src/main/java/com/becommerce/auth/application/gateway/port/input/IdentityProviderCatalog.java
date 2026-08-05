package com.becommerce.auth.application.gateway.port.input;

import java.util.List;
import java.util.Optional;

/**
 * Catálogo de provedores de identidade (Identity Brokering, Sprint 7.0).
 *
 * <p>Fonte de verdade para a tela de login: quais provedores existem
 * (registro), como aparecem para o usuário ({@code label}) e se estão
 * realmente disponíveis ({@code available}). A disponibilidade é controlada
 * no servidor ({@code auth.gateway.enabled-providers}) — o browser apenas
 * exibe o que o catálogo devolve e nunca decide qual bucket/limite usa.
 *
 * <p>Meta/Facebook está <b>fora de escopo</b> e não consta do registro.
 */
public interface IdentityProviderCatalog {

    /** Todos os provedores suportados, na ordem de exibição da tela de login. */
    List<IdentityProviderInfo> list();

    /** Busca por alias ({@code google}, {@code microsoft}, {@code apple}, {@code phone}). */
    Optional<IdentityProviderInfo> find(String alias);

    record IdentityProviderInfo(String alias, String label, boolean available) {
    }
}
