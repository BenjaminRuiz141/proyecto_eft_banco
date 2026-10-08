package duoc.batch.config;

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

import duoc.batch.model.Interes;
import duoc.batch.listener.DatabaseCleanupJobListener;
import duoc.batch.processor.InteresProcessor;
import duoc.batch.repository.InteresRepository;

@Configuration
public class InteresJobConfig {

    @Bean
    public SynchronizedItemStreamReader<Interes> interesReader() {
        BeanWrapperFieldSetMapper<Interes> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(Interes.class);

        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("id", "nombre", "saldo", "edad", "tipo");
        tokenizer.setDelimiter(",");

        DefaultLineMapper<Interes> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);

        FlatFileItemReader<Interes> reader = new FlatFileItemReader<>();
        reader.setName("interesReader");
        reader.setResource(new ClassPathResource("intereses_trimestrales.csv"));
        reader.setLinesToSkip(1);
        reader.setLineMapper(lineMapper);
        return new SynchronizedItemStreamReaderBuilder<Interes>()
            .delegate(reader)
            .build();
    }

    @Bean
    public RepositoryItemWriter<Interes> interesWriter(InteresRepository repository) {
        return new RepositoryItemWriterBuilder<Interes>()
                .repository(repository)
                .methodName("save")
                .build();
    }

    @Bean
    public Step monthlyInterestStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            InteresRepository repository,
            InteresProcessor processor,
            SkipPolicy batchSkipPolicy) {
        return new StepBuilder("monthlyInterestStep", jobRepository)
                .<Interes, Interes>chunk(50, transactionManager)
                .faultTolerant()
                .skipPolicy(batchSkipPolicy)
                .retry(TransientDataAccessException.class)
                .retryLimit(3)
                .reader(interesReader())
                .processor(processor)
                .writer(interesWriter(repository))
                .build();
    }

    @Bean(name = {"monthlyInterestJob", "procesarInteresesJob", "interesesJob"})
    public Job monthlyInterestJob(JobRepository jobRepository,
            Step monthlyInterestStep,
            DatabaseCleanupJobListener interesCleanupListener) {
        return new JobBuilder("monthlyInterestJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(monthlyInterestStep)
                .listener(interesCleanupListener)
                .build();
    }

    @Bean
    public DatabaseCleanupJobListener interesCleanupListener(InteresRepository repository) {
        return new DatabaseCleanupJobListener(repository);
    }
}
