package za.co.samrepa.service;

import za.co.samrepa.categorization.CategorizationService;
import za.co.samrepa.connector.RawTransaction;
import za.co.samrepa.connector.SourceConnector;
import za.co.samrepa.entity.Customer;
import za.co.samrepa.repository.CustomerRepository;
import za.co.samrepa.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private CustomerRepository    customerRepository;
    @Mock private CategorizationService categorizationService;
    @Mock private SourceConnector       connector;
    @Mock private TransactionTemplate   transactionTemplate;

    private IngestionService ingestionService;

    @BeforeEach
    void setUp() {
        ingestionService = new IngestionService(
                List.of(connector),
                categorizationService,
                transactionRepository,
                customerRepository,
                transactionTemplate
        );
        // Default: execute the lambda directly. Marked lenient because some tests override this stub.
        lenient().when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> callback = inv.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    private Customer buildCustomer(String number) {
        var c = new Customer();
        c.setId(UUID.randomUUID());
        c.setCustomerNumber(number);
        c.setFullName("Thabo Nkosi");
        return c;
    }

    @Test
    void ingestAll_savesNewTransactions() {
        var customer = buildCustomer("CUST-001");
        var raw = new RawTransaction("FNB-001", "Shoprite Jabulani Mall",
                new BigDecimal("-320.00"), "ZAR", LocalDateTime.now());

        when(customerRepository.findAll()).thenReturn(List.of(customer));
        when(connector.sourceName()).thenReturn("FNB");
        when(connector.fetchTransactions("CUST-001")).thenReturn(List.of(raw));
        when(transactionRepository.existsByExternalTransactionIdAndSourceSystem("FNB-001", "FNB"))
                .thenReturn(false);
        when(categorizationService.categorize("Shoprite Jabulani Mall")).thenReturn("GROCERIES");
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ingestionService.ingestAll();

        verify(transactionRepository, times(1)).save(any());
    }

    @Test
    void ingestAll_skipsDuplicateTransactions() {
        var customer = buildCustomer("CUST-001");
        var raw = new RawTransaction("FNB-DUP", "Shell Soweto Highway",
                new BigDecimal("-750.00"), "ZAR", LocalDateTime.now());

        when(customerRepository.findAll()).thenReturn(List.of(customer));
        when(connector.sourceName()).thenReturn("FNB");
        when(connector.fetchTransactions("CUST-001")).thenReturn(List.of(raw));
        when(transactionRepository.existsByExternalTransactionIdAndSourceSystem("FNB-DUP", "FNB"))
                .thenReturn(true);

        ingestionService.ingestAll();

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void ingestAll_setsUncategorizedForUnknownMerchant() {
        var customer = buildCustomer("CUST-001");
        var raw = new RawTransaction("FNB-002", "Kota Palace Soweto",
                new BigDecimal("-50.00"), "ZAR", LocalDateTime.now());

        when(customerRepository.findAll()).thenReturn(List.of(customer));
        when(connector.sourceName()).thenReturn("FNB");
        when(connector.fetchTransactions("CUST-001")).thenReturn(List.of(raw));
        when(transactionRepository.existsByExternalTransactionIdAndSourceSystem(anyString(), anyString()))
                .thenReturn(false);
        when(categorizationService.categorize("Kota Palace Soweto")).thenReturn("UNCATEGORIZED");
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ingestionService.ingestAll();

        verify(transactionRepository, times(1)).save(argThat(t -> "UNCATEGORIZED".equals(t.getCategory())));
    }

    @Test
    void ingestAll_withMultipleConnectors_processesAll() {
        var customer = buildCustomer("CUST-001");
        SourceConnector connectorB = mock(SourceConnector.class);
        ingestionService = new IngestionService(
                List.of(connector, connectorB),
                categorizationService,
                transactionRepository,
                customerRepository,
                transactionTemplate
        );

        var rawA = new RawTransaction("FNB-010", "Netflix", new BigDecimal("-199.00"), "ZAR", LocalDateTime.now());
        var rawB = new RawTransaction("ABSA-010", "Checkers Southgate", new BigDecimal("-400.00"), "ZAR", LocalDateTime.now());

        when(customerRepository.findAll()).thenReturn(List.of(customer));
        when(connector.sourceName()).thenReturn("FNB");
        when(connectorB.sourceName()).thenReturn("ABSA");
        when(connector.fetchTransactions(anyString())).thenReturn(List.of(rawA));
        when(connectorB.fetchTransactions(anyString())).thenReturn(List.of(rawB));
        when(transactionRepository.existsByExternalTransactionIdAndSourceSystem(anyString(), anyString()))
                .thenReturn(false);
        when(categorizationService.categorize(anyString())).thenReturn("UNCATEGORIZED");
        // @BeforeEach already stubs transactionTemplate.execute — no re-stub needed here
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ingestionService.ingestAll();

        verify(transactionRepository, times(2)).save(any());
    }

    @Test
    void ingestAll_continuesWhenOneConnectorFails() {
        var customer = buildCustomer("CUST-001");
        SourceConnector failingConnector = mock(SourceConnector.class);
        ingestionService = new IngestionService(
                List.of(failingConnector, connector),
                categorizationService,
                transactionRepository,
                customerRepository,
                transactionTemplate
        );

        var raw = new RawTransaction("FNB-020", "Engen Soweto", new BigDecimal("-480.00"), "ZAR", LocalDateTime.now());

        when(customerRepository.findAll()).thenReturn(List.of(customer));
        when(failingConnector.sourceName()).thenReturn("BROKEN");
        when(connector.sourceName()).thenReturn("FNB");
        // doThrow/doAnswer avoids triggering the @BeforeEach stub during Mockito setup recording
        doThrow(new RuntimeException("Bank API timeout"))
                .doAnswer(inv -> {
                    TransactionCallback<?> callback = inv.getArgument(0);
                    return callback.doInTransaction(null);
                })
                .when(transactionTemplate).execute(any());
        when(connector.fetchTransactions(anyString())).thenReturn(List.of(raw));
        when(transactionRepository.existsByExternalTransactionIdAndSourceSystem(anyString(), anyString()))
                .thenReturn(false);
        when(categorizationService.categorize(anyString())).thenReturn("FUEL");
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Should not throw — failure is caught and logged
        ingestionService.ingestAll();

        verify(transactionRepository, times(1)).save(any());
    }
}
