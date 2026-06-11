package za.co.samrepa.api;

import za.co.samrepa.delegate.CustomerApiDelegateImpl;
import za.co.samrepa.entity.Customer;
import za.co.samrepa.entity.Transaction;
import za.co.samrepa.exception.CustomerNotFoundException;
import za.co.samrepa.mapper.TransactionMapper;
import za.co.samrepa.repository.AccountRepository;
import za.co.samrepa.repository.TransactionRepository;
import za.co.samrepa.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import(CustomerApiDelegateImpl.class)
class CustomerApiTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean TransactionService  transactionService;
    @MockitoBean TransactionMapper   transactionMapper;
    @MockitoBean AccountRepository   accountRepository;
    @MockitoBean JwtDecoder          jwtDecoder;

    private Customer buildCustomer(String number) {
        var c = new Customer();
        c.setId(UUID.randomUUID());
        c.setCustomerNumber(number);
        c.setFullName("Sipho Mthembu");
        return c;
    }

    private Transaction buildTransaction(Customer customer) {
        var t = new Transaction();
        t.setId(UUID.randomUUID());
        t.setMerchant("Checkers Southgate");
        t.setAmount(new BigDecimal("-420.00"));
        t.setCurrency("ZAR");
        t.setTransactionDate(LocalDateTime.now());
        t.setCategory("GROCERIES");
        t.setSourceSystem("FNB");
        t.setCustomer(customer);
        return t;
    }

    // ─── /summary ─────────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void getCustomerSummary_returns200_whenCustomerExists() throws Exception {
        var customer = buildCustomer("CUST-001");
        when(transactionService.getCustomer("CUST-001")).thenReturn(customer);
        when(transactionService.getMonthlySpendById(eq(customer.getId()), any(YearMonth.class)))
                .thenReturn(new BigDecimal("5200.00"));
        when(transactionService.getMonthlyIncomeById(eq(customer.getId()), any(YearMonth.class)))
                .thenReturn(new BigDecimal("28500.00"));
        when(accountRepository.countByCustomerId(customer.getId())).thenReturn(2);

        mockMvc.perform(get("/customers/CUST-001/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("CUST-001"))
                .andExpect(jsonPath("$.totalAccounts").value(2))
                .andExpect(jsonPath("$.monthlySpend").value(5200.00))
                .andExpect(jsonPath("$.monthlyIncome").value(28500.00));
    }

    @Test
    @WithMockUser
    void getCustomerSummary_returns404_whenCustomerNotFound() throws Exception {
        when(transactionService.getCustomer("CUST-MISSING"))
                .thenThrow(new CustomerNotFoundException("CUST-MISSING"));

        mockMvc.perform(get("/customers/CUST-MISSING/summary"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCustomerSummary_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/customers/CUST-001/summary"))
                .andExpect(status().isUnauthorized());
    }

    // ─── /transactions ────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void getTransactions_returns200_withPaginatedResults() throws Exception {
        var customer = buildCustomer("CUST-001");
        var txn      = buildTransaction(customer);

        when(transactionService.getTransactions(
                eq("CUST-001"), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(txn)));

        var item = new za.co.samrepa.api.model.TransactionItem();
        item.setMerchant("Checkers Southgate");
        item.setAmount(new BigDecimal("420.00"));
        item.setCurrency("ZAR");
        when(transactionMapper.toItem(any())).thenReturn(item);

        mockMvc.perform(get("/customers/CUST-001/transactions")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].merchant").value("Checkers Southgate"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser
    void getTransactions_returns200_withCategoryFilter() throws Exception {
        when(transactionService.getTransactions(
                eq("CUST-001"), any(), any(), eq("GROCERIES"), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/customers/CUST-001/transactions")
                        .param("category", "GROCERIES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ─── /categories ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void getCategoryBreakdown_returns200() throws Exception {
        var projMock = Mockito.mock(TransactionRepository.CategoryTotals.class);
        when(projMock.getCategory()).thenReturn("GROCERIES");
        when(projMock.getTransactionCount()).thenReturn(3L);
        when(projMock.getTotalAmount()).thenReturn(new BigDecimal("-840.00"));

        when(transactionService.getCategoryTotals(eq("CUST-001"), any(), any()))
                .thenReturn(List.of(projMock));

        var catTotal = new za.co.samrepa.api.model.CategoryTotal();
        catTotal.setCategory("GROCERIES");
        catTotal.setAmount(new BigDecimal("840.00"));
        catTotal.setTransactionCount(3);
        when(transactionMapper.toCategoryTotal(any())).thenReturn(catTotal);

        mockMvc.perform(get("/customers/CUST-001/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("CUST-001"))
                .andExpect(jsonPath("$.categories[0].category").value("GROCERIES"));
    }

    // ─── /monthly-spend ───────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void getMonthlySpend_returns200() throws Exception {
        when(transactionService.getMonthlySpend(eq("CUST-001"), eq(YearMonth.of(2026, 6))))
                .thenReturn(new BigDecimal("5200.00"));

        mockMvc.perform(get("/customers/CUST-001/monthly-spend")
                        .param("month", "2026-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("CUST-001"))
                .andExpect(jsonPath("$.month").value("2026-06"))
                .andExpect(jsonPath("$.amount").value(5200.00));
    }

    @Test
    @WithMockUser
    void getMonthlySpend_returns400_whenMonthMissing() throws Exception {
        mockMvc.perform(get("/customers/CUST-001/monthly-spend"))
                .andExpect(status().isBadRequest());
    }
}
