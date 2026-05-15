package org.example.corepaypaymentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication(scanBasePackages = {
        "org.example.corepaypaymentservice",
        "org.example.corepaycommon"
})
public class CorepayPaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CorepayPaymentServiceApplication.class, args);
    }

}
