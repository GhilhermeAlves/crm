package com.becommerce.crm.masterdata.location;

import com.becommerce.crm.masterdata.person.PersonException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Localidades (doc seção 19) e busca de endereço por CEP (seção 20.3): consulta o ViaCEP,
 * localiza a cidade pelo código IBGE e localiza ou cria o bairro (origem C).
 */
@Service
public class LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationService.class);

    public record Country(Integer id, String name, String iso2, String iso3, Integer bacenCode, String dialCode,
                         String nationality, boolean active) {}

    public record State(Integer id, Integer countryId, String name, String uf, Integer ibgeCode, String region, boolean active) {}

    public record City(Integer id, Integer stateId, String name, String uf, Integer ibgeCode, String areaCode,
                       boolean capital, boolean active) {}

    public record Neighborhood(UUID id, Integer cityId, String name, String source, boolean active) {}

    public record CountryRequest(String name, String iso2, String iso3, Integer bacenCode, String dialCode, String nationality,
                                 Boolean active) {}

    public record StateRequest(Integer countryId, String name, String uf, Integer ibgeCode, String region, Boolean active) {}

    public record CityRequest(Integer stateId, String name, Integer ibgeCode, String areaCode, Boolean capital, Boolean active) {}

    public record NeighborhoodRequest(Integer cityId, String name, Boolean active) {}

    public record PostalCodeResult(String postalCode, String street, String complement, UUID neighborhoodId,
                                   String neighborhoodName, Integer cityId, String cityName, String uf, Integer ibgeCode) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ViaCep(String cep, String logradouro, String complemento, String bairro, String localidade, String uf, String ibge,
                  Boolean erro) {}

    private final JdbcTemplate jdbc;
    private final RestClient http;

    public LocationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(4).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(6).toMillis());
        this.http = RestClient.builder().requestFactory(factory).baseUrl("https://viacep.com.br").build();
    }

    // ---------------------------------------------------------------- consultas

    public List<Country> countries(boolean onlyActive) {
        return jdbc.query("SELECT * FROM countries" + (onlyActive ? " WHERE active" : "") + " ORDER BY (id <> 1), country_name",
                (rs, i) -> new Country(rs.getInt("id"), rs.getString("country_name"), rs.getString("iso2"), rs.getString("iso3"),
                        (Integer) rs.getObject("bacen_code", Integer.class), rs.getString("country_dial_code"),
                        rs.getString("nationality"), rs.getBoolean("active")));
    }

    public List<State> states(Integer countryId, boolean onlyActive) {
        return jdbc.query("SELECT * FROM states WHERE country_id = ?" + (onlyActive ? " AND active" : "") + " ORDER BY state_name",
                (rs, i) -> new State(rs.getInt("id"), rs.getInt("country_id"), rs.getString("state_name"), rs.getString("uf"),
                        (Integer) rs.getObject("ibge_code", Integer.class), rs.getString("region"), rs.getBoolean("active")),
                countryId);
    }

    public List<City> cities(Integer stateId, String q, boolean onlyActive, int limit) {
        String term = q == null || q.isBlank() ? null : q.trim();
        return jdbc.query("SELECT c.*, s.uf FROM cities c JOIN states s ON s.id = c.state_id WHERE c.state_id = ?"
                        + (onlyActive ? " AND c.active" : "")
                        + (term == null ? "" : " AND app.normalize_name(c.city_name) LIKE '%' || app.normalize_name(?) || '%'")
                        + " ORDER BY c.is_capital DESC, c.city_name LIMIT " + Math.max(1, Math.min(limit, 6000)),
                (rs, i) -> city(rs), term == null ? new Object[]{stateId} : new Object[]{stateId, term});
    }

    public City cityByIbge(int ibge) {
        List<City> l = jdbc.query("SELECT c.*, s.uf FROM cities c JOIN states s ON s.id = c.state_id WHERE c.ibge_code = ?",
                (rs, i) -> city(rs), ibge);
        if (l.isEmpty()) throw PersonException.notFound("Cidade");
        return l.get(0);
    }

    public List<Neighborhood> neighborhoods(Integer cityId, String q, boolean onlyActive) {
        String term = q == null || q.isBlank() ? null : q.trim();
        return jdbc.query("SELECT * FROM neighborhoods WHERE city_id = ?" + (onlyActive ? " AND active" : "")
                        + (term == null ? "" : " AND normalized_name LIKE '%' || app.normalize_name(?) || '%'")
                        + " ORDER BY neighborhood_name LIMIT 500",
                (rs, i) -> new Neighborhood(rs.getObject("id", UUID.class), rs.getInt("city_id"), rs.getString("neighborhood_name"),
                        rs.getString("source"), rs.getBoolean("active")),
                term == null ? new Object[]{cityId} : new Object[]{cityId, term});
    }

    private static City city(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new City(rs.getInt("id"), rs.getInt("state_id"), rs.getString("city_name"), rs.getString("uf"),
                (Integer) rs.getObject("ibge_code", Integer.class), rs.getString("area_code"), rs.getBoolean("is_capital"),
                rs.getBoolean("active"));
    }

    // ---------------------------------------------------------------- manutenção

    @Transactional
    public Country saveCountry(Integer id, CountryRequest r, UUID userId) {
        if (r.name() == null || r.name().isBlank() || r.iso2() == null || r.iso2().trim().length() != 2) {
            throw PersonException.badRequest("Informe nome e sigla ISO de 2 letras.");
        }
        try {
            if (id == null) {
                id = jdbc.queryForObject("INSERT INTO countries (country_name, iso2, iso3, bacen_code, country_dial_code, nationality, "
                                + "created_by) VALUES (?,?,?,?,?,?,?) RETURNING id", Integer.class, r.name().trim(),
                        r.iso2().trim().toUpperCase(Locale.ROOT), up(r.iso3()), r.bacenCode(), blank(r.dialCode()),
                        blank(r.nationality()), userId);
            } else {
                jdbc.update("UPDATE countries SET country_name = ?, iso2 = ?, iso3 = ?, bacen_code = ?, country_dial_code = ?, "
                                + "nationality = ?, active = coalesce(?, active), updated_by = ?, updated_at = now() WHERE id = ?",
                        r.name().trim(), r.iso2().trim().toUpperCase(Locale.ROOT), up(r.iso3()), r.bacenCode(), blank(r.dialCode()),
                        blank(r.nationality()), r.active(), userId, id);
            }
        } catch (DuplicateKeyException e) {
            throw PersonException.conflict("COUNTRY_EXISTS", "Já existe um país com esta sigla ou código BACEN.");
        }
        Integer fid = id;
        return countries(false).stream().filter(c -> c.id().equals(fid)).findFirst().orElseThrow();
    }

    @Transactional
    public State saveState(Integer id, StateRequest r, UUID userId) {
        if (r.countryId() == null || r.name() == null || r.name().isBlank() || r.uf() == null || r.uf().isBlank()) {
            throw PersonException.badRequest("Informe país, nome e sigla.");
        }
        if (r.countryId() == 1 && r.ibgeCode() == null) throw PersonException.badRequest("Código IBGE obrigatório para o Brasil.");
        try {
            if (id == null) {
                id = jdbc.queryForObject("INSERT INTO states (country_id, state_name, uf, ibge_code, region, created_by) "
                                + "VALUES (?,?,?,?,?,?) RETURNING id", Integer.class, r.countryId(), r.name().trim(),
                        r.uf().trim().toUpperCase(Locale.ROOT), r.ibgeCode(), blank(r.region()), userId);
            } else {
                jdbc.update("UPDATE states SET country_id = ?, state_name = ?, uf = ?, ibge_code = ?, region = ?, "
                                + "active = coalesce(?, active), updated_by = ?, updated_at = now() WHERE id = ?",
                        r.countryId(), r.name().trim(), r.uf().trim().toUpperCase(Locale.ROOT), r.ibgeCode(), blank(r.region()),
                        r.active(), userId, id);
            }
        } catch (DuplicateKeyException e) {
            throw PersonException.conflict("STATE_EXISTS", "Já existe um estado com esta sigla ou código IBGE.");
        }
        Integer fid = id;
        return states(r.countryId(), false).stream().filter(s -> s.id().equals(fid)).findFirst().orElseThrow();
    }

    @Transactional
    public City saveCity(Integer id, CityRequest r, UUID userId) {
        if (r.stateId() == null || r.name() == null || r.name().isBlank()) throw PersonException.badRequest("Informe estado e nome.");
        Integer country = jdbc.queryForObject("SELECT country_id FROM states WHERE id = ?", Integer.class, r.stateId());
        if (Integer.valueOf(1).equals(country) && (r.ibgeCode() == null || r.ibgeCode() < 1000000 || r.ibgeCode() > 9999999)) {
            throw PersonException.badRequest("Código IBGE de 7 dígitos obrigatório para o Brasil.");
        }
        try {
            if (id == null) {
                id = jdbc.queryForObject("INSERT INTO cities (state_id, city_name, ibge_code, area_code, is_capital, created_by) "
                                + "VALUES (?,?,?,?,?,?) RETURNING id", Integer.class, r.stateId(), r.name().trim(), r.ibgeCode(),
                        blank(r.areaCode()), Boolean.TRUE.equals(r.capital()), userId);
            } else {
                jdbc.update("UPDATE cities SET state_id = ?, city_name = ?, ibge_code = ?, area_code = ?, is_capital = coalesce(?, is_capital), "
                                + "active = coalesce(?, active), updated_by = ?, updated_at = now() WHERE id = ?",
                        r.stateId(), r.name().trim(), r.ibgeCode(), blank(r.areaCode()), r.capital(), r.active(), userId, id);
            }
        } catch (DuplicateKeyException e) {
            throw PersonException.conflict("CITY_EXISTS", "Já existe uma cidade com este código IBGE.");
        }
        Integer fid = id;
        return jdbc.query("SELECT c.*, s.uf FROM cities c JOIN states s ON s.id = c.state_id WHERE c.id = ?", (rs, i) -> city(rs), fid).get(0);
    }

    @Transactional
    public Neighborhood saveNeighborhood(UUID id, NeighborhoodRequest r, UUID userId) {
        if (r.cityId() == null || r.name() == null || r.name().isBlank()) throw PersonException.badRequest("Informe cidade e nome.");
        String name = r.name().trim().replaceAll("\\s+", " ");
        try {
            if (id == null) {
                id = jdbc.queryForObject("INSERT INTO neighborhoods (city_id, neighborhood_name, normalized_name, source, created_by) "
                        + "VALUES (?, ?, left(app.normalize_name(?), 100), 'M', ?) RETURNING id", UUID.class, r.cityId(), name, name, userId);
            } else {
                jdbc.update("UPDATE neighborhoods SET city_id = ?, neighborhood_name = ?, normalized_name = left(app.normalize_name(?), 100), "
                                + "active = coalesce(?, active), updated_by = ?, updated_at = now() WHERE id = ?",
                        r.cityId(), name, name, r.active(), userId, id);
            }
        } catch (DuplicateKeyException e) {
            throw PersonException.conflict("NEIGHBORHOOD_EXISTS", "Este bairro já está cadastrado nesta cidade.");
        }
        UUID fid = id;
        return jdbc.query("SELECT * FROM neighborhoods WHERE id = ?", (rs, i) -> new Neighborhood(rs.getObject("id", UUID.class),
                rs.getInt("city_id"), rs.getString("neighborhood_name"), rs.getString("source"), rs.getBoolean("active")), fid).get(0);
    }

    // ---------------------------------------------------------------- CEP

    @Transactional
    public PostalCodeResult lookupPostalCode(String raw) {
        String cep = raw == null ? "" : raw.replaceAll("\\D", "");
        if (cep.length() != 8) throw PersonException.badRequest("CEP inválido: informe 8 dígitos.");
        ViaCep v;
        try {
            v = http.get().uri("/ws/{cep}/json/", cep).retrieve().body(ViaCep.class);
        } catch (Exception e) {
            log.warn("[CEP] consulta indisponível: {}", e.getMessage());
            throw new PersonException(HttpStatus.SERVICE_UNAVAILABLE, "POSTAL_CODE_UNAVAILABLE",
                    "Serviço de CEP indisponível. Preencha o endereço manualmente.");
        }
        if (v == null || Boolean.TRUE.equals(v.erro())) {
            throw new PersonException(HttpStatus.NOT_FOUND, "POSTAL_CODE_NOT_FOUND", "CEP não encontrado.");
        }
        Integer cityId = null;
        String cityName = v.localidade();
        String uf = v.uf();
        Integer ibge = null;
        if (v.ibge() != null && v.ibge().matches("\\d{7}")) {
            ibge = Integer.valueOf(v.ibge());
            List<Map<String, Object>> c = jdbc.queryForList("SELECT c.id, c.city_name, s.uf FROM cities c JOIN states s "
                    + "ON s.id = c.state_id WHERE c.ibge_code = ?", ibge);
            if (!c.isEmpty()) {
                cityId = (Integer) c.get(0).get("id");
                cityName = (String) c.get(0).get("city_name");
                uf = (String) c.get(0).get("uf");
            }
        }
        UUID nbId = null;
        String nb = v.bairro() == null || v.bairro().isBlank() ? null : v.bairro().trim();
        if (cityId != null && nb != null) {
            jdbc.update("INSERT INTO neighborhoods (city_id, neighborhood_name, normalized_name, source) "
                    + "VALUES (?, left(?, 100), left(app.normalize_name(?), 100), 'C') ON CONFLICT (city_id, normalized_name) DO NOTHING",
                    cityId, nb, nb);
            nbId = jdbc.queryForObject("SELECT id FROM neighborhoods WHERE city_id = ? AND normalized_name = left(app.normalize_name(?), 100)",
                    UUID.class, cityId, nb);
        }
        return new PostalCodeResult(cep, blank(v.logradouro()), blank(v.complemento()), nbId, nb, cityId, cityName, uf, ibge);
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String up(String s) {
        String b = blank(s);
        return b == null ? null : b.toUpperCase(Locale.ROOT);
    }
}
