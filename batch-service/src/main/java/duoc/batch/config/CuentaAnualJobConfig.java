package duoc.batch.config;

import java.util.Date;
import java.util.Map;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.batch.core.step.skip.SkipPolicy;

import duoc.batch.model.CuentaAnual;
import duoc.batch.listener.DatabaseCleanupJobListener;
import duoc.batch.processor.CuentaAnualProcessor;
import duoc.batch.repository.CuentaAnualRepository;

@Configuration
public class CuentaAnualJobConfig {

    @Bean
    public SynchronizedItemStreamReader<CuentaAnual> cuentaAnualReader() {
        BeanWrapperFieldSetMapper<CuentaAnual> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(CuentaAnual.class);
        fieldSetMapper.setCustomEditors(Map.of(
                Date.class,
                new MultiFormatDateEditor()));

        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("cuentaId", "fecha", "transaccion", "monto", "descripcion");
        tokenizer.setDelimiter(",");

        DefaultLineMapper<CuentaAnual> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);

        FlatFileItemReader<CuentaAnual> reader = new FlatFileItemReader<>();
        reader.setName("cuentaAnualReader");
        reader.setResource(new ClassPathResource("estados_financieros_anuales.csv"));
        reader.setLinesToSkip(1);
        reader.setLineMapper(lineMapper);
        return new SynchronizedItemStreamReaderBuilder<CuentaAnual>()
            .delegate(reader)
            .build();
    }

    @Bean
    public RepositoryItemWriter<CuentaAnual> cuentaAnualWriter(CuentaAnualRepository repository) {
        return new RepositoryItemWriterBuilder<CuentaAnual>()
                .repository(repository)
                .methodName("save")
                .build();
    }

    @Bean
    public Step annualStatementStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            CuentaAnualRepository repository,
            CuentaAnualProcessor processor,
            SkipPolicy batchSkipPolicy) {
        return new StepBuilder("annualStatementStep", jobRepository)
                .<CuentaAnual, CuentaAnual>chunk(50, transactionManager)
                .faultTolerant()
                .skipPolicy(batchSkipPolicy)
                .retry(TransientDataAccessException.class)
                .retryLimit(3)
                .reader(cuentaAnualReader())
                .processor(processor)
                .writer(cuentaAnualWriter(repository))
                .build();
    }

    @Bean(name = {"annualStatementJob", "procesarEstadosFinancierosJob", "estadosCuentaAnualesJob"})
    public Job annualStatementJob(JobRepository jobRepository,
            Step annualStatementStep,
            DatabaseCleanupJobListener cuentaAnualCleanupListener) {
        return new JobBuilder("annualStatementJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(annualStatementStep)
                .listener(cuentaAnualCleanupListener)
                .build();
    }

    @Bean
    public DatabaseCleanupJobListener cuentaAnualCleanupListener(CuentaAnualRepository repository) {
        return new DatabaseCleanupJobListener(repository);
    }
}
