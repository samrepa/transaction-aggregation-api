package za.co.samrepa.repository;

import za.co.samrepa.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    int countByCustomerId(UUID customerId);
}
