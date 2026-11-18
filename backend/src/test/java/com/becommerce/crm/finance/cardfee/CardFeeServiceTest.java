package com.becommerce.crm.finance.cardfee;

import com.becommerce.crm.finance.cardfee.CardFeeDtos.RateItem;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class CardFeeServiceTest {

    private final CardFeeRateJpaRepository repository = mock(CardFeeRateJpaRepository.class);
    private final CardFeeService service = new CardFeeService(repository);
    private final UUID company = UUID.randomUUID();

    private static List<RateItem> twelve() {
        return new ArrayList<>(IntStream.rangeClosed(1, 12)
                .mapToObj(n -> new RateItem(n, new BigDecimal("3.15").add(BigDecimal.valueOf(n))))
                .toList());
    }

    @Test
    void savesTwelveRatesSortedReplacingPrevious() {
        List<RateItem> rates = twelve();
        java.util.Collections.reverse(rates);

        List<RateItem> saved = service.save(company, rates);

        assertThat(saved).extracting(RateItem::installments).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        verify(repository).deleteAllOfCompany(company);
        verify(repository).saveAll(anyList());
    }

    @Test
    void rejectsLessThanTwelve() {
        List<RateItem> rates = twelve();
        rates.remove(0);
        assertBad(rates);
    }

    @Test
    void rejectsInstallmentOutOfRange() {
        List<RateItem> rates = twelve();
        rates.set(0, new RateItem(13, BigDecimal.ONE));
        assertBad(rates);
    }

    @Test
    void rejectsRepeatedInstallment() {
        List<RateItem> rates = twelve();
        rates.set(0, new RateItem(2, BigDecimal.ONE));
        assertBad(rates);
    }

    @Test
    void rejectsRateOfHundredOrNegative() {
        List<RateItem> hundred = twelve();
        hundred.set(0, new RateItem(1, new BigDecimal("100")));
        assertBad(hundred);

        List<RateItem> negative = twelve();
        negative.set(0, new RateItem(1, new BigDecimal("-0.01")));
        assertBad(negative);
    }

    @Test
    void rejectsMoreThanTwoDecimals() {
        List<RateItem> rates = twelve();
        rates.set(0, new RateItem(1, new BigDecimal("3.155")));
        assertBad(rates);
    }

    @Test
    void acceptsZeroAndTrailingZeros() {
        List<RateItem> rates = twelve();
        rates.set(0, new RateItem(1, new BigDecimal("0.000")));
        assertThat(service.save(company, rates)).hasSize(12);
    }

    private void assertBad(List<RateItem> rates) {
        assertThatThrownBy(() -> service.save(company, rates))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
        verifyNoInteractions(repository);
    }
}
