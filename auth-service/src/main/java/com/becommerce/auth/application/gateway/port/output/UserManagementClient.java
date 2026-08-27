package com.becommerce.auth.application.gateway.port.output;

import java.util.Map;

/**
 * Porta de saída para gerenciamento de usuários via Keycloak Admin REST.
 * Utilizada pelo crm-backend para criar usuários durante o registro por
 * e-mail/senha.
 */
public interface UserManagementClient {

    /**
     * Busca um usuário no Keycloak por e-mail (busca exata).
     *
     * @param email e-mail do usuário
     * @return array JSON com os usuários encontrados, ou array vazio
     */
    Map<String, Object>[] findUserByEmail(String email);

    /**
     * Cria um usuário no Keycloak.
     *
     * @param email     e-mail do usuário
     * @param password  senha em texto puro (Keycloak hashea internamente)
     * @param firstName primeiro nome
     * @param lastName  sobrenome (pode ser vazio)
     * @return ID do usuário criado no Keycloak (UUID como String)
     */
    String createUser(String email, String password, String firstName, String lastName);

    /**
     * Exclui um usuário do Keycloak pelo ID.
     * Utilizado como compensação quando a persistência no CRM falha.
     *
     * @param keycloakUserId ID do usuário no Keycloak
     */
    void deleteUser(String keycloakUserId);
}
