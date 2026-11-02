package com.android.dnssinkhole;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:context-test;DB_CLOSE_DELAY=-1",
        "sinkhole.listen-port=0",
        "sinkhole.blocklist.dir=target/no-blocklist"
})
class DnsSinkholeApplicationTest {

    @Test
    void contextLoads() {
    }
}
