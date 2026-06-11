package za.co.samrepa.connector;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Mock Capitec connector.
 * In production this would call Capitec's Open Banking API.
 */
@Component
public class CapitecConnector implements SourceConnector {

    @Override
    public String sourceName() { return "CAPITEC"; }

    @Override
    public List<RawTransaction> fetchTransactions(String customerId) {
        return List.of(
                new RawTransaction("CAP-001", "Pick n Pay Bara",    new BigDecimal("-410.00"),  "ZAR", LocalDateTime.now().minusDays(1)),
                new RawTransaction("CAP-002", "Engen Soweto",       new BigDecimal("-480.00"),  "ZAR", LocalDateTime.now().minusDays(2)),
                new RawTransaction("CAP-003", "DStv Subscription",  new BigDecimal("-849.00"),  "ZAR", LocalDateTime.now().minusDays(3)),
                new RawTransaction("CAP-004", "Steers Jabulani",    new BigDecimal("-165.00"),  "ZAR", LocalDateTime.now().minusDays(4)),
                new RawTransaction("CAP-005", "FREELANCE PAYMENT",  new BigDecimal("5000.00"),  "ZAR", LocalDateTime.now().minusDays(8))
        );
    }
}
