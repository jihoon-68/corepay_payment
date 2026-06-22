package org.example.corepaypaymentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaAuditing
@SpringBootApplication(scanBasePackages = {
        "org.example.corepaypaymentservice",
        "org.example.corepaycommon"
})
@EnableJpaRepositories(basePackages = {
        "org.example.corepaypaymentservice",
        "org.example.corepaycommon"
})
@EntityScan(basePackages = {
        "org.example.corepaypaymentservice",
        "org.example.corepaycommon"
})
public class CorepayPaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CorepayPaymentServiceApplication.class, args);
    }

}
