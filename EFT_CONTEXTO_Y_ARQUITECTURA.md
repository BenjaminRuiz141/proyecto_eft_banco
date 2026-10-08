# 🏛️ Evaluación Final Transversal (EFT) - Banco XYZ
## Ecosistema de Microservicios Cloud, Patrón BFF y Procesamiento Batch (PBY2203)

---

## 📌 1. Resumen Ejecutivo del Proyecto
El **Banco XYZ** es una institución bancaria tradicional con más de 30 años en el mercado financiero. Su infraestructura histórica operaba sobre servidores mainframe ejecutando programas en **COBOL** y scripts en **Shell** para sus operaciones críticas (procesamiento diario de transacciones, cálculo de intereses y estados de cuenta anuales).

Ante los desafíos de escalabilidad, acoplamiento monolítico y costos operativos, se implementa una **arquitectura moderna, distribuida y cloud-native** basada en:
1. **Spring Batch**: Ingesta masiva y procesamiento paralelo tolerante a fallos de los datos legacy.
2. **Backend for Frontend (BFF)**: 3 canales optimizados de forma independiente (**BFF Web**, **BFF Móvil**, **BFF ATM/Cajero**).
3. **Plataforma Spring Cloud**: Descubrimiento dinámico de servicios (**Netflix Eureka**), configuración centralizada (**Spring Cloud Config**) y seguridad distribuida (**OAuth2 / JWT**).
4. **Resiliencia y Mensajería Asíncrona**: Tolerancia a fallos con **Resilience4j** (Circuit Breakers, Retries y Fallbacks) y arquitectura orientada a eventos con mensajería asíncrona / patrón Saga.
5. **Docker & Cloud Readiness**: Despliegue orquestado mediante **Docker Compose** y preparado para despliegue en la nube (**AWS**).

---

## 🏗️ 2. Arquitectura Global de la Solución

`mermaid
graph TD
    subgraph Canales ["Clientes / Canales de Acceso"]
        W[Navegador Web / Portal Clientes]
        M[App Móvil Android / iOS]
        A[Red de Cajeros Automáticos ATM]
    end

    subgraph Seguridad ["Seguridad y Autenticación Centralizada"]
        AUTH[auth-service: 8084<br/>Spring Security + GitHub OAuth2 + JWT]
    end

    subgraph PlataformaCloud ["Infraestructura Spring Cloud"]
        CONF[config-server: 8888<br/>Spring Cloud Config]
        EUK[eureka-server: 8761<br/>Netflix Eureka Service Discovery]
        BROKER[(Message Broker<br/>ActiveMQ Artemis / Kafka)]
    end

    subgraph CapaBFF ["Capa Backend For Frontend (BFF)"]
        BFF_W[bff-web: 8081<br/>Canal Web - DTOs Completos<br/>ROLE_WEB]
        BFF_M[bff-mobile: 8082<br/>Canal Movil - DTOs Ligeros<br/>ROLE_MOBILE]
        BFF_A[bff-atm: 8083<br/>Canal ATM - Retiros y Riesgo<br/>ROLE_ATM]
    end

    subgraph BatchSystem ["Ingesta y Procesamiento Batch"]
        BATCH[batch-service: 8088<br/>Spring Batch 5 - 3 Jobs Paralelos]
    end

    subgraph Persistencia ["Almacenamiento de Datos"]
        DB[(MySQL Database: banco_xyz_db<br/>Tablas: interes, transacciones, cuenta_anual)]
    end

    %% Flujos de Autenticacion
    W -->|1. Solicita Token| AUTH
    M -->|1. Solicita Token| AUTH
    A -->|1. Solicita Token| AUTH

    %% Consumo de BFFs
    W -->|2. Peticion con Bearer Token WEB| BFF_W
    M -->|2. Peticion con Bearer Token MOBILE| BFF_M
    A -->|2. Peticion con Bearer Token ATM| BFF_A

    %% Registro y Descubrimiento
    BFF_W -.->|Registro y Health| EUK
    BFF_M -.->|Registro y Health| EUK
    BFF_A -.->|Registro y Health| EUK
    AUTH -.->|Registro y Health| EUK

    %% Configuracion Centralizada
    BFF_W -.->|Pull Config| CONF
    BFF_M -.->|Pull Config| CONF
    BFF_A -.->|Pull Config| CONF
    AUTH -.->|Pull Config| CONF

    %% Resiliencia y Eventos
    BFF_A -->|Publica Transaccion / Saga| BROKER
    BROKER -->|Notificaciones / Resiliencia| BFF_M

    %% Persistencia
    BATCH -->|Ingesta desde CSVs| DB
    BFF_W -->|Lectura de Cuentas / Indicadores| DB
    BFF_M -->|Consulta Saldo y Notificaciones| DB
    BFF_A -->|Validacion y Retiros| DB
`

---

## 📊 3. Trazabilidad de Datos Legacy a la Nueva Plataforma

| Archivo Legacy (Semana 9 EFT) | Entidad Java | Tabla MySQL (atchdb) | Job de Spring Batch | Consumidores en la Plataforma |
|---|---|---|---|---|
| movimientos_financieros_diarios.csv (1.001 filas) | Transaccion.java | 	ransacciones | dailyTransaccionJob | Auditoría, reportes de movimientos y resúmenes diarios. |
| intereses_trimestrales.csv (1.001 filas) | Interes.java | interes | monthlyInterestJob | **BFF Web, Mobile y ATM**: Provee los titulares, saldos, tipo de cuenta y edad. |
| stados_financieros_anuales.csv (1.001 filas) | CuentaAnual.java | cuenta_anual | nnualStatementJob | Auditorías anuales, resúmenes consolidados y cumplimiento tributario. |

