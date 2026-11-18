package com.becommerce.crm.finance.cardfee;

import com.becommerce.crm.finance.cardfee.CardFeeDtos.PresetRequest;
import com.becommerce.crm.finance.cardfee.CardFeeDtos.PresetResponse;
import com.becommerce.crm.finance.cardfee.CardFeeDtos.RateItem;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/** Modelos de taxa das maquininhas (globais). A guarda de CRM_ADMIN fica no controller. */
@Service
public class CardFeePresetService {

    private final CardFeePresetJpaRepository repository;

    public CardFeePresetService(CardFeePresetJpaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PresetResponse> listActive() {
        return repository.findByActiveTrueOrderByNameAsc().stream().map(CardFeePresetService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PresetResponse> listAll() {
        return repository.findAllByOrderByNameAsc().stream().map(CardFeePresetService::toResponse).toList();
    }

    @Transactional
    public PresetResponse create(PresetRequest request) {
        CardFeePresetJpaEntity entity = new CardFeePresetJpaEntity(requireName(request));
        apply(entity, request);
        return toResponse(repository.save(entity));
    }

    @Transactional
    public PresetResponse update(UUID id, PresetRequest request) {
        CardFeePresetJpaEntity entity = find(id);
        apply(entity, request);
        return toResponse(repository.save(entity));
    }

    @Transactional
    public void deactivate(UUID id) {
        CardFeePresetJpaEntity entity = find(id);
        entity.deactivate();
        repository.save(entity);
    }

    private void apply(CardFeePresetJpaEntity entity, PresetRequest request) {
        String name = requireName(request);
        if (repository.existsByNameIgnoreCaseAndIdNot(name, entity.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um modelo com esse nome.");
        }
        List<RateItem> rates = CardFeeDtos.validate(request.rates());
        String note = request.sourceNote() == null || request.sourceNote().isBlank()
                ? null : request.sourceNote().trim();
        if (note != null && note.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Observação com no máximo 500 caracteres.");
        }
        entity.update(name, Boolean.TRUE.equals(request.estimated()), request.checkedAt(), note,
                request.active() == null || request.active(),
                rates.stream().map(r -> new CardFeePresetJpaEntity.Rate(r.installments(), r.ratePercent())).toList());
    }

    private static String requireName(PresetRequest request) {
        String name = request == null || request.name() == null ? "" : request.name().trim().replaceAll("\\s+", " ");
        if (name.isEmpty() || name.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um nome de até 120 caracteres.");
        }
        return name;
    }

    private CardFeePresetJpaEntity find(UUID id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Modelo de taxa não encontrado."));
    }

    static PresetResponse toResponse(CardFeePresetJpaEntity e) {
        return new PresetResponse(e.getId(), e.getName(), e.isEstimated(), e.getCheckedAt(), e.getSourceNote(),
                e.isActive(), e.getRates().stream().map(r -> new RateItem(r.getInstallments(), r.getRatePercent())).toList());
    }
}
