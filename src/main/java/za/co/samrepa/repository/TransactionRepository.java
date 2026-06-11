package za.co.samrepa.repository;

import za.co.samrepa.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    boolean existsByExternalTransactionIdAndSourceSystem(String externalTransactionId, String sourceSystem);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.customer.id = :customerId
              AND t.transactionDate >= :from
              AND t.transactionDate <= :to
              AND (:categoryName IS NULL OR t.category = :categoryName)
            """)
    Page<Transaction> findByCustomer(
            @Param("customerId")    UUID customerId,
            @Param("from")          LocalDateTime from,
            @Param("to")            LocalDateTime to,
            @Param("categoryName")  String categoryName,
            Pageable pageable
    );

    @Query("""
            SELECT t.category AS category,
                   COUNT(t)        AS transactionCount,
                   SUM(t.amount)   AS totalAmount
            FROM Transaction t
            WHERE t.customer.id = :customerId
              AND t.transactionDate >= :from
              AND t.transactionDate <= :to
              AND t.category IS NOT NULL
            GROUP BY t.category
            """)
    List<CategoryTotals> getCategoryTotals(
            @Param("customerId") UUID customerId,
            @Param("from")       LocalDateTime from,
            @Param("to")         LocalDateTime to
    );

    @Query("""
            SELECT SUM(t.amount) FROM Transaction t
            WHERE t.customer.id = :customerId
              AND t.transactionDate >= :from
              AND t.transactionDate < :to
              AND t.amount < 0
            """)
    BigDecimal sumSpendBetween(
            @Param("customerId") UUID customerId,
            @Param("from")       LocalDateTime from,
            @Param("to")         LocalDateTime to
    );

    @Query("""
            SELECT SUM(t.amount) FROM Transaction t
            WHERE t.customer.id = :customerId
              AND t.transactionDate >= :from
              AND t.transactionDate < :to
              AND t.amount > 0
            """)
    BigDecimal sumIncomeBetween(
            @Param("customerId") UUID customerId,
            @Param("from")       LocalDateTime from,
            @Param("to")         LocalDateTime to
    );

    interface CategoryTotals {
        String     getCategory();
        Long       getTransactionCount();
        BigDecimal getTotalAmount();
    }
}
