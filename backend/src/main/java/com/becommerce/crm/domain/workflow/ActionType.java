package com.becommerce.crm.domain.workflow;

/**
 * Tipos de ação executáveis (Item 4). Apenas ações de baixo risco e controladas
 * pelo backend. A lista é extensível (Item 15) sem alteração estrutural, pois a
 * configuração de cada ação é um payload JSON (config).
 */
public enum ActionType {
    CREATE_TASK,
    CREATE_ACTIVITY
}
