package duoc.batch.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import duoc.batch.model.Interes;

@Component
public class InteresProcessor implements ItemProcessor<Interes, Interes> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InteresProcessor.class);
    private static final double AHORRO_RATE = 0.02;
    private static final double PRESTAMO_RATE = -0.05;
    private static final double HIPOTECA_RATE = -0.04;

    @Override
    public Interes process(Interes cuenta) {
        if (cuenta == null) {
            LOGGER.warn("Cuenta descartada: registro nulo");
            return null;
        }

        if (cuenta.getSaldo() < 0) {
            LOGGER.warn("Cuenta {} descartada: saldo invalido ({})",
                    cuenta.getId(), cuenta.getSaldo());
            return null;
        }

        if (cuenta.getTipo() == null || cuenta.getTipo().isBlank()) {
            LOGGER.warn("Cuenta {} descartada: tipo de cuenta vacio",
                    cuenta.getId());
            return null;
        }

        String tipo = cuenta.getTipo().trim().toLowerCase();
        double tasa;
        switch (tipo) {
            case "ahorro" -> tasa = AHORRO_RATE;
            case "prestamo" -> tasa = PRESTAMO_RATE;
            case "hipoteca" -> tasa = HIPOTECA_RATE;
            default -> {
                LOGGER.warn("Cuenta {} descartada: tipo de cuenta desconocido ({})",
                        cuenta.getId(), cuenta.getTipo());
                return null;
            }
        };

        cuenta.setTipo(tipo);
        cuenta.setSaldo((int) Math.round(cuenta.getSaldo() * (1 + tasa)));
        return cuenta;
    }
}