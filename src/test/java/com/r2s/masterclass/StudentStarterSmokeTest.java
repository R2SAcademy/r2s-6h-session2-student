package com.r2s.masterclass;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("h2")
class StudentStarterSmokeTest {

    @Test
    void contextLoads() {
        // Verifies the Spring ApplicationContext starts successfully
    }
}
