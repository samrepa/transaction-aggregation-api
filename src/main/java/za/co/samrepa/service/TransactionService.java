package za.co.samrepa.service;

import za.co.samrepa.entity.Customer;
import za.co.samrepa.entity.Transaction;
import za.co.samrepa.exception.CustomerNotFoundException;
import za.co.samrepa.repository.CustomerRepository;
import za.co.samrepa.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private static final LocalDateTime BEGINNING_OF_TIME = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime END_OF_TIME       = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    private final TransactionRepository transactionRepository;
    private final CustomerRepository    customerRepository;

    public Customer getCustomer(String customerNumber) {
        return customerRepository.findByCustomerNumber(customerNumber)
                .orElseThrow(() -> new CustomerNotFoundException(customerNumber));
    }

    public Page<Transaction> getTransactions(
            String customerNumber, LocalDate from, LocalDate to, String category, int page, int size) {

        var customer = getCustomer(customerNumber);
        var fromDate = from != null ? from.atStartOfDay()    : BEGINNING_OF_TIME;
        var toDate   = to   != null ? to.atTime(LocalTime.MAX) : END_OF_TIME;
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate"));

        return transactionRepository.findByCustomer(customer.getId(), fromDate, toDate, category, pageable);
    }

    public List<TransactionRepository.CategoryTotals> getCategoryTotals(
            String customerNumber, LocalDate from, LocalDate to) {

        var customer = getCustomer(customerNumber);
        var fromDate = from != null ? from.atStartOfDay()    : BEGINNING_OF_TIME;
        var toDate   = to   != null ? to.atTime(LocalTime.MAX) : END_OF_TIME;

        return transactionRepository.getCategoryTotals(customer.getId(), fromDate, toDate);
    }

    /** Resolves customer number to UUID first — use when UUID is not yet known. */
    public BigDecimal getMonthlySpend(String customerNumber, YearMonth month) {
        return getMonthlySpendById(getCustomer(customerNumber).getId(), month);
    }

    /** Direct UUID overload — avoids an extra customer lookup when UUID is already known. */
    public BigDecimal getMonthlySpendById(UUID customerId, YearMonth month) {
        var from   = month.atDay(1).atStartOfDay();
        var to     = month.atEndOfMonth().atTime(LocalTime.MAX);
        var result = transactionRepository.sumSpendBetween(customerId, from, to);
        return result != null ? result.abs() : BigDecimal.ZERO;
    }

    /** Resolves customer number to UUID first — use when UUID is not yet known. */
    public BigDecimal getMonthlyIncome(String customerNumber, YearMonth month) {
        return getMonthlyIncomeById(getCustomer(customerNumber).getId(), month);
    }

    /** Direct UUID overload — avoids an extra customer lookup when UUID is already known. */
    public BigDecimal getMonthlyIncomeById(UUID customerId, YearMonth month) {
        var from   = month.atDay(1).atStartOfDay();
        var to     = month.atEndOfMonth().atTime(LocalTime.MAX);
        var result = transactionRepository.sumIncomeBetween(customerId, from, to);
        return result != null ? result : BigDecimal.ZERO;
    }
}
