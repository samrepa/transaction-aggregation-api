package za.co.samrepa.connector;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RawTransaction(
        String externalId,
        String merchant,
        BigDecimal amount,
        String currency,
        LocalDateTime transactionDate
) {}
