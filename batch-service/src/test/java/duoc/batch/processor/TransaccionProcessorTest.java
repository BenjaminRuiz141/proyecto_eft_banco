package duoc.batch.processor;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import duoc.batch.model.Transaccion;

class TransaccionProcessorTest {

    private TransaccionProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new TransaccionProcessor();
    }

    @Test
    void testProcesarTransaccionValida() {
        Transaccion t = new Transaccion(1, new Date(), 1500, " debito ");
        Transaccion resultado = processor.process(t);

        assertNotNull(resultado);
        assertEquals(1, resultado.getId());
        assertEquals(1500, resultado.getMonto());
        assertEquals("DEBITO", resultado.getTipo());
    }

    @Test
    void testDescartarTransaccionNula() {
        assertNull(processor.process(null));
    }

    @Test
    void testDescartarMontoCeroONegativo() {
        Transaccion tCero = new Transaccion(2, new Date(), 0, "credito");
        Transaccion tNegativo = new Transaccion(3, new Date(), -200, "debito");

        assertNull(processor.process(tCero));
        assertNull(processor.process(tNegativo));
    }

    @Test
    void testDescartarTipoVacioONulo() {
        Transaccion tVacio = new Transaccion(4, new Date(), 500, "   ");
        Transaccion tNulo = new Transaccion(5, new Date(), 500, null);

        assertNull(processor.process(tVacio));
        assertNull(processor.process(tNulo));
    }
}

