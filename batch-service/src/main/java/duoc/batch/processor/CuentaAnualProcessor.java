package duoc.batch.processor;

import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

import duoc.batch.model.CuentaAnual;

@Component
@StepScope
public class CuentaAnualProcessor implements ItemProcessor<CuentaAnual, CuentaAnual> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CuentaAnualProcessor.class);
    private final Set<Integer> cuentasProcesadas = new HashSet<>();

    @Override
    public CuentaAnual process(CuentaAnual movimiento) {
        if (movimiento == null) {
            LOGGER.warn("Movimiento anual descartado: registro nulo");
            return null;
        }

        if (movimiento.getCuentaId() <= 0) {
            LOGGER.warn("Movimiento anual descartado: cuenta invalida ({})",
                    movimiento.getCuentaId());
            return null;
        }

        if (!cuentasProcesadas.add(movimiento.getCuentaId())) {
            LOGGER.warn("Movimiento anual de cuenta {} descartado: cuenta duplicada",
                    movimiento.getCuentaId());
            return null;
        }

        if (movimiento.getFecha() == null) {
            LOGGER.warn("Movimiento anual de cuenta {} descartado: fecha vacia",
                    movimiento.getCuentaId());
            return null;
        }

        if (movimiento.getTransaccion() == null || movimiento.getTransaccion().isBlank()) {
            LOGGER.warn("Movimiento anual de cuenta {} descartado: transaccion vacia",
                    movimiento.getCuentaId());
            return null;
        }

        int year = movimiento.getFecha().toInstant()
                .atZone(ZoneId.systemDefault())
                .getYear();
        if (year != 2024) {
            LOGGER.warn("Movimiento anual de cuenta {} descartado: ano fuera de reporte ({})",
                movimiento.getCuentaId(), year);
            return null;
        }

        movimiento.setTransaccion(movimiento.getTransaccion().trim().toUpperCase());
        if (movimiento.getDescripcion() != null) {
            movimiento.setDescripcion(movimiento.getDescripcion().trim());
        }
        return movimiento;
    }
}