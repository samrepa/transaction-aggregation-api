package za.co.samrepa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
// Category import removed — category is now a plain String column

@Entity
@Table(name = "transactions")
@Getter @Setter
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_transaction_id", nullable = false)
    private String externalTransactionId;

    @Column(name = "source_system", nullable = false)
    private String sourceSystem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column
    private String merchant;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(length = 50)
    private String category;
}
