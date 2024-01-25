package fr.custom.backend;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {BackendApplication.class})
class BackendApplicationTest {

    @Test
    void whenLoadApplication_thenSuccess() {
        assertTrue(true);
    }

}
