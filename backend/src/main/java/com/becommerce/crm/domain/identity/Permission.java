package com.becommerce.crm.domain.identity;

import java.time.LocalDateTime;
import java.util.UUID;

public class Permission {
    private UUID id;
    private String name;
    private String description;
    private String module;
    private String resource;
    private String action;
    private LocalDateTime createdAt;

    public Permission() {}

    public static Permission create(String name, String description, String module, String resource, String action) {
        Permission p = new Permission();
        p.id = UUID.randomUUID();
        p.name = name;
        p.description = description;
        p.module = module;
        p.resource = resource;
        p.action = action;
        p.createdAt = LocalDateTime.now();
        return p;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
