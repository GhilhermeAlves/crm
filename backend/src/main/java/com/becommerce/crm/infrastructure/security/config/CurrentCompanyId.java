package com.becommerce.crm.infrastructure.security.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca o parâmetro {@code UUID companyId} de um controller como o tenant da
 * request: o resolver central {@link CurrentCompanyIdArgumentResolver} extrai o
 * valor do path ({@code {companyId}}) e valida que a empresa pertence ao usuário
 * autenticado (com bypass para SUPER_ADMIN), à mesma semântica da checagem
 * {@code requireCompanyAccess} que cada controller repetia.
 *
 * <p>{@code value()} é a mensagem pt-BR de negação exibida ao cliente
 * (403 {@code CRM_ACCESS_DENIED}), preservando por recurso a mensagem que o
 * controller usava na checagem manual removida.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentCompanyId {

    String value();
}