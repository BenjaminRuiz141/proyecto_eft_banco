package duoc.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class TransaccionJobListener implements JobExecutionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransaccionJobListener.class);

    @Override
    public void beforeJob(JobExecution jobExecution) {
	LOGGER.info("Iniciando job {} con parametros {}",
		jobExecution.getJobInstance().getJobName(),
		jobExecution.getJobParameters());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
	LOGGER.info("Finalizo job {} con estado {}. Inicio: {}. Fin: {}. Leidos: {}. Escritos: {}. Omitidos: {}",
		jobExecution.getJobInstance().getJobName(),
		jobExecution.getStatus(),
		jobExecution.getStartTime(),
		jobExecution.getEndTime(),
		jobExecution.getStepExecutions().stream()
			.mapToLong(stepExecution -> stepExecution.getReadCount())
			.sum(),
		jobExecution.getStepExecutions().stream()
			.mapToLong(stepExecution -> stepExecution.getWriteCount())
			.sum(),
		jobExecution.getStepExecutions().stream()
			.mapToLong(stepExecution -> stepExecution.getFilterCount())
			.sum());
    }
}
