package duoc.batch.processor;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import duoc.batch.model.CuentaAnual;

class CuentaAnualProcessorTest {

    private CuentaAnualProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new CuentaAnualProcessor();
    }

    private Date toDate(int year, int month, int day) {
        return Date.from(LocalDate.of(year, month, day)
                .atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    @Test
    void testProcesarMovimientoValido() {
        Date fecha = toDate(2024, 5, 10);
        CuentaAnual cuenta = new CuentaAnual(101, fecha, "deposito", 1000, "  Ingreso mensual  ");
        CuentaAnual resultado = processor.process(cuenta);

        assertNotNull(resultado);
        assertEquals(101, resultado.getCuentaId());
        assertEquals("DEPOSITO", resultado.getTransaccion());
        assertEquals("Ingreso mensual", resultado.getDescripcion());
    }

    @Test
    void testDescartarCuentaDuplicada() {
        Date fecha = toDate(2024, 1, 15);
        CuentaAnual primerMovimiento = new CuentaAnual(201, fecha, "deposito", 500, "Primero");
        CuentaAnual segundoMovimiento = new CuentaAnual(201, fecha, "retiro", 200, "Duplicado");

        assertNotNull(processor.process(primerMovimiento));
        assertNull(processor.process(segundoMovimiento));
    }

    @Test
    void testDescartarAnoFueraDeReporte() {
        Date fecha2023 = toDate(2023, 12, 31);
        Date fecha2025 = toDate(2025, 1, 1);

        CuentaAnual cuentaVieja = new CuentaAnual(301, fecha2023, "deposito", 1000, "Viejo");
        CuentaAnual cuentaFutura = new CuentaAnual(302, fecha2025, "deposito", 1000, "Futuro");

        assertNull(processor.process(cuentaVieja));
        assertNull(processor.process(cuentaFutura));
    }

    @Test
    void testDescartarCuentaInvalidaONula() {
        assertNull(processor.process(null));

        Date fecha = toDate(2024, 3, 1);
        CuentaAnual cuentaIdCero = new CuentaAnual(0, fecha, "deposito", 100, "Test");
        assertNull(processor.process(cuentaIdCero));
    }
}

