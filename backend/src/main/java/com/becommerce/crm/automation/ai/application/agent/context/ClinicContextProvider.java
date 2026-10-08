package com.becommerce.crm.automation.ai.application.agent.context;

import com.becommerce.crm.masterdata.company.application.port.output.CompanyRepository;
import com.becommerce.crm.masterdata.company.application.port.output.CompanySettingsRepository;
import com.becommerce.crm.masterdata.company.domain.Company;
import com.becommerce.crm.masterdata.company.domain.CompanySettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Carrega o contexto da clínica do CRM (empresa + configurações). Falhas de
 * leitura degradam para o mínimo (data/hora no fuso padrão) — nunca impedem a
 * resposta.
 */
@Component
public class ClinicContextProvider {

    private static final Logger log = LoggerFactory.getLogger(ClinicContextProvider.class);

    static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Sao_Paulo");
    /** Horário de funcionamento é texto livre; limitado para não inflar tokens. */
    static final int MAX_BUSINESS_HOURS_CHARS = 300;

    private final CompanyRepository companyRepository;
    private final CompanySettingsRepository settingsRepository;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public ClinicContextProvider(CompanyRepository companyRepository, CompanySettingsRepository settingsRepository) {
        this(companyRepository, settingsRepository, Clock.systemUTC());
    }

    ClinicContextProvider(CompanyRepository companyRepository, CompanySettingsRepository settingsRepository,
                          Clock clock) {
        this.companyRepository = companyRepository;
        this.settingsRepository = settingsRepository;
        this.clock = clock;
    }

    /** Sem CRM (fallback/testes): só data/hora no fuso padrão. */
    public static ClinicContextProvider none() {
        return new ClinicContextProvider(null, null, Clock.systemUTC());
    }

    public ClinicContext load(UUID companyId) {
        if (companyRepository == null || settingsRepository == null) {
            return new ClinicContext(null, null, null, null, DEFAULT_ZONE, ZonedDateTime.now(clock.withZone(DEFAULT_ZONE)));
        }
        Optional<Company> company = safe(() -> companyRepository.findById(companyId), companyId);
        Optional<CompanySettings> settings = safe(() -> settingsRepository.findByCompanyId(companyId), companyId);
        ZoneId zone = settings.map(CompanySettings::getTimezone).map(ClinicContextProvider::zone).orElse(DEFAULT_ZONE);
        String hours = settings.map(CompanySettings::getBusinessHours).filter(h -> !h.isBlank())
                .map(h -> h.length() > MAX_BUSINESS_HOURS_CHARS ? h.substring(0, MAX_BUSINESS_HOURS_CHARS) : h)
                .orElse(null);
        return new ClinicContext(
                company.map(c -> firstNonBlank(c.getTradingName(), c.getLegalName())).orElse(null),
                company.map(Company::getPhone).filter(p -> !p.isBlank()).orElse(null),
                company.map(ClinicContextProvider::address).orElse(null),
                hours, zone, ZonedDateTime.now(clock.withZone(zone)));
    }

    private static String address(Company c) {
        String street = Stream.of(c.getAddressStreet(), c.getAddressNumber())
                .filter(s -> s != null && !s.isBlank()).collect(Collectors.joining(", "));
        String joined = Stream.of(street, c.getAddressNeighborhood(), cityState(c))
                .filter(s -> s != null && !s.isBlank()).collect(Collectors.joining(" - "));
        return joined.isBlank() ? null : joined;
    }

    private static String cityState(Company c) {
        if (c.getAddressCity() == null || c.getAddressCity().isBlank()) {
            return null;
        }
        return c.getAddressState() == null || c.getAddressState().isBlank()
                ? c.getAddressCity() : c.getAddressCity() + "/" + c.getAddressState();
    }

    private static ZoneId zone(String id) {
        try {
            return ZoneId.of(id);
        } catch (DateTimeException e) {
            return DEFAULT_ZONE;
        }
    }

    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : (b != null && !b.isBlank() ? b : null);
    }

    private static <T> Optional<T> safe(java.util.function.Supplier<Optional<T>> query, UUID companyId) {
        try {
            return query.get();
        } catch (RuntimeException e) {
            log.warn("Contexto da clínica parcial (company={}): {}", companyId, e.getMessage());
            return Optional.empty();
        }
    }
}
