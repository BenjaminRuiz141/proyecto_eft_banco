# 📋 Guía de Ejecución y Pruebas del Sistema - Banco XYZ
## Evaluación Final Transversal (EFT) - Desarrollo Backend III (PBY2203)

Este documento detalla el paso a paso para compilar, ejecutar y verificar el funcionamiento de cada componente del ecosistema bancario del **Banco XYZ**.

---

## 1. Prerrequisitos del Entorno

* **Java JDK**: Versión 17 o superior instalada (`java -version`).
* **Apache Maven**: Versión 3.9+ (`mvn -version`).
* **Docker Desktop**: En ejecución con soporte de Docker Compose (`docker compose version`).
* **Base de Datos MySQL**: Instancia local en puerto `3306` con la base de datos `banco_xyz_db` creada:
  ```sql
  CREATE DATABASE IF NOT EXISTS banco_xyz_db;
  ```

---

## 2. Paso 1: Ingesta de Datos Legacy (Spring Batch)

Antes de iniciar los servicios cliente, ejecutamos el módulo batch para procesar los 3 archivos CSV de 1.000 filas (`movimientos_financieros_diarios.csv`, `intereses_trimestrales.csv`, `estados_financieros_anuales.csv`) y poblar las tablas en MySQL (`transacciones`, `interes`, `cuenta_anual`):

```bash
cd batch-service

# 1. Ejecutar Job de Transacciones Diarias (default):
mvn spring-boot:run

# 2. O ejecutar el Job de Calculo de Intereses Mensuales (puebla cuentas para los BFFs):
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.batch.job.name=monthlyInterestJob"

# 3. O ejecutar el Job de Estados de Cuenta Anuales:
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.batch.job.name=annualStatementJob"
```

* **Resultado esperado**: La consola mostrará el procesamiento en chunks de los 3 jobs (`dailyTransaccionJob`, `monthlyInterestJob`, `annualStatementJob`) con tolerancia a fallos mediante `BatchSkipPolicy` y `MultiFormatDateEditor`. Al finalizar, la tabla `interes` quedará cargada con las cuentas bancarias de prueba.

---

## 3. Paso 2: Despliegue de los Microservicios con Docker Compose

Desde la raíz del proyecto (`proyecto_eft_banco`):

```bash
# 1. Asegurar la existencia del archivo de entorno .env:
cp .env.example .env

# 2. Compilar los artefactos de todos los microservicios:
mvn clean package -DskipTests

# 3. Levantar los contenedores del ecosistema:
docker compose up --build -d
```

### Verificación de contenedores activos:
```bash
docker compose ps
```

Deberás observar en estado `Up` o `healthy`:
* `activemq-artemis` (Puertos `61616`, `8161`)
* `config-server` (Puerto `8888`)
* `eureka-server` (Puerto `8761`)
* `auth-service` (Puerto `8084`)
* `bff-web` (Puerto `8081`)
* `bff-mobile` (Puerto `8082`)
* `bff-atm` (Puerto `8083`)

---

## 4. Paso 3: Dashboards y Monitoreo

Una vez levantados los servicios, puedes verificar su estado en tu navegador:

1. **Netflix Eureka (Service Discovery)**:
   * URL: [http://localhost:8761](http://localhost:8761)
   * Verifica que figuren registrados: `AUTH-SERVICE`, `BFF-WEB`, `BFF-MOBILE` y `BFF-ATM`.

2. **Consola Web de ActiveMQ Artemis (Broker JMS)**:
   * URL: [http://localhost:8161/console](http://localhost:8161/console)
   * Usuario: `admin` | Contraseña: `admin`
   * Verifica las colas: `banco.saga.transacciones.queue` y `banco.saga.compensaciones.queue`.

---

## 5. Paso 4: Pruebas Funcionales por Componente

### 5.1. Generación de Tokens de Acceso (auth-service: 8084)

Para consumir los BFFs protegidos, generamos un token JWT indicando el rol requerido:

```bash
# Token con todos los roles para pruebas generales:
curl -X GET "http://localhost:8084/api/auth/token?usuario=ejecutivo_banco"

# Token específico para Canal Web (ROLE_WEB):
curl -X GET "http://localhost:8084/api/auth/token?usuario=cliente_web&rol=ROLE_WEB"

# Token específico para Canal Móvil (ROLE_MOBILE):
curl -X GET "http://localhost:8084/api/auth/token?usuario=cliente_movil&rol=ROLE_MOBILE"

# Token específico para Canal Cajero (ROLE_ATM):
curl -X GET "http://localhost:8084/api/auth/token?usuario=cajero_atm&rol=ROLE_ATM"
```

*Copia el valor del campo `"token"` de la respuesta JSON para usarlo como `<TOKEN_JWT>` en las siguientes peticiones.*

---

### 5.2. Pruebas BFF Web (bff-web: 8081)

Optimizado para navegadores, entrega respuestas completas e indicadores financieros:

```bash
# 1. Consulta de cuenta completa (requiere ROLE_WEB):
curl -X GET "http://localhost:8081/api/web/cuentas/1" \
  -H "Authorization: Bearer <TOKEN_JWT>"

# 2. Indicadores financieros y tipo de cambio:
curl -X GET "http://localhost:8081/api/web/indicadores"
```

---

### 5.3. Pruebas BFF Móvil (bff-mobile: 8082)

Optimizado para dispositivos móviles, entrega respuestas compactas y alertas push:

```bash
# 1. Consulta de saldo ligera (requiere ROLE_MOBILE):
curl -X GET "http://localhost:8082/api/mobile/cuentas/1" \
  -H "Authorization: Bearer <TOKEN_JWT>"

# 2. Ver notificaciones push recibidas por eventos de transacciones:
curl -X GET "http://localhost:8082/api/mobile/notificaciones" \
  -H "Authorization: Bearer <TOKEN_JWT>"
```

---

### 5.4. Pruebas BFF Cajeros Automáticos (bff-atm: 8083)

Interfaz especializada para operaciones críticas, retiros y validación de riesgo:

```bash
# 1. Consulta de saldo en cajero (requiere ROLE_ATM):
curl -X GET "http://localhost:8083/api/atm/cuentas/1" \
  -H "Authorization: Bearer <TOKEN_JWT>"

# 2. Validación de riesgo de retiro (Circuit Breaker con Resilience4j):
curl -X GET "http://localhost:8083/api/atm/cuentas/1/validar-riesgo?monto=50000" \
  -H "Authorization: Bearer <TOKEN_JWT>"

# 3. Retiro de efectivo con publicación de evento Saga asíncrono:
curl -X POST "http://localhost:8083/api/atm/cuentas/1/saga-retiro?monto=20000" \
  -H "Authorization: Bearer <TOKEN_JWT>"
```

*Al ejecutar el retiro con saga en el cajero ATM, puedes consultar `GET http://localhost:8082/api/mobile/notificaciones` en el BFF Móvil y comprobarás que recibió la notificación del retiro en tiempo real vía ActiveMQ Artemis.*

---

## 6. Paso 5: Detención del Ecosistema

Para detener todos los servicios y liberar los puertos:

```bash
docker compose down
```


