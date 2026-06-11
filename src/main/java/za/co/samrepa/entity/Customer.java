package za.co.samrepa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "customers")
@Getter @Setter
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_number", nullable = false, unique = true)
    private String customerNumber;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    private List<Account> accounts;
}
