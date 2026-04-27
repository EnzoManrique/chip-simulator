# Contexto y Roadmap: Simulador de Póker (Backend)

## 🎲 Contexto del Proyecto (Project Idea)
Estamos construyendo el backend en Java (Spring Boot) para un **Simulador de Póker Texas Hold'em Presencial**. 
La idea es que un grupo de amigos reunidos físicamente usen sus celulares como **fichas virtuales** para apuestas, jugando con **cartas presenciales** (cada jugador tiene sus cartas en la mano). Por lo tanto, si el grupo no lleva un maletín con fichas físicas, pueden usar esta aplicación. La experiencia de usuario está adaptada a un entorno presencial (ej. no hay timers estrictos de turno, sino detección de desconexión).

## 📍 Estado Actual
- **Fase en Progreso:** Fase 3 - Robustez y Reglas de Negocio Edge
- **Objetivo Inmediato:** Manejo Global de Excepciones + Auto-Fold por desconexión

---

## 📋 Registro de Fases

### ✅ Fase 0: Motor del Juego y Arquitectura (COMPLETADA)
- [x] Lógica matemática del Texas Hold'em (Fases, Turnos, Ciegas).
- [x] Resolución de ganadores y Split Pots (Pozos divididos).
- [x] Refactorización a Arquitectura Limpia (Implementación del Patrón Orquestador para evitar dependencias circulares).

### ✅ Fase 1: Identidad y Seguridad (COMPLETA)
- [x] Crear Entidad `User` y `UserRepository`.
- [x] Implementar `AuthService` y `AuthController`.
- [x] Configurar Spring Security y `BCryptPasswordEncoder`.
- [x] Configurar `SecurityFilterChain` (Rutas públicas para `/api/auth/**`).

### ✅ Fase 2: Comunicación en Tiempo Real (COMPLETA)
- [x] Implementar WebSockets (STOMP) para actualizar la mesa en las pantallas sin recargar.
- [x] Crear WebSocketConfig con STOMP
- [x] Crear WebSocketNotificationService
- [x] Integrar notificaciones en GameOrchestratorService
- [x] Agregar campo currentPlayer en Room

### ⏳ Fase 3: Robustez y Reglas de Negocio Edge
- [ ] Manejo Global de Excepciones (@ControllerAdvice).
- [ ] Sistema de Auto-Fold por **desconexión de red** (No por límite de tiempo).

### ⏳ Fase 4: Frontend Mobile
- [ ] Integrar con frontend mobile (Android/iOS) para fichas virtuales.
- [ ] Sincronización en tiempo real con WebSockets.

### ⏳ Fase 5: Documentación
- [ ] Integrar Swagger / OpenAPI para facilitar la futura creación del Frontend.

---

## 🛡️ Reglas de Oro (Instrucciones para la IA)
> **Importante:** Gentle-AI / OpenCode debe leer y respetar estas reglas antes de escribir o modificar código.

1. **Seguridad:** - PROHIBIDO usar o sugerir JWT (JSON Web Tokens) en esta fase.
   - Las contraseñas SIEMPRE se encriptan con **BCrypt** antes de tocar MySQL.
   - NUNCA devolver el campo `password` (ni encriptado ni plano) en los JSON de respuesta.
2. **Arquitectura Core del Póker (Intocable):**
   - Respetar el patrón actual: Solo el `GameOrchestratorService` tiene la anotación `@Transactional` y es el único que hace llamadas de guardado (`save`) a la base de datos durante el juego.
   - Los sub-servicios (`BettingService`, `ShowdownService`, etc.) solo mutan entidades en memoria.
3. **Calidad de Código:**
   - Respetar el Principio de Responsabilidad Única (SRP).
   - Lógica de negocio va en el `Service`, exposición de endpoints en el `Controller`.
   - Antes de crear o modificar tablas, verificar la coherencia exacta entre la Entidad JPA, el DTO y la tabla real en MySQL.