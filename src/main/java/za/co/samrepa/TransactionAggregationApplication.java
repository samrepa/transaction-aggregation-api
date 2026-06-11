package za.co.samrepa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TransactionAggregationApplication {
    public static void main(String[] args) {
        SpringApplication.run(TransactionAggregationApplication.class, args);
    }
}
