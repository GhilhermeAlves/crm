package com.becommerce.crm.domain.pipeline;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Estágio de um pipeline (Sprint 11). Corresponde à tabela {@code stages}
 * (V017). A ordem ({@code order} ≥ 1) define a sequência do funil; a
 * probabilidade (0..100) alimenta o forecast (P-030). Um pipeline possui de
 * 2 a 15 estágios (P-002/P-003).
 */
public class Stage {

    private final UUID id;
    private final UUID pipelineId;
    private final UUID companyId;
    private String name;
    private String color;
    private int orderNum;
    private int probability;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Stage(UUID id, UUID pipelineId, UUID companyId, String name, String color,
                  int orderNum, int probability, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.pipelineId = pipelineId;
        this.companyId = companyId;
        this.name = name;
        this.color = color;
        this.orderNum = orderNum;
        this.probability = probability;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Stage create(UUID pipelineId, UUID companyId, String name, String color,
                               int orderNum, int probability) {
        LocalDateTime now = LocalDateTime.now();
        return new Stage(UUID.randomUUID(), pipelineId, companyId, name, color,
                orderNum, probability, now, now);
    }

    public static Stage reconstitute(UUID id, UUID pipelineId, UUID companyId, String name, String color,
                                     int orderNum, int probability, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Stage(id, pipelineId, companyId, name, color, orderNum, probability, createdAt, updatedAt);
    }

    public void update(String name, String color, Integer probability) {
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
        if (color != null) {
            this.color = color;
        }
        if (probability != null) {
            this.probability = probability;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getPipelineId() { return pipelineId; }
    public UUID getCompanyId() { return companyId; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public int getOrderNum() { return orderNum; }
    public int getProbability() { return probability; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
