package za.co.samrepa.repository;

import za.co.samrepa.entity.Customer;
import za.co.samrepa.entity.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// Mirrors TransactionService sentinel values — used when no date filter is specified


@DataJpaTest
@ActiveProfiles("test")
class TransactionRepositoryTest {

    private static final LocalDateTime ALL_TIME_FROM = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime ALL_TIME_TO   = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    @Autowired private TransactionRepository transactionRepository;
    @Autowired private CustomerRepository    customerRepository;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setCustomerNumber("CUST-R01");
        customer.setFullName("Thabo Nkosi");
        customer = customerRepository.save(customer);
    }

    private Transaction saveTransaction(String externalId, String source,
                                        String merchant, BigDecimal amount,
                                        String category, LocalDateTime date) {
        var t = new Transaction();
        t.setExternalTransactionId(externalId);
        t.setSourceSystem(source);
        t.setCustomer(customer);
        t.setMerchant(merchant);
        t.setAmount(amount);
        t.setCurrency("ZAR");
        t.setTransactionDate(date);
        t.setCategory(category);
        return transactionRepository.save(t);
    }

    @Test
    void existsByExternalTransactionIdAndSourceSystem_returnsTrueForDuplicate() {
        saveTransaction("EXT-001", "FNB", "Checkers Southgate",
                new BigDecimal("-300.00"), "GROCERIES", LocalDateTime.now());

        assertThat(transactionRepository
                .existsByExternalTransactionIdAndSourceSystem("EXT-001", "FNB")).isTrue();
        assertThat(transactionRepository
                .existsByExternalTransactionIdAndSourceSystem("EXT-001", "ABSA")).isFalse();
        assertThat(transactionRepository
                .existsByExternalTransactionIdAndSourceSystem("EXT-999", "FNB")).isFalse();
    }

    @Test
    void findByCustomer_filtersCorrectly_byCategory() {
        var now = LocalDateTime.now();
        saveTransaction("EXT-C1", "FNB",  "Checkers Southgate", new BigDecimal("-400.00"), "GROCERIES", now);
        saveTransaction("EXT-C2", "ABSA", "Netflix",            new BigDecimal("-199.00"), "ENTERTAINMENT", now);

        var page = transactionRepository.findByCustomer(
                customer.getId(), ALL_TIME_FROM, ALL_TIME_TO, "GROCERIES", PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getMerchant()).isEqualTo("Checkers Southgate");
    }

    @Test
    void findByCustomer_filtersCorrectly_byDateRange() {
        var past   = LocalDateTime.now().minusDays(10);
        var recent = LocalDateTime.now().minusDays(1);
        saveTransaction("EXT-D1", "FNB", "Pick n Pay Bara", new BigDecimal("-200.00"), "GROCERIES", past);
        saveTransaction("EXT-D2", "FNB", "Shell Soweto",    new BigDecimal("-800.00"), "FUEL",      recent);

        var from = LocalDateTime.now().minusDays(5);
        var to   = LocalDateTime.now();

        var page = transactionRepository.findByCustomer(
                customer.getId(), from, to, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getMerchant()).isEqualTo("Shell Soweto");
    }

    @Test
    void getCategoryTotals_aggregatesCorrectly() {
        var now = LocalDateTime.now();
        saveTransaction("EXT-G1", "FNB",  "Checkers Southgate", new BigDecimal("-420.00"), "GROCERIES", now);
        saveTransaction("EXT-G2", "ABSA", "Pick n Pay Bara",    new BigDecimal("-380.00"), "GROCERIES", now);

        var totals = transactionRepository.getCategoryTotals(customer.getId(), ALL_TIME_FROM, ALL_TIME_TO);

        assertThat(totals).hasSize(1);
        assertThat(totals.get(0).getCategory()).isEqualTo("GROCERIES");
        assertThat(totals.get(0).getTransactionCount()).isEqualTo(2L);
        assertThat(totals.get(0).getTotalAmount()).isEqualByComparingTo("-800.00");
    }

    @Test
    void sumSpendBetween_sumsNegativeAmounts() {
        var now = LocalDateTime.now();
        saveTransaction("EXT-S1", "FNB",  "Checkers Southgate", new BigDecimal("-300.00"), "GROCERIES", now);
        saveTransaction("EXT-S2", "ABSA", "Shell Soweto",       new BigDecimal("-500.00"), "FUEL",      now);
        saveTransaction("EXT-S3", "FNB",  "SALARY CREDIT",      new BigDecimal("28500.00"), null,       now);

        var from  = now.minusDays(1);
        var to    = now.plusDays(1);
        var total = transactionRepository.sumSpendBetween(customer.getId(), from, to);

        assertThat(total).isEqualByComparingTo("-800.00");
    }

    @Test
    void sumIncomeBetween_sumsPositiveAmounts() {
        var now = LocalDateTime.now();
        saveTransaction("EXT-I1", "FNB",  "SALARY CREDIT",      new BigDecimal("28500.00"), null,         now);
        saveTransaction("EXT-I2", "ABSA", "Checkers Southgate", new BigDecimal("-200.00"),  "GROCERIES",  now);

        var from  = now.minusDays(1);
        var to    = now.plusDays(1);
        var total = transactionRepository.sumIncomeBetween(customer.getId(), from, to);

        assertThat(total).isEqualByComparingTo("28500.00");
    }

    @Test
    void findByCustomer_returnsAll_whenNoFilters() {
        var now = LocalDateTime.now();
        saveTransaction("EXT-A1", "FNB",     "Shoprite Jabulani", new BigDecimal("-100.00"), "GROCERIES", now);
        saveTransaction("EXT-A2", "ABSA",    "Engen Booysens",    new BigDecimal("-200.00"), "FUEL",      now);
        saveTransaction("EXT-A3", "CAPITEC", "Nando's Bara",      new BigDecimal("-165.00"), "DINING",    now);

        var page = transactionRepository.findByCustomer(
                customer.getId(), ALL_TIME_FROM, ALL_TIME_TO, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(3);
    }
}