---

## 🔌 4. Matriz de Microservicios, Puertos y Roles

| Componente / Microservicio | Puerto Host | Puerto Contenedor | Seguridad / Rol Exigido | Propósito Principal |
|---|:---:|:---:|:---:|---|
| **config-server** | 8888 | 8888 | Acceso interno / Actuator | Servidor centralizado de configuración con perfil 
ative y repositorio config-repo. |
| **eureka-server** | 8761 | 8761 | Público / Monitoreo | Service Discovery para resolución dinámica de nombres e instancias. |
| **auth-service** | 8084 | 8084 | Público / OAuth2 | Generación de JWT tokens con roles específicos (ROLE_WEB, ROLE_MOBILE, ROLE_ATM) y GitHub OAuth2. |
| **bff-web** | 8081 | 8081 | ROLE_WEB | Optimizado para navegadores, datos completos del cliente, saldo e indicadores financieros. |
| **bff-mobile** | 8082 | 8082 | ROLE_MOBILE | Optimizado para bajo consumo de ancho de banda, DTOs ligeros, notificaciones móviles push. |
| **bff-atm** | 8083 | 8083 | ROLE_ATM | Operaciones transaccionales críticas, retiros en cajero y validación de riesgo con CircuitBreaker. |
| **batch-service** | 8088 | 8088 | Interno / Batch CLI | 3 Jobs de Spring Batch con procesamiento multihilo, chunk de 10 y política de omisión hasta 1.000 anomalías. |
| **activemq-artemis** | 8161 (Web) / 61616 (JMS) | Mismos | Admin/Broker | Broker de mensajería asíncrona para orquestación de eventos y transacciones distribuidas (Saga). |

---

## 🎯 5. Mapeo con la Rúbrica de Evaluación EFT (100 Puntos)

1. **Criterio 1: Identificación de los 5 Procesos Clave (10 Pts - 100%)**
   * ✅ Migración de Procesos Batch a Spring Batch.
   * ✅ División del Sistema Monolítico en Microservicios.
   * ✅ Implementación del Patrón Backend for Frontend (BFF).
   * ✅ Implementación de Seguridad Distribuida con Spring Cloud Security (OAuth2 / JWT).
   * ✅ Integración de Mensajería Asíncrona (Eventos y Sagas con ActiveMQ/Kafka).

2. **Criterio 2: Propuesta de Arquitectura Justificada (15 Pts - 100%)**
   * ✅ Desacoplamiento por canales (Web, Móvil, ATM).
   * ✅ Centralización de configuración y descubrimiento elástico.
   * ✅ Resiliencia ante caídas de dependencias externas.

3. **Criterio 3: Spring Batch Avanzado (15 Pts - 100%)**
   * ✅ 3 Jobs implementados: Transacciones Diarias, Intereses Mensuales y Cuentas Anuales.
   * ✅ Tolerancia a fallos: BatchSkipPolicy para 1.000 omisiones y reintentos automáticos con @retry(TransientDataAccessException.class).
   * ✅ Paralelismo y escalabilidad: ThreadPoolTaskExecutor (3 workers paralelos) y SynchronizedItemStreamReaderBuilder.
   * ✅ Parser multiformato: MultiFormatDateEditor para formatos yyyy-MM-dd, yyyy/MM/dd, dd-MM-yyyy, etc.

4. **Criterio 4: Patrón BFF para Múltiples Canales (15 Pts - 100%)**
   * ✅ 3 Canales con optimización de payload y seguridad estricta: ff-web (completo), ff-mobile (ligero) y ff-atm (seguro/retiros).

5. **Criterio 5: Microservicios Resilientes con Spring Cloud (15 Pts - 100%)**
   * ✅ Circuit Breaker con **Resilience4j** en validación de riesgos y notificaciones.
   * ✅ Fallbacks configurados para garantizar alta disponibilidad.
   * ✅ Mensajería asíncrona para eventos de transacciones.

6. **Criterio 6: Docker y Escalabilidad Horizontal (10 Pts - 100%)**
   * ✅ docker-compose.yml unificado para todos los módulos.
   * ✅ Dockerfile optimizados con OpenJDK Alpine y opciones JVM containerizadas.
   * ✅ Aislamiento de red (anco-network) y healthchecks para inicio ordenado.

7. **Criterio 7: Documentación de Aspectos Clave (10 Pts - 100%)**
   * 📝 eadme.md: Repositorio GitHub con descripción técnica y arquitectura.
   * 📝 Informe Técnico (PDF plantilla Duoc UC): Análisis, diagramas y justificación.
   * 📝 instrucciones.md: Pruebas paso a paso con cURL y Postman.
   * 📝 despliegue.md: Pasos detallados para puesta en producción en nube (AWS).

8. **Criterio 8: Presentación en Video (10 Pts - 100%)**
   * 🎥 Video de 5 a 7 min en MP4 con webcam visible:
     - Resumen ejecutivo.
     - Resultados y comparación con sistema legacy.
     - Desafíos enfrentados y soluciones implementadas.
     - Propuestas de mejora futura.

