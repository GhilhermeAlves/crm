package com.becommerce.crm.masterdata.person;

import com.becommerce.crm.masterdata.person.PersonDtos.HistoryEntry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Histórico por pessoa (doc seção 7): gravado na mesma transação da alteração, só com os campos
 * que mudaram. Valores sensíveis (dados bancários, documentos) entram apenas como "alterado".
 */
@Service
public class PersonHistoryService {

    private static final Set<String> SENSITIVE = Set.of("branchNumber", "accountNumber", "accountDigit", "pixKey",
            "documentNumber", "salary");
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public PersonHistoryService(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public void record(UUID companyId, UUID personId, UUID userId, String operation, String table,
                       Map<String, ?> oldValues, Map<String, ?> newValues, String reason) {
        Map<String, Object> before = new LinkedHashMap<>();
        Map<String, Object> after = new LinkedHashMap<>();
        if (oldValues != null && newValues != null) {
            for (String k : union(oldValues, newValues)) {
                Object o = oldValues.get(k);
                Object n = newValues.get(k);
                if (!Objects.equals(str(o), str(n))) {
                    before.put(k, SENSITIVE.contains(k) && o != null ? "(alterado)" : o);
                    after.put(k, SENSITIVE.contains(k) && n != null ? "(alterado)" : n);
                }
            }
            if (before.isEmpty() && after.isEmpty() && "UPDATE".equals(operation)) return;
        } else {
            if (oldValues != null) oldValues.forEach((k, v) -> before.put(k, SENSITIVE.contains(k) && v != null ? "(informado)" : v));
            if (newValues != null) newValues.forEach((k, v) -> after.put(k, SENSITIVE.contains(k) && v != null ? "(informado)" : v));
        }
        jdbc.update("INSERT INTO person_history (company_id, person_id, user_id, operation, table_name, old_values, "
                        + "new_values, reason) VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?)",
                companyId, personId, userId, operation, table, json(before.isEmpty() ? null : before),
                json(after.isEmpty() ? null : after), reason);
    }

    public List<HistoryEntry> list(UUID companyId, UUID personId) {
        return jdbc.query("SELECT h.id, h.occurred_at, h.operation, h.table_name, h.old_values::text AS o, "
                        + "h.new_values::text AS n, h.reason, h.user_id, "
                        + "nullif(trim(coalesce(u.first_name,'') || ' ' || coalesce(u.last_name,'')), '') AS uname, u.email "
                        + "FROM person_history h LEFT JOIN users u ON u.id = h.user_id "
                        + "WHERE h.company_id = ? AND h.person_id = ? ORDER BY h.occurred_at DESC LIMIT 300",
                (rs, i) -> new HistoryEntry(rs.getObject("id", UUID.class),
                        rs.getObject("occurred_at", OffsetDateTime.class), rs.getString("operation"),
                        rs.getString("table_name"), parse(rs.getString("o")), parse(rs.getString("n")),
                        rs.getString("reason"), rs.getObject("user_id", UUID.class),
                        rs.getString("uname") != null ? rs.getString("uname")
                                : rs.getString("email") != null ? rs.getString("email") : "Sistema"),
                companyId, personId);
    }

    private static java.util.Set<String> union(Map<String, ?> a, Map<String, ?> b) {
        java.util.LinkedHashSet<String> s = new java.util.LinkedHashSet<>(a.keySet());
        s.addAll(b.keySet());
        return s;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private String json(Map<String, Object> m) {
        if (m == null) return null;
        try {
            return mapper.writeValueAsString(m);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> parse(String s) {
        if (s == null) return null;
        try {
            return mapper.readValue(s, MAP);
        } catch (Exception e) {
            return null;
        }
    }
}
