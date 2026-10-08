package duoc.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.data.jpa.repository.JpaRepository;

public class DatabaseCleanupJobListener implements JobExecutionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseCleanupJobListener.class);
    private final JpaRepository<?, ?> repository;

    public DatabaseCleanupJobListener(JpaRepository<?, ?> repository) {
        this.repository = repository;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        LOGGER.info("Limpiando registros previos en la base de datos antes de iniciar job '{}'...",
                jobExecution.getJobInstance().getJobName());
        repository.deleteAllInBatch();
        LOGGER.info("Tabla limpiada exitosamente para el job '{}'.",
                jobExecution.getJobInstance().getJobName());
    }
}
