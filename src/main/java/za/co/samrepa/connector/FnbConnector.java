package za.co.samrepa.connector;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Mock FNB (First National Bank) connector.
 * In production this would call FNB's Open Banking API.
 */
@Component
public class FnbConnector implements SourceConnector {

    @Override
    public String sourceName() { return "FNB"; }

    @Override
    public List<RawTransaction> fetchTransactions(String customerId) {
        return List.of(
                new RawTransaction("FNB-001", "Shoprite Jabulani Mall",    new BigDecimal("-320.00"),  "ZAR", LocalDateTime.now().minusDays(1)),
                new RawTransaction("FNB-002", "Shell Soweto Highway",      new BigDecimal("-750.00"),  "ZAR", LocalDateTime.now().minusDays(2)),
                new RawTransaction("FNB-003", "Nando's Bara",              new BigDecimal("-185.00"),  "ZAR", LocalDateTime.now().minusDays(3)),
                new RawTransaction("FNB-004", "SALARY CREDIT",             new BigDecimal("28500.00"), "ZAR", LocalDateTime.now().minusDays(5)),
                new RawTransaction("FNB-005", "Uber",                      new BigDecimal("-95.00"),   "ZAR", LocalDateTime.now().minusDays(6))
        );
    }
}
