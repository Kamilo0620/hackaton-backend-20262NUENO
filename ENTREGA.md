# Entrega Hackathon DBP: Tuckersoft Branch Engine

## Estado de Evaluación
Las 5 estrellas completadas y verificadas con la suite `autotests`:
- ★1 SEGURIDAD
- ★2 NODOS
- ★3 PARTIDAS
- ★4 DECISIONES
- ★5 ASINCRONIA

## Explicación del Flujo Asíncrono
1. `POST /api/v1/decisions` procesa la decisión en una transacción `@Transactional` sincrónica.
2. Si la entrada es válida y no corrupta, actualiza el estado de la partida, persiste la entidad `Decision` con estado `REGISTRADA` y publica un `DecisionCommittedEvent`.
3. El controlador retorna HTTP 201 de forma inmediata.
4. Tras confirmarse el `COMMIT` en PostgreSQL, el listener `BranchNotificationListener` se activa mediante `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` y `@Async("branchExecutor")` con pool dedicado `branch-worker-X`.
5. En una transacción independiente (`REQUIRES_NEW`), actualiza la decisión a `PROCESANDO`, construye el cuerpo del correo con los datos auditados y envía el mensaje con `JavaMailSender`.
6. Si el envío es exitoso, la decisión pasa a `ESTABILIZADA` y se registra un `RealityLog` con estado `SENT`. Si falla o se simula fallo con `X-Bandersnatch-Simulate: MAIL_FAILURE`, pasa a `ERROR` y se registra `RealityLog` con estado `FAILED`.
