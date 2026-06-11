package za.co.samrepa.connector;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Mock ABSA connector.
 * In production this would call ABSA's Open Banking API.
 */
@Component
public class AbsaConnector implements SourceConnector {

    @Override
    public String sourceName() { return "ABSA"; }

    @Override
    public List<RawTransaction> fetchTransactions(String customerId) {
        return List.of(
                new RawTransaction("ABSA-001", "Checkers Southgate",         new BigDecimal("-540.00"),  "ZAR", LocalDateTime.now().minusDays(1)),
                new RawTransaction("ABSA-002", "Showmax",                    new BigDecimal("-99.00"),   "ZAR", LocalDateTime.now().minusDays(2)),
                new RawTransaction("ABSA-003", "Bolt",                       new BigDecimal("-75.00"),   "ZAR", LocalDateTime.now().minusDays(3)),
                new RawTransaction("ABSA-004", "Chicken Licken Meadowlands", new BigDecimal("-220.00"),  "ZAR", LocalDateTime.now().minusDays(4)),
                new RawTransaction("ABSA-005", "Engen Booysens",             new BigDecimal("-600.00"),  "ZAR", LocalDateTime.now().minusDays(7))
        );
    }
}
