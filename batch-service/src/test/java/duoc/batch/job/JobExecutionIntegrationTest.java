package duoc.batch.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import duoc.batch.repository.CuentaAnualRepository;
import duoc.batch.repository.InteresRepository;
import duoc.batch.repository.TransaccionRepository;

@SpringBootTest
class JobExecutionIntegrationTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("dailytransaccionJob")
    private Job dailyTransaccionJob;

    @Autowired
    @Qualifier("monthlyInterestJob")
    private Job monthlyInterestJob;

    @Autowired
    @Qualifier("annualStatementJob")
    private Job annualStatementJob;

    @Autowired
    private TransaccionRepository transaccionRepository;

    @Autowired
    private InteresRepository interesRepository;

    @Autowired
    private CuentaAnualRepository cuentaAnualRepository;

    @Test
    void testDailyTransaccionJobEjecutaYGuardaDatos() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(dailyTransaccionJob, params);

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertTrue(transaccionRepository.count() > 0, "Debe haber transacciones persistidas en base de datos");
    }

    @Test
    void testMonthlyInterestJobEjecutaYGuardaDatos() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(monthlyInterestJob, params);

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertTrue(interesRepository.count() > 0, "Debe haber registros de interes persistidos en base de datos");
    }

    @Test
    void testAnnualStatementJobEjecutaYGuardaDatos() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(annualStatementJob, params);

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertTrue(cuentaAnualRepository.count() > 0, "Debe haber registros de cuentas anuales persistidos en base de datos");
    }
}

