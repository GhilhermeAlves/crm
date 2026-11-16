package com.becommerce.crm.finance.cardfee;

import com.becommerce.crm.finance.cardfee.CardFeeDtos.PresetRequest;
import com.becommerce.crm.finance.cardfee.CardFeeDtos.PresetResponse;
import com.becommerce.crm.finance.cardfee.CardFeeDtos.RateItem;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CardFeePresetServiceTest {

    private final CardFeePresetJpaRepository repository = mock(CardFeePresetJpaRepository.class);
    private final CardFeePresetService service = new CardFeePresetService(repository);

    private static List<RateItem> twelve() {
        return IntStream.rangeClosed(1, 12).mapToObj(n -> new RateItem(n, BigDecimal.valueOf(n))).toList();
    }

    @Test
    void createsWithTrimmedNameAndSortedRates() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        PresetResponse r = service.create(new PresetRequest("  Ton   na hora ", true,
                LocalDate.of(2026, 11, 13), " ", null, twelve().reversed()));

        assertThat(r.name()).isEqualTo("Ton na hora");
        assertThat(r.estimated()).isTrue();
        assertThat(r.active()).isTrue();
        assertThat(r.sourceNote()).isNull();
        assertThat(r.rates()).extracting(RateItem::installments).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
    }

    @Test
    void rejectsDuplicateName() {
        when(repository.existsByNameIgnoreCaseAndIdNot(anyString(), any())).thenReturn(true);
        assertThatThrownBy(() -> service.create(new PresetRequest("Ton", false, null, null, true, twelve())))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> service.create(new PresetRequest(" ", false, null, null, true, twelve())))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
    }

    @Test
    void deactivateMarksInactive() {
        CardFeePresetJpaEntity e = new CardFeePresetJpaEntity("Ton");
        when(repository.findById(e.getId())).thenReturn(Optional.of(e));
        service.deactivate(e.getId());
        assertThat(e.isActive()).isFalse();
        verify(repository).save(e);
    }

    @Test
    void updateUnknownIs404() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(id, new PresetRequest("Ton", false, null, null, true, twelve())))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
    }
}
