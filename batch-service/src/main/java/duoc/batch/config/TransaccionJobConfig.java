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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.batch.core.step.skip.SkipPolicy;

import duoc.batch.listener.TransaccionJobListener;
import duoc.batch.listener.DatabaseCleanupJobListener;
import duoc.batch.model.Transaccion;
import duoc.batch.processor.TransaccionProcessor;
import duoc.batch.repository.TransaccionRepository;

@Configuration
public class TransaccionJobConfig {

    @Bean
    public SynchronizedItemStreamReader<Transaccion> transaccionReader() {
        BeanWrapperFieldSetMapper<Transaccion> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(Transaccion.class);
        fieldSetMapper.setCustomEditors(Map.of(
            Date.class,
            new MultiFormatDateEditor()));

        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("id", "fecha", "monto", "tipo");
        tokenizer.setDelimiter(",");

        DefaultLineMapper<Transaccion> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);

        FlatFileItemReader<Transaccion> reader = new FlatFileItemReader<>();
        reader.setName("transaccionReader");
        reader.setResource(new ClassPathResource("movimientos_financieros_diarios.csv"));
        reader.setLinesToSkip(1);
        reader.setLineMapper(lineMapper);
        return new SynchronizedItemStreamReaderBuilder<Transaccion>()
            .delegate(reader)
            .build();
    }

    @Bean
    public RepositoryItemWriter<Transaccion> transaccionWriter(TransaccionRepository repository) {
        return new RepositoryItemWriterBuilder<Transaccion>()
                .repository(repository)
                .methodName("save")
                .build();
    }

    @Bean
    public Step dailytransaccionStep(JobRepository jobRepository,
            PlatformTransactionManager transaccionManager,
            TransaccionRepository repository,
            TransaccionProcessor processor,
            ThreadPoolTaskExecutor batchTaskExecutor,
            SkipPolicy batchSkipPolicy) {
        return new StepBuilder("dailyTransaccionStep", jobRepository)
                .<Transaccion, Transaccion>chunk(10, transaccionManager)
                .faultTolerant()
                .skipPolicy(batchSkipPolicy)
                .retry(TransientDataAccessException.class)
                .retryLimit(3)
                .reader(transaccionReader())
                .processor(processor)
                .writer(transaccionWriter(repository))
                .taskExecutor(batchTaskExecutor)
                .build();
    }

    @Bean
    public Job dailytransaccionJob(JobRepository jobRepository,
            Step dailytransaccionStep,
            TransaccionJobListener listener,
            DatabaseCleanupJobListener transaccionCleanupListener) {
        return new JobBuilder("dailyTransaccionJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(dailytransaccionStep)
                .listener(transaccionCleanupListener)
                .listener(listener)
                .build();
    }

    @Bean
    public DatabaseCleanupJobListener transaccionCleanupListener(TransaccionRepository repository) {
        return new DatabaseCleanupJobListener(repository);
    }
}
