# Proyecto Spring Batch - Procesamiento de Datos Financieros

Aplicacion desarrollada con Spring Boot y Spring Batch para procesar tres flujos de datos a partir de archivos CSV y almacenarlos en una base de datos MySQL local.

## 1. Arquitectura de la Solucion

El sistema sigue la arquitectura estandar de Spring Batch basada en procesamiento por fragmentos (chunks de 5 registros). Cada proceso cuenta con tres fases:

1. Reader: Lee registros desde un archivo CSV utilizando SynchronizedItemStreamReader para permitir concurrencia de forma segura.
2. Processor: Valida las reglas de negocio, transforma los datos y descarta registros que no cumplan los criterios.
3. Writer: Guarda los datos procesados en MySQL mediante Spring Data JPA (RepositoryItemWriter).

Componentes transversales:
- ThreadPoolTaskExecutor: Ejecucion multihilo (3 trabajadores concurrentes).
- DatabaseCleanupJobListener: Limpia la tabla destino antes de iniciar cada Job para asegurar datos limpios y consistentes.
- RunIdIncrementer: Permite volver a ejecutar los jobs consecutivamente sin bloqueos de Spring Batch.

### Jobs implementados

1. dailyTransaccionJob
- Archivo origen: transacciones.csv
- Logica: Descarta montos menores o iguales a cero y tipos vacios. Convierte el tipo a mayusculas (DEBITO / CREDITO).
- Destino: Tabla transacciones

2. monthlyInterestJob
- Archivo origen: intereses.csv
- Logica: Valida saldos positivos. Aplica tasas segun tipo de cuenta: ahorro (+2%), prestamo (-5%), hipoteca (-4%).
- Destino: Tabla interes

3. annualStatementJob
- Archivo origen: cuentas_anuales.csv
- Logica: Filtra movimientos del ano 2024, descarta cuentas duplicadas y formatea descripcion.
- Destino: Tabla cuenta_anual

## 2. Politicas de Resiliencia (Skip y Retry)

Cada Step cuenta con mecanismos de tolerancia a fallos:

- Skip Policy (BatchSkipPolicy): Omite hasta 100 registros con errores de formato o conversion en el archivo CSV (FlatFileParseException, BindException, IllegalArgumentException) sin abortar el lote.
- Retry Policy: Reintenta hasta 3 veces cualquier operacion que falle por errores temporales de base de datos o bloqueos concurrentes (TransientDataAccessException).

## 3. Requisitos y Configuracion

Requisitos:
- Java 21
- MySQL 8.0 corriendo en el puerto 3306

Configuracion en src/main/resources/application.properties:
- spring.datasource.url=jdbc:mysql://localhost:3306/banco_xyz_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
- spring.datasource.username=root
- spring.datasource.password=(colocar tu contrasena de MySQL aqui si tiene)

Nota: El parametro createDatabaseIfNotExist=true crea la base de datos banco_xyz_db automaticamente en el primer arranque.

## 4. Como Ejecutar los Jobs

Desde la consola en la carpeta del proyecto (PowerShell o CMD):

- Ejecutar Job de Transacciones:
  ./mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--spring.batch.job.name=dailyTransaccionJob"

- Ejecutar Job de Intereses:
  ./mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--spring.batch.job.name=monthlyInterestJob"

- Ejecutar Job de Cuentas Anuales:
  ./mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--spring.batch.job.name=annualStatementJob"

Al terminar el proceso, la aplicacion se apaga automaticamente gracias a la configuracion spring.main.web-application-type=none.

Tambien puedes consultar el archivo COMANDOS_JOBS.txt para ver opciones alternativas de ejecucion por JAR o configuracion directa.

## 5. Pruebas Automatizadas

El proyecto cuenta con 18 pruebas automatizadas que cubren:
- Pruebas unitarias de logica y validaciones para los tres procesadores (TransaccionProcessorTest, InteresProcessorTest, CuentaAnualProcessorTest).
- Pruebas de integracion que ejecutan los jobs completos y verifican la persistencia en base de datos (JobExecutionIntegrationTest).

Para ejecutar las pruebas:
./mvnw.cmd test

## 6. Evidencia de Ejecucion y Verificacion

Para comprobar los datos almacenados en MySQL:

USE banco_xyz_db;

-- 1. Datos cargados por el Job de transacciones:
SELECT * FROM transacciones;

-- 2. Datos cargados por el Job de intereses:
SELECT * FROM interes;

-- 3. Datos cargados por el Job de cuentas anuales:
SELECT * FROM cuenta_anual;

-- 4. Historial de ejecuciones registrado por Spring Batch:
SELECT JOB_EXECUTION_ID, STATUS, START_TIME, END_TIME, EXIT_CODE 
FROM BATCH_JOB_EXECUTION 
ORDER BY JOB_EXECUTION_ID DESC;

En la salida de consola se puede verificar:
- Registro de inicio y limpieza: "Limpiando registros previos en la base de datos antes de iniciar job..."
- Log resumen del Job: "Finalizo job ... con estado COMPLETED. Leidos: X. Escritos: Y. Omitidos: Z."


