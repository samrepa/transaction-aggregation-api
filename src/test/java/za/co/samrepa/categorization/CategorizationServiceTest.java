package za.co.samrepa.categorization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CategorizationServiceTest {

    private final CategorizationService service = new CategorizationService();

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "Uber,                      TRANSPORT",
            "Bolt,                      TRANSPORT",
            "TAXIFY,                    TRANSPORT",
            "Checkers Southgate,        GROCERIES",
            "Woolworths Food,           GROCERIES",
            "Pick n Pay Bara,           GROCERIES",
            "SHOPRITE Jabulani Mall,    GROCERIES",
            "Spar Meadowlands,          GROCERIES",
            "Shell Soweto Highway,      FUEL",
            "Engen Booysens,            FUEL",
            "sasol garage,              FUEL",
            "Caltex,                    FUEL",
            "BP Petrol,                 FUEL",
            "Netflix,                   ENTERTAINMENT",
            "Spotify Premium,           ENTERTAINMENT",
            "DStv Subscription,         ENTERTAINMENT",
            "Showmax,                   ENTERTAINMENT",
            "Ster-Kinekor,              ENTERTAINMENT",
            "Nando's Bara,              DINING",
            "Steers Jabulani,           DINING",
            "Chicken Licken Meadowlands,DINING"
    })
    void categorize_knownMerchants(String merchant, String expectedCategory) {
        assertThat(service.categorize(merchant)).isEqualTo(expectedCategory);
    }

    @Test
    void categorize_unknownMerchant_returnsUncategorized() {
        assertThat(service.categorize("SomeRandomShop XYZ")).isEqualTo("UNCATEGORIZED");
    }

    @Test
    void categorize_nullMerchant_returnsUncategorized() {
        assertThat(service.categorize(null)).isEqualTo("UNCATEGORIZED");
    }

    @Test
    void categorize_emptyMerchant_returnsUncategorized() {
        assertThat(service.categorize("")).isEqualTo("UNCATEGORIZED");
    }

    @Test
    void categorize_caseInsensitive() {
        assertThat(service.categorize("WOOLWORTHS")).isEqualTo("GROCERIES");
        assertThat(service.categorize("woolworths")).isEqualTo("GROCERIES");
        assertThat(service.categorize("WoOlWoRtHs")).isEqualTo("GROCERIES");
    }
}
