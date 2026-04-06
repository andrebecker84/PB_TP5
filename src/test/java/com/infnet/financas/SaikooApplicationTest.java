package com.infnet.financas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SaikooApplicationTest {

    @Test
    void contextLoads() {
        // Verifica que o contexto Spring sobe corretamente
    }

    @Test
    void mainMethodStartsApplication() {
        SaikooApplication.main(new String[]{});
    }
}
