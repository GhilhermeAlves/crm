package com.becommerce.crm.masterdata.company.domain;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Segmento + módulos habilitados de uma empresa (colunas {@code segment} e
 * {@code enabled_modules} de {@code companies}, V104). {@code enabled_modules}
 * é persistido como CSV ({@code CRM,PATIENTS,...}); valores desconhecidos são
 * ignorados na leitura.
 */
public record CompanyModules(CompanySegment segment, Set<CompanyModule> enabledModules) {

    public CompanyModules {
        Objects.requireNonNull(segment, "segment");
        enabledModules = enabledModules == null || enabledModules.isEmpty()
                ? EnumSet.noneOf(CompanyModule.class)
                : EnumSet.copyOf(enabledModules);
    }

    public boolean isEnabled(CompanyModule module) {
        return enabledModules.contains(module);
    }

    /** Ordem estável (declaração do enum) para respostas e persistência. */
    public List<CompanyModule> orderedModules() {
        return enabledModules.stream().sorted().toList();
    }

    public String toCsv() {
        return orderedModules().stream().map(Enum::name).collect(Collectors.joining(","));
    }

    public static Set<CompanyModule> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return EnumSet.noneOf(CompanyModule.class);
        }
        return parse(Arrays.asList(csv.split(",")));
    }

    public static Set<CompanyModule> parse(Collection<String> names) {
        EnumSet<CompanyModule> modules = EnumSet.noneOf(CompanyModule.class);
        if (names == null) {
            return modules;
        }
        for (String name : names) {
            if (name == null) {
                continue;
            }
            String normalized = name.trim().toUpperCase();
            for (CompanyModule module : CompanyModule.values()) {
                if (module.name().equals(normalized)) {
                    modules.add(module);
                }
            }
        }
        return modules;
    }
}
