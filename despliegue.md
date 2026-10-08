# ☁️ Guía de Despliegue en Entorno Cloud (AWS) - Banco XYZ
## Evaluación Final Transversal (EFT) - Desarrollo Backend III (PBY2203)

Este documento describe el procedimiento general para desplegar y poner en funcionamiento el ecosistema de microservicios del **Banco XYZ** en un entorno de infraestructura en la nube (Amazon Web Services - AWS), aprovechando **Docker** y **Docker Compose** para garantizar portabilidad y escalabilidad horizontal.

---

## 1. Arquitectura de Despliegue en la Nube

Para el despliegue en AWS se contempla la siguiente topología de referencia:

* **Cómputo**: Instancia virtual **AWS EC2** (Ubuntu Linux 22.04 LTS, tipo `t3.medium` o superior con 4GB+ RAM).
* **Base de Datos**: Instancia **Amazon RDS (MySQL 8.0)** o contenedor MySQL en red interna para la persistencia de `banco_xyz_db`.
* **Orquestación**: **Docker Compose** para la gestión y aislamiento de los contenedores en la red `banco-network`.
* **Seguridad perimetral (Security Groups)**:
  * Puerto `22`: SSH para administración del servidor.
  * Puerto `8761`: Eureka Dashboard (Service Discovery).
  * Puerto `8888`: Spring Cloud Config Server.
  * Puerto `8084`: Auth Service (OAuth2 / JWT).
  * Puertos `8081`, `8082`, `8083`: Canales BFF Web, BFF Móvil y BFF Cajeros Automáticos.
  * Puerto `8161`: Consola Web de ActiveMQ Artemis.

---

## 2. Paso 1: Aprovisionamiento del Servidor Cloud

1. Iniciar una instancia **EC2** en la consola de AWS con Amazon Linux o Ubuntu Server.
2. Configurar el **Security Group** con las reglas de entrada para los puertos del ecosistema (`8081-8084`, `8761`, `8888`, `8161`).
3. Conectarse a la instancia mediante SSH:
   ```bash
   ssh -i "clave-banco-xyz.pem" ubuntu@<IP_PUBLICA_EC2>
   ```

---

## 3. Paso 2: Instalación de Dependencias en la Instancia

Instalar el motor de Docker y herramientas de desarrollo:

```bash
# Actualizar repositorios e instalar paquetes base:
sudo apt update && sudo apt install -y git openjdk-17-jdk maven docker.io docker-compose-v2

# Habilitar permisos de Docker para el usuario:
sudo usermod -aG docker $USER
newgrp docker

# Verificar instalación:
docker --version
docker compose version
```

---

## 4. Paso 3: Configuración del Proyecto en el Servidor

1. Clonar el repositorio del proyecto desde GitHub:
   ```bash
   git clone <LINK_DEL_REPOSITORIO_GITHUB>
   cd proyecto_eft_banco
   ```

2. Configurar el archivo de variables de entorno `.env` para producción:
   ```bash
   cp .env.example .env
   ```

3. Ajustar los valores en `.env` (credenciales de GitHub OAuth2, clave secreta JWT y conexión a la base de datos MySQL en la nube):
   ```env
   GITHUB_CLIENT_ID=tu_github_client_id_produccion
   GITHUB_CLIENT_SECRET=tu_github_client_secret_produccion
   JWT_SECRET=BancoXYZBffSecretKey2026SecureJwtTokenKeyMustBe256BitsLong!
   AUTH_PORT=8084
   DB_USERNAME=admin
   DB_PASSWORD=PasswordSeguroCloud2026!
   ```

---

## 5. Paso 4: Compilación y Lanzamiento de Contenedores

1. **Compilar los paquetes JAR** de todos los microservicios:
   ```bash
   mvn clean package -DskipTests
   ```

2. **Ejecutar el proceso Batch** para la ingesta inicial de datos legacy en la base de datos:
   ```bash
   cd batch-service && mvn spring-boot:run && cd ..
   ```

3. **Construir las imágenes y levantar el ecosistema completo en segundo plano**:
   ```bash
   docker compose up --build -d
   ```

4. **Verificar el estado de salud de los servicios**:
   ```bash
   docker compose ps
   ```

---

## 6. Paso 5: Verificación del Despliegue en la Nube

Una vez en ejecución, los servicios están disponibles a través de la IP pública o DNS público asignado por AWS:

* **Panel Eureka**: `http://<IP_PUBLICA_EC2>:8761`
* **Servicio de Autenticación**: `http://<IP_PUBLICA_EC2>:8084`
* **BFF Web**: `http://<IP_PUBLICA_EC2>:8081/api/web/indicadores`
* **BFF Móvil**: `http://<IP_PUBLICA_EC2>:8082/api/mobile/cuentas/1`
* **BFF Cajeros Automáticos**: `http://<IP_PUBLICA_EC2>:8083/api/atm/cuentas/1`
* **Consola ActiveMQ**: `http://<IP_PUBLICA_EC2>:8161`

---

## 7. Paso 6: Escalabilidad Horizontal

Una de las grandes ventajas de esta arquitectura basada en microservicios y Spring Cloud es la capacidad de escalar instancias de forma elástica según la demanda de los usuarios:

```bash
# Escalar el canal Web a 2 instancias:
docker compose up -d --scale bff-web=2

# Escalar el canal Móvil a 3 instancias para eventos de alta concurrencia:
docker compose up -d --scale bff-mobile=3
```

* **Resultado**: Las nuevas instancias se registrarán automáticamente en el **Eureka Server**, el cual distribuirá la carga entre todas las réplicas activas sin tiempo de inactividad.

---

## 8. Paso 7: Mantenimiento y Detención

* **Ver logs en tiempo real**:
  ```bash
  docker compose logs -f bff-web
  ```

* **Reiniciar un servicio específico tras una actualización**:
  ```bash
  docker compose restart bff-atm
  ```

* **Detener el entorno completo**:
  ```bash
  docker compose down
  ```

