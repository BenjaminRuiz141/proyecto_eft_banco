package duoc.batch.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import duoc.batch.model.Transaccion;

@Component
public class TransaccionProcessor implements ItemProcessor<Transaccion, Transaccion> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransaccionProcessor.class);

    @Override
    public Transaccion process(Transaccion transaccion) {
        if (transaccion == null) {
            LOGGER.warn("Transaccion descartada: registro nulo");
            return null;
        }

        if (transaccion.getMonto() <= 0) {
            LOGGER.warn("Transaccion {} descartada: monto invalido ({})",
                    transaccion.getId(), transaccion.getMonto());
            return null;
        }

        if (transaccion.getTipo() == null || transaccion.getTipo().isBlank()) {
            LOGGER.warn("Transaccion {} descartada: tipo vacio",
                    transaccion.getId());
            return null;
        }

        transaccion.setTipo(transaccion.getTipo().trim().toUpperCase());
        return transaccion;
    }
}
