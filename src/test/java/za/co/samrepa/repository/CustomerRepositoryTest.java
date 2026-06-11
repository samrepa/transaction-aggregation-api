package za.co.samrepa.repository;

import za.co.samrepa.entity.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    private Customer saveCustomer(String number, String name) {
        var c = new Customer();
        c.setCustomerNumber(number);
        c.setFullName(name);
        return customerRepository.save(c);
    }

    @Test
    void findByCustomerNumber_returnsCustomer_whenExists() {
        saveCustomer("CUST-T01", "Test User");

        var result = customerRepository.findByCustomerNumber("CUST-T01");
        assertThat(result).isPresent();
        assertThat(result.get().getFullName()).isEqualTo("Test User");
    }

    @Test
    void findByCustomerNumber_returnsEmpty_whenNotExists() {
        var result = customerRepository.findByCustomerNumber("CUST-MISSING");
        assertThat(result).isEmpty();
    }

    @Test
    void save_persists_andAssignsId() {
        var customer = saveCustomer("CUST-T02", "Jane Doe");
        assertThat(customer.getId()).isNotNull();
    }

    @Test
    void customerNumber_isUnique() {
        saveCustomer("CUST-T03", "Original");
        var duplicate = new Customer();
        duplicate.setCustomerNumber("CUST-T03");
        duplicate.setFullName("Duplicate");

        org.junit.jupiter.api.Assertions.assertThrows(
                Exception.class,
                () -> customerRepository.saveAndFlush(duplicate));
    }
}
