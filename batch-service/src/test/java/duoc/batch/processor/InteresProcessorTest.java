package duoc.batch.processor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import duoc.batch.model.Interes;

class InteresProcessorTest {

    private InteresProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new InteresProcessor();
    }

    @Test
    void testCalculoInteresAhorro() {
        // +2% de 5000 = 5100
        Interes cuenta = new Interes(101, "John Doe", 5000, 30, "AHORRO");
        Interes resultado = processor.process(cuenta);

        assertNotNull(resultado);
        assertEquals("ahorro", resultado.getTipo());
        assertEquals(5100, resultado.getSaldo());
    }

    @Test
    void testCalculoInteresPrestamo() {
        // -5% de 8000 = 7600
        Interes cuenta = new Interes(102, "Jane Smith", 8000, 25, "prestamo");
        Interes resultado = processor.process(cuenta);

        assertNotNull(resultado);
        assertEquals("prestamo", resultado.getTipo());
        assertEquals(7600, resultado.getSaldo());
    }

    @Test
    void testCalculoInteresHipoteca() {
        // -4% de 7000 = 6720
        Interes cuenta = new Interes(105, "Charlie Green", 7000, 35, "hipoteca");
        Interes resultado = processor.process(cuenta);

        assertNotNull(resultado);
        assertEquals("hipoteca", resultado.getTipo());
        assertEquals(6720, resultado.getSaldo());
    }

    @Test
    void testDescartarSaldoInvalido() {
        Interes cuenta = new Interes(103, "Bob", -500, 30, "ahorro");
        assertNull(processor.process(cuenta));
    }

    @Test
    void testDescartarTipoDesconocido() {
        Interes cuenta = new Interes(104, "Alice", 1000, 40, "inversion");
        assertNull(processor.process(cuenta));
    }

    @Test
    void testDescartarCuentaNulaOTipoVacio() {
        assertNull(processor.process(null));

        Interes cuentaTipoVacio = new Interes(106, "Pedro", 1000, 20, "   ");
        assertNull(processor.process(cuentaTipoVacio));
    }
}

