package za.co.samrepa.connector;

import java.util.List;

public interface SourceConnector {
    String sourceName();
    List<RawTransaction> fetchTransactions(String customerId);
}
