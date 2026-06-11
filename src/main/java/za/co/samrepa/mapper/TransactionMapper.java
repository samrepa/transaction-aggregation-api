package za.co.samrepa.mapper;

import za.co.samrepa.api.model.CategoryTotal;
import za.co.samrepa.api.model.TransactionItem;
import za.co.samrepa.entity.Transaction;
import za.co.samrepa.repository.TransactionRepository;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
public class TransactionMapper {

    public TransactionItem toItem(Transaction t) {
        var item = new TransactionItem();
        item.setTransactionId(t.getId() != null ? t.getId().toString() : null);
        item.setMerchant(t.getMerchant());
        item.setAmount(t.getAmount());
        item.setCurrency(t.getCurrency());
        item.setCategory(t.getCategory());
        item.setTransactionDate(t.getTransactionDate() != null
                ? t.getTransactionDate().atOffset(ZoneOffset.UTC) : null);
        item.setSourceSystem(t.getSourceSystem());
        return item;
    }

    public CategoryTotal toCategoryTotal(TransactionRepository.CategoryTotals totals) {
        var ct = new CategoryTotal();
        ct.setCategory(totals.getCategory());
        ct.setAmount(totals.getTotalAmount());
        ct.setTransactionCount(totals.getTransactionCount() != null
                ? totals.getTransactionCount().intValue() : 0);
        return ct;
    }
}
