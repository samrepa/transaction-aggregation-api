package za.co.samrepa.delegate;

import za.co.samrepa.api.CustomersApiDelegate;
import za.co.samrepa.api.model.*;
import za.co.samrepa.mapper.TransactionMapper;
import za.co.samrepa.repository.AccountRepository;
import za.co.samrepa.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;

@Component
@RequiredArgsConstructor
public class CustomerApiDelegateImpl implements CustomersApiDelegate {

    private final TransactionService transactionService;
    private final TransactionMapper  transactionMapper;
    private final AccountRepository  accountRepository;

    @Override
    public ResponseEntity<CustomerSummary> getCustomerSummary(String customerId) {
        var customer = transactionService.getCustomer(customerId);
        var now      = YearMonth.now();
        var spend    = transactionService.getMonthlySpendById(customer.getId(), now);
        var income   = transactionService.getMonthlyIncomeById(customer.getId(), now);
        var accounts = accountRepository.countByCustomerId(customer.getId());

        var summary = new CustomerSummary();
        summary.setCustomerId(customerId);
        summary.setTotalAccounts(accounts);
        summary.setMonthlySpend(spend);
        summary.setMonthlyIncome(income);

        return ResponseEntity.ok(summary);
    }

    @Override
    public ResponseEntity<TransactionPage> getTransactions(
            String customerId, LocalDate from, LocalDate to, String category, Integer page, Integer size) {

        var result = transactionService.getTransactions(
                customerId, from, to, category,
                page != null ? page : 0,
                size != null ? size : 20);

        var response = new TransactionPage();
        response.setContent(result.getContent().stream().map(transactionMapper::toItem).toList());
        response.setPage(result.getNumber());
        response.setSize(result.getSize());
        response.setTotalElements(result.getTotalElements());
        response.setTotalPages(result.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<CategoryBreakdown> getCategoryBreakdown(
            String customerId, LocalDate from, LocalDate to) {

        var totals = transactionService.getCategoryTotals(customerId, from, to);

        var response = new CategoryBreakdown();
        response.setCustomerId(customerId);
        response.setFrom(from);
        response.setTo(to);
        response.setCategories(totals.stream().map(transactionMapper::toCategoryTotal).toList());

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<MonthlySpend> getMonthlySpend(String customerId, String month) {
        var yearMonth = YearMonth.parse(month);
        var amount    = transactionService.getMonthlySpend(customerId, yearMonth);

        var response = new MonthlySpend();
        response.setCustomerId(customerId);
        response.setMonth(month);
        response.setAmount(amount);

        return ResponseEntity.ok(response);
    }
}
