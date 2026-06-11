package za.co.samrepa.categorization;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CategorizationService {

    public static final String UNCATEGORIZED = "UNCATEGORIZED";

    private static final Map<String, List<String>> RULES = Map.of(
            "TRANSPORT",     List.of("UBER", "BOLT", "TAXIFY"),
            "GROCERIES",     List.of("WOOLWORTHS", "CHECKERS", "PICK N PAY", "SHOPRITE", "SPAR"),
            "FUEL",          List.of("SHELL", "ENGEN", "SASOL", "CALTEX", "BP"),
            "ENTERTAINMENT", List.of("NETFLIX", "SPOTIFY", "DSTV", "SHOWMAX", "STER-KINEKOR"),
            "DINING",        List.of("NANDO'S", "STEERS", "CHICKEN LICKEN", "KFC", "MCDONALD"),
            "UTILITIES",     List.of("ESKOM", "RAND WATER", "CITY POWER", "JOBURG WATER")
    );

    public String categorize(String merchant) {
        if (merchant == null) return UNCATEGORIZED;
        String upper = merchant.toUpperCase();
        return RULES.entrySet().stream()
                .filter(e -> e.getValue().stream().anyMatch(upper::contains))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(UNCATEGORIZED);
    }
}
