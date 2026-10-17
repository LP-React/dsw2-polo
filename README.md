# BIBLIO-TEC

Repositorio multimódulo desarrollado con Java 17 y Spring Boot 3.5.16. Contiene dos
aplicaciones Spring Boot independientes:

- `bibliotec-api/`: backend REST, SOAP y WebSocket, puerto `8080`.
- `eureka-server/`: servidor de descubrimiento, puerto `8761`.

El `pom.xml` de la raíz solo agrega ambos módulos para compilarlos juntos. El frontend
Next.js se desarrolla como un proyecto separado.

La colección `postman/BIBLIO-TEC.postman_collection.json` contiene los flujos manuales
ordenados y guarda automáticamente los identificadores creados durante la ejecución.

## Servicios disponibles

### REST

- `POST /api/usuarios/registro`
- `POST /api/auth/login` (validación de credenciales HTTP Basic)
- `GET /api/usuarios/me`
- `GET /api/usuarios` (ADMIN)
- `GET /api/usuarios/{id}` (ADMIN)
- `PUT /api/usuarios/{id}` (ADMIN)
- `DELETE /api/usuarios/{id}` (ADMIN)
- CRUD `/api/libros`
- CRUD `/api/categorias`
- `GET /api/libros?titulo=spring&autor=perez&categoria=tecnologia`
- `POST /api/prestamos` (crea una solicitud `PENDIENTE`)
- `GET /api/prestamos/mis-prestamos`
- `GET /api/prestamos` (ADMIN)
- `PUT /api/prestamos/{id}/aprobar` (ADMIN)
- `PUT /api/prestamos/{id}/rechazar` (ADMIN)
- `PUT /api/prestamos/{id}/devolver`

Salvo el registro, los endpoints REST utilizan autenticación HTTP Basic.
Las consultas del catálogo están disponibles para cualquier usuario autenticado. Las
operaciones de administración de libros, categorías, usuarios y aprobación de préstamos
requieren el rol `ADMIN`.

Al iniciar por primera vez se crea un administrador para desarrollo:

- Usuario: `admin`
- Contraseña: `Admin12345`

En un despliegue se deben cambiar mediante `ADMIN_USERNAME` y `ADMIN_PASSWORD`.

## Flujo de préstamos

1. Un usuario solicita un libro y el préstamo queda `PENDIENTE`.
2. Un administrador aprueba o rechaza la solicitud.
3. Al aprobar, se descuenta una unidad y se fija el vencimiento a siete días.
4. Al devolver, se repone una unidad y el préstamo queda `DEVUELTO`.

No se permite solicitar un libro sin stock ni duplicar una solicitud o préstamo activo.

### SOAP

- WSDL: `http://localhost:8080/soap/libros.wsdl`
- Endpoint: `http://localhost:8080/soap`
- Operación: `consultarDisponibilidad`

Ejemplo del contenido del `Body` SOAP:

```xml
<lib:consultarDisponibilidadRequest xmlns:lib="http://bibliotec.com/soap/libros">
    <lib:idLibro>1</lib:idLibro>
</lib:consultarDisponibilidadRequest>
```

### Chat WebSocket

- Conexión STOMP: `ws://localhost:8080/chat-websocket`
- Envío de mensajes: `/app/chat.enviar`
- Suscripción: `/topic/chat`

Mensaje de ejemplo:

```json
{
  "usuario": "ana",
  "mensaje": "¿Está disponible el libro?"
}
```

### Eureka

El servidor de descubrimiento está en `eureka-server/` y utiliza el puerto `8761`.
BIBLIO-TEC se registra con el nombre `bibliotec`.

Para iniciar ambos procesos, abre dos terminales desde la raíz del repositorio.

Terminal 1:

```powershell
Set-Location eureka-server
..\mvnw.cmd spring-boot:run
```

Terminal 2:

```powershell
Set-Location bibliotec-api
..\mvnw.cmd spring-boot:run
```

Cada comando debe permanecer abierto en una terminal diferente. Desde VS Code también se
puede seleccionar `Servidor Eureka`, `Backend BIBLIO-TEC` o el arranque conjunto
`BIBLIO-TEC completo (Eureka + Backend)` en **Run and Debug**.

El panel de Eureka queda disponible en `http://localhost:8761`.

## Integración con Next.js

Por defecto se permite el origen `http://localhost:3000`. Para agregar otros orígenes,
separarlos por comas en la variable de entorno `CORS_ALLOWED_ORIGINS`.

Variables principales:

- `CORS_ALLOWED_ORIGINS`
- `EUREKA_ENABLED`
- `EUREKA_URL`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `DDL_AUTO`
- `PORT`
- `ADMIN_USERNAME`
- `ADMIN_PASSWORD`

Spring Boot carga las variables locales desde `bibliotec-api/.env`. Edita allí
`DB_USERNAME` y `DB_PASSWORD`. El archivo está ignorado por Git;
`bibliotec-api/.env.example` sirve como plantilla compartible.

## Empaquetado y despliegue

Ejecutar todas las pruebas y generar el JAR:

```powershell
.\mvnw.cmd clean package
```

También se incluye un `Dockerfile`. El contenedor recibe la configuración de base de datos,
CORS, Eureka y administrador mediante las variables anteriores. En una plataforma sin
Eureka se debe configurar `EUREKA_ENABLED=false`. El build se ejecuta usando la carpeta
del módulo como contexto:

```powershell
docker build -t bibliotec-backend .\bibliotec-api
docker run --rm -p 8080:8080 `
  -e DB_URL=jdbc:mysql://host.docker.internal:3306/bibliotec_db `
  -e DB_USERNAME=root `
  -e DB_PASSWORD=mysql `
  -e EUREKA_ENABLED=false `
  -e ADMIN_PASSWORD=UnaClaveSegura `
  bibliotec-backend
```
