package za.co.samrepa.connector;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectorTest {

    @Test
    void fnbConnector_returnsExpectedTransactions() {
        var connector = new FnbConnector();

        assertThat(connector.sourceName()).isEqualTo("FNB");

        var txns = connector.fetchTransactions("CUST-001");
        assertThat(txns).isNotEmpty();
        txns.forEach(t -> {
            assertThat(t.externalId()).isNotBlank();
            assertThat(t.merchant()).isNotBlank();
            assertThat(t.amount()).isNotNull();
            assertThat(t.currency()).isNotBlank();
            assertThat(t.transactionDate()).isNotNull();
        });
    }

    @Test
    void absaConnector_returnsExpectedTransactions() {
        var connector = new AbsaConnector();

        assertThat(connector.sourceName()).isEqualTo("ABSA");

        var txns = connector.fetchTransactions("CUST-001");
        assertThat(txns).isNotEmpty();
        txns.forEach(t -> assertThat(t.externalId()).isNotBlank());
    }

    @Test
    void capitecConnector_returnsExpectedTransactions() {
        var connector = new CapitecConnector();

        assertThat(connector.sourceName()).isEqualTo("CAPITEC");

        var txns = connector.fetchTransactions("CUST-001");
        assertThat(txns).isNotEmpty();
        txns.forEach(t -> assertThat(t.externalId()).isNotBlank());
    }

    @Test
    void allConnectors_haveUniqueSourceNames() {
        var names = java.util.List.of(
                new FnbConnector().sourceName(),
                new AbsaConnector().sourceName(),
                new CapitecConnector().sourceName()
        );
        assertThat(names).doesNotHaveDuplicates();
    }
}
