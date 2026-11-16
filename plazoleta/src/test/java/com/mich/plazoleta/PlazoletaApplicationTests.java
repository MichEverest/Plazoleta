package com.mich.plazoleta;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:plazoleta-test;DB_CLOSE_DELAY=-1",
        "plazoleta.anuncio.enabled=false"})
class PlazoletaApplicationTests {

	@Test
	void contextLoads() {
	}

}
