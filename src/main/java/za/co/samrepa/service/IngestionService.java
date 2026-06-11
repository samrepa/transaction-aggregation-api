package za.co.samrepa.service;

import za.co.samrepa.categorization.CategorizationService;
import za.co.samrepa.connector.SourceConnector;
import za.co.samrepa.entity.Customer;
import za.co.samrepa.entity.Transaction;
import za.co.samrepa.repository.CustomerRepository;
import za.co.samrepa.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionService {

    private final List<SourceConnector>  connectors;
    private final CategorizationService  categorizationService;
    private final TransactionRepository  transactionRepository;
    private final CustomerRepository     customerRepository;
    private final TransactionTemplate    transactionTemplate;

    public void ingestAll() {
        List<Customer> customers = customerRepository.findAll();
        log.info("ingestion started: customers={}", customers.size());

        int saved = 0;
        for (Customer customer : customers) {
            for (SourceConnector connector : connectors) {
                try {
                    Integer count = transactionTemplate.execute(status -> ingest(customer, connector));
                    saved += count != null ? count : 0;
                } catch (Exception ex) {
                    log.error("ingestion failed: customer={} source={}",
                            customer.getCustomerNumber(), connector.sourceName(), ex);
                }
            }
        }
        log.info("ingestion finished: newTransactions={}", saved);
    }

    private int ingest(Customer customer, SourceConnector connector) {
        var rawList = connector.fetchTransactions(customer.getCustomerNumber());
        int count = 0;

        for (var raw : rawList) {
            if (transactionRepository.existsByExternalTransactionIdAndSourceSystem(
                    raw.externalId(), connector.sourceName())) {
                continue;
            }

            var transaction = new Transaction();
            transaction.setExternalTransactionId(raw.externalId());
            transaction.setSourceSystem(connector.sourceName());
            transaction.setCustomer(customer);
            transaction.setTransactionDate(raw.transactionDate());
            transaction.setMerchant(raw.merchant());
            transaction.setAmount(raw.amount());
            transaction.setCurrency(raw.currency());
            transaction.setCategory(categorizationService.categorize(raw.merchant()));

            transactionRepository.save(transaction);
            count++;
        }
        return count;
    }
}
