package za.co.samrepa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "accounts")
@Getter @Setter
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "account_type", nullable = false)
    private String accountType;
}
