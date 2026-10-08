package duoc.batch.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.validation.BindException;

public class BatchSkipPolicy implements SkipPolicy {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchSkipPolicy.class);
    // Configurado para permitir omitir hasta 1000 registros anomalos o malformados del dataset legacy
    private static final long MAX_SKIPS = 1000;

    @Override
    public boolean shouldSkip(Throwable throwable, long skipCount) throws SkipLimitExceededException {
        boolean skippable = throwable instanceof FlatFileParseException
            || throwable instanceof BindException
            || throwable instanceof IllegalArgumentException
            || throwable instanceof NumberFormatException;

        if (!skippable) {
            return false;
        }

        if (skipCount >= MAX_SKIPS) {
            LOGGER.error("Se ha excedido el limite maximo de omisiones ({})", MAX_SKIPS);
            throw new SkipLimitExceededException(MAX_SKIPS, throwable);
        }

        LOGGER.warn("Registro omitido (#{}) por error de lectura o mapeo: {}", (skipCount + 1), throwable.getMessage());
        return true;
    }
}
