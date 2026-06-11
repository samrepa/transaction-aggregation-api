package za.co.samrepa.service;

import za.co.samrepa.entity.Customer;
import za.co.samrepa.entity.Transaction;
import za.co.samrepa.exception.CustomerNotFoundException;
import za.co.samrepa.repository.CustomerRepository;
import za.co.samrepa.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private CustomerRepository    customerRepository;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(transactionRepository, customerRepository);
    }

    private Customer buildCustomer(String number) {
        var c = new Customer();
        c.setId(UUID.randomUUID());
        c.setCustomerNumber(number);
        c.setFullName("Naledi Dlamini");
        return c;
    }

    private Transaction buildTransaction(Customer customer, String merchant, BigDecimal amount) {
        var t = new Transaction();
        t.setId(UUID.randomUUID());
        t.setCustomer(customer);
        t.setMerchant(merchant);
        t.setAmount(amount);
        t.setCurrency("ZAR");
        t.setTransactionDate(LocalDateTime.now());
        t.setCategory("GROCERIES");
        return t;
    }

    @Test
    void getCustomer_returnsCustomer_whenExists() {
        var customer = buildCustomer("CUST-001");
        when(customerRepository.findByCustomerNumber("CUST-001")).thenReturn(Optional.of(customer));

        assertThat(transactionService.getCustomer("CUST-001").getCustomerNumber()).isEqualTo("CUST-001");
    }

    @Test
    void getCustomer_throwsNotFoundException_whenMissing() {
        when(customerRepository.findByCustomerNumber("CUST-XXX")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getCustomer("CUST-XXX"))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("CUST-XXX");
    }

    @Test
    void getTransactions_returnsPage_withFilters() {
        var customer = buildCustomer("CUST-001");
        var txn = buildTransaction(customer, "Checkers Southgate", new BigDecimal("-420.00"));

        when(customerRepository.findByCustomerNumber("CUST-001")).thenReturn(Optional.of(customer));
        when(transactionRepository.findByCustomer(
                eq(customer.getId()), any(), any(), eq("GROCERIES"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(txn)));

        var page = transactionService.getTransactions(
                "CUST-001", LocalDate.now().minusDays(7), LocalDate.now(), "GROCERIES", 0, 20);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getMerchant()).isEqualTo("Checkers Southgate");
    }

    @Test
    void getTransactions_withNullFilters_stillWorks() {
        var customer = buildCustomer("CUST-001");
        when(customerRepository.findByCustomerNumber("CUST-001")).thenReturn(Optional.of(customer));
        when(transactionRepository.findByCustomer(
                eq(customer.getId()), any(), any(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(transactionService.getTransactions("CUST-001", null, null, null, 0, 20).getContent()).isEmpty();
    }

    @Test
    void getMonthlySpendById_returnsAbsoluteValue() {
        var customerId = UUID.randomUUID();
        when(transactionRepository.sumSpendBetween(eq(customerId), any(), any()))
                .thenReturn(new BigDecimal("-5200.00"));

        assertThat(transactionService.getMonthlySpendById(customerId, YearMonth.of(2026, 6)))
                .isEqualByComparingTo("5200.00");
    }

    @Test
    void getMonthlySpendById_returnsZero_whenNoTransactions() {
        var customerId = UUID.randomUUID();
        when(transactionRepository.sumSpendBetween(eq(customerId), any(), any())).thenReturn(null);

        assertThat(transactionService.getMonthlySpendById(customerId, YearMonth.of(2026, 6)))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getMonthlyIncomeById_returnsPositiveAmount() {
        var customerId = UUID.randomUUID();
        when(transactionRepository.sumIncomeBetween(eq(customerId), any(), any()))
                .thenReturn(new BigDecimal("28500.00"));

        assertThat(transactionService.getMonthlyIncomeById(customerId, YearMonth.of(2026, 6)))
                .isEqualByComparingTo("28500.00");
    }

    @Test
    void getCategoryTotals_returnsList() {
        var customer = buildCustomer("CUST-001");
        TransactionRepository.CategoryTotals totals = new TransactionRepository.CategoryTotals() {
            public String getCategory() { return "GROCERIES"; }
            public Long getTransactionCount() { return 4L; }
            public BigDecimal getTotalAmount() { return new BigDecimal("-1140.50"); }
        };

        when(customerRepository.findByCustomerNumber("CUST-001")).thenReturn(Optional.of(customer));
        when(transactionRepository.getCategoryTotals(eq(customer.getId()), any(), any()))
                .thenReturn(List.of(totals));

        var result = transactionService.getCategoryTotals("CUST-001", null, null);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo("GROCERIES");
    }
}
