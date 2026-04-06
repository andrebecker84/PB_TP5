package com.infnet.financas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class SaikooApplicationTest {

    @Test
    void contextLoads() {
        // Verifica que o contexto Spring sobe corretamente
    }

    @Test
    void mainMethodStartsApplication() {
        assertDoesNotThrow(() -> SaikooApplication.main(new String[]{}));
    }
}
