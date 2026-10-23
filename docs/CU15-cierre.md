CU-15 — Bitácora de Movimientos de Inventario quedó funcional y verificada el 23/10/2026. Se inspeccionaron `git status`, `git diff` y los archivos del trabajo parcial antes de editar. Se conservaron los cambios locales, `cu15-wip.patch` y `cu15-wip-status.txt`. No se hizo commit, push ni descarte de cambios.

La validación final se repitió el 23/10/2026 y terminó a las 07:40:16 (UTC−06:00): 244 pruebas ejecutadas y aprobadas (233 Java y 11 JavaScript), sin fallos, errores ni omisiones. El empaquetado y la compilación de las 231 fuentes en un directorio nuevo finalizaron con `BUILD SUCCESS`. No fue necesario corregir funcionalidad ni pruebas. En esta validación solo se actualizó este reporte; los archivos de código, pruebas y respaldos conservaron su contenido.

Los archivos modificados o creados en el trabajo acumulado de CU-15 son:

| Archivo | Resultado |
| --- | --- |
| [MovimientoInventarioController.java](../src/main/java/com/example/proyecto_semestre_medico/controller/MovimientoInventarioController.java) | Alerta bajo mínimo y resumen con una sola consulta de movimientos; conserva filtros, detalle, validación de binding y operaciones lógicas del trabajo parcial. |
| [MovimientoInventarioServiceImpl.java](../src/main/java/com/example/proyecto_semestre_medico/service/impl/MovimientoInventarioServiceImpl.java) | Reglas por tipo, mensaje de stock insuficiente, identidad autenticada también en despacho automático y resumen con acumuladores `long`. |
| [ResumenMensualInventarioDTO.java](../src/main/java/com/example/proyecto_semestre_medico/dto/ResumenMensualInventarioDTO.java) | Totales sin desbordamiento de `int` y registros utilizados para calcular y presentar el resumen. |
| [lista.html](../src/main/resources/templates/farmacia/inventario/lista.html) | Acciones visibles según rol y permisos administrativos; conserva filtros, paginación y columnas de auditoría. |
| [nuevo.html](../src/main/resources/templates/farmacia/inventario/nuevo.html) | Reglas condicionales, límites numéricos y proyección con alertas tanto para entradas como para salidas. |
| [resumen.html](../src/main/resources/templates/farmacia/inventario/resumen.html) | Aclara unidades e inclusión de registros inactivos; muestra costo, referencia, usuario, estado y acceso al detalle. |
| [InventarioCu15Tests.java](../src/test/java/com/example/proyecto_semestre_medico/InventarioCu15Tests.java) | Continúa y amplía las pruebas parciales: 73 casos. |
| [FarmaciaCu11Tests.java](../src/test/java/com/example/proyecto_semestre_medico/FarmaciaCu11Tests.java) | Datos de prueba con farmacéutico autenticado y rol activo; mantiene la comprobación explícita de rechazo anónimo. |
| [inventario-cu15-form.test.cjs](../src/test/js/inventario-cu15-form.test.cjs) | Nuevo: 11 pruebas del script real del formulario, sin dependencias adicionales. |
| [CU15-cierre.md](CU15-cierre.md) | Nuevo: este reporte. |

También permanecen en el diff los siguientes archivos del trabajo parcial, cuyo contenido se conservó durante esta continuación:

- [FiltroMovimientoInventarioDTO.java](../src/main/java/com/example/proyecto_semestre_medico/dto/FiltroMovimientoInventarioDTO.java).
- [MovimientoInventarioMedicamentoRepository.java](../src/main/java/com/example/proyecto_semestre_medico/repository/MovimientoInventarioMedicamentoRepository.java).
- [MovimientoInventarioService.java](../src/main/java/com/example/proyecto_semestre_medico/service/MovimientoInventarioService.java).
- [detalle.html](../src/main/resources/templates/farmacia/inventario/detalle.html).
- [farmacia/recetas/lista.html](../src/main/resources/templates/farmacia/recetas/lista.html).
- [CatalogosCu14Tests.java](../src/test/java/com/example/proyecto_semestre_medico/CatalogosCu14Tests.java).
- [FarmaciaTransaccionCu11Tests.java](../src/test/java/com/example/proyecto_semestre_medico/FarmaciaTransaccionCu11Tests.java).

La pantalla principal permite consultar, filtrar por inventario, medicamento, sucursal, tipo, referencia, usuario, fechas y estado, paginar, ver detalle, registrar movimientos y desactivar/reactivar. Presenta stock anterior/nuevo, cantidad, costo, referencia, usuario y fecha/hora. El detalle conserva los datos históricos y explica cómo corregir existencias mediante movimientos compensatorios.

Las reglas finales son:

| Tipo | Dirección | Motivo/notas | Costo unitario |
| --- | --- | --- | --- |
| COMPRA | Entrada | Opcional, máximo 500 caracteres | Obligatorio, mayor que cero, máximo dos decimales y capacidad `numeric(10,2)` |
| DEVOLUCION | Entrada | Obligatorio, máximo 500; sin mínimo de 10 | No aplica |
| VENTA | Salida | Oculto; no se guardan notas enviadas desde campos ocultos | No aplica |
| RECLAMO | Salida | Obligatorio, máximo 500; sin mínimo de 10 | No aplica |
| AJUSTE_POSITIVO | Entrada | Obligatorio, entre 10 y 500 | No aplica |
| AJUSTE_NEGATIVO | Salida | Obligatorio, entre 10 y 500 | No aplica |
| DESPACHO | Salida automática desde CU-11 | Conserva el flujo automático existente | No aplica |

La cantidad es obligatoria y entera positiva; se rechazan decimales, valores no numéricos, cero, negativos y valores fuera de capacidad. Las salidas manuales rechazan cantidades superiores a las existencias con el mensaje literal: `Stock insuficiente. El stock actual es [X] unidades.` Se impide desbordar el stock al sumar entradas. La referencia tiene un máximo de 100 caracteres. Los motivos se normalizan quitando espacios exteriores.

El formulario presenta stock actual, mínimo y proyectado, además de Entrada/Salida. Advierte cuando la proyección queda por debajo del mínimo, incluso en una entrada que todavía no alcance ese mínimo. La advertencia no bloquea movimientos válidos. Informa stock insuficiente y desbordamiento sin truncar cantidades decimales para calcular una proyección engañosa.

La concurrencia conserva `@Version` de `InventarioMedicamento` y `saveAndFlush` antes de persistir el movimiento. Si dos transacciones parten de la misma versión, una falla y se revierte íntegramente; el formulario permite revisar el stock y reintentar. Permanecen intactos el bloqueo pesimista de recetas de CU-11 y el bloqueo administrativo de `ControlCatalogo` en CU-14. No se sustituyó ni debilitó ninguno de esos mecanismos.

Stock, movimiento y auditoría se escriben en la misma transacción. Las pruebas inducen fallos independientes en las tres persistencias y verifican rollback tanto para movimientos manuales como para despachos. También comprueban competencia manual/manual, despacho/despacho y manual/despacho: una sola salida obtiene el mismo stock. La desactivación/reactivación solo altera la bandera lógica y registra auditoría; no repone, descuenta ni reaplica existencias, incluidos los despachos. No hay eliminación física desde CU-15.

Se mantiene la autorización del proyecto: FARMACEUTICO o ADMINISTRADOR, con permisos `CATALOG_VIEW`, `CATALOG_UPDATE` y `CATALOG_DISABLE` en las rutas administrativas correspondientes. Se verifican usuario y rol activos y su correspondencia con la autenticación. Los movimientos y auditorías utilizan al usuario autenticado real. Los campos de stock, usuario, estado y fecha enviados por HTTP no son editables desde el formulario.

El resumen mensual reutiliza servicio, DTO y vista existentes. Calcula número de movimientos, unidades de entrada/salida, unidades de los siete tipos y valor de compras. Usa inicio del mes inclusivo y del mes siguiente exclusivo. Incluye registros inactivos porque su efecto físico permanece. Los totales y el listado provienen de los mismos registros consultados, y las cantidades acumuladas usan `long` para soportar más de 2.147.483.647 unidades entre varios movimientos.

La nueva ejecución de verificación final produjo:

| Verificación | Resultado |
| --- | --- |
| InventarioCu15Tests | 73 aprobadas |
| FarmaciaCu11Tests | 36 aprobadas |
| FarmaciaTransaccionCu11Tests | 7 aprobadas |
| CatalogosCu14Tests | 117 aprobadas |
| Total Maven seleccionado | 233 aprobadas; 0 fallos, 0 errores, 0 omitidas |
| JavaScript del formulario | 11 aprobadas |
| Total de pruebas Java y JavaScript | 244 ejecutadas y aprobadas; 0 fallos, 0 errores, 0 omitidas |
| Maven `package` | BUILD SUCCESS a las 07:39:18; JAR ejecutable generado (58.582.699 bytes) |
| Compilación en directorio nuevo | 231 fuentes, Java 17; BUILD SUCCESS a las 07:40:16 |
| `git diff --check` | Sin errores |

Los comandos de reproducción equivalentes son:

```powershell
mvn -o -B -Dmaven.repo.local=target/cu15-maven-repository '-Dtest=InventarioCu15Tests,FarmaciaCu11Tests,FarmaciaTransaccionCu11Tests,CatalogosCu14Tests' package
node --test src/test/js/inventario-cu15-form.test.cjs
mvn -o -B -Dmaven.repo.local=target/cu15-maven-repository -Dmaven.compiler.outputDirectory=target/cu15-validacion-final-classes compile
git diff --check
```

En este equipo `mvn` se invocó mediante la distribución 3.9.16 ya instalada en `.m2/wrapper/dists`; la sesión anterior documentó un fallo de `mvnw.cmd` antes de arrancar Maven. Se reutilizó la caché existente en `target/cu15-maven-repository`, en modo offline y sin descargas. El primer intento restringido de esta validación falló al acceder al JAR local `slf4j-api-2.0.18.jar`, con `Cannot close compiler resources`, antes de ejecutar pruebas. El reintento autorizado fuera del sandbox completó las pruebas y el empaquetado. Como ese reintento reutilizó clases existentes, se compiló además en `target/cu15-validacion-final-classes`, donde Maven recompiló las 231 fuentes correctamente. No se modificaron wrapper, dependencias ni configuración para eludir el problema del entorno.

Los registros de esta ejecución están en [cu15-validacion-final-maven.log](../target/cu15-validacion-final-maven.log), [cu15-validacion-final-js.log](../target/cu15-validacion-final-js.log) y [cu15-validacion-final-compilacion.log](../target/cu15-validacion-final-compilacion.log). Los cuatro informes Java actualizados están en `target/surefire-reports`. El artefacto generado es `target/proyecto-semestre-medico-0.0.1-SNAPSHOT.jar`.

Las pruebas aprobadas confirman expresamente:

- `desactivarReactivarNoReescribeHistoricoNiStock` y `despachoAutomaticoUsaUsuarioRealYSePuedeFiltrarYDesactivarSinReponer`: desactivar/reactivar conserva el stock y los datos históricos, incluidos los movimientos DESPACHO.
- `tipoManualProhibidoPorHttp` rechaza DESPACHO manual. Las pruebas transaccionales de CU-11 comprueban que un despacho de receta genera automáticamente el movimiento DESPACHO con cantidad, usuario, fecha, referencia y stock anterior/nuevo correctos.
- `cantidadInvalida`, `cantidadManipuladaPorHttpConservaFormulario`, `salidasSinStockRechazadas` y `capacidadMonetariaYOverflowStock`: cantidades enteras positivas, rechazo de stock insuficiente y protección contra desbordamiento, sin persistencias parciales.
- `dosSalidasConcurrentesNoGastanMismoStock`, con los tres casos MANUAL, DESPACHO y MIXTO: una sola salida consume el stock compartido. CU-14 también comprueba versiones concurrentes y ausencia de duplicados de inventario; CU-11 comprueba que una receta simultánea se despacha una sola vez.
- `falloPersistenciaRevierteMovimientoStockYAuditoria`, con seis casos: los fallos de inventario, movimiento o auditoría revierten la transacción completa, tanto manual como automática.

Se revisaron los siete archivos nuevos sin seguimiento. Cinco son entregables de CU-15 para un futuro versionado:

| Archivo | Clasificación |
| --- | --- |
| `src/main/java/com/example/proyecto_semestre_medico/dto/FiltroMovimientoInventarioDTO.java` | DTO funcional de filtros |
| `src/main/resources/templates/farmacia/inventario/detalle.html` | Vista funcional de detalle |
| `src/test/java/com/example/proyecto_semestre_medico/InventarioCu15Tests.java` | Pruebas Java de CU-15 |
| `src/test/js/inventario-cu15-form.test.cjs` | Pruebas JavaScript de CU-15 |
| `docs/CU15-cierre.md` | Reporte de cierre actualizado |

Los otros dos, `cu15-wip.patch` y `cu15-wip-status.txt`, son respaldos locales del trabajo parcial: se conservan y NO deben versionarse. Todavía no tienen una regla de exclusión; no se modificó `.gitignore` ni el índice. Los logs, informes, caché Maven, clases y JAR en `target/` son artefactos locales ya ignorados por Git. `docs/CU09-auditoria.md` pertenece a CU-09 y ya tiene una exclusión específica; no forma parte de los nuevos entregables de CU-15.

Los 12 archivos con seguimiento modificados son el controlador, los DTO de resumen, el repositorio y la interfaz/implementación de movimientos; las vistas de lista, nuevo y resumen de inventario; el enlace desde recetas; y las tres suites relacionadas `CatalogosCu14Tests`, `FarmaciaCu11Tests` y `FarmaciaTransaccionCu11Tests`. Sus rutas y funciones están enumeradas en las tablas y enlaces anteriores. Ninguno necesitó cambios durante esta validación final.

Las contradicciones de requisitos quedaron resueltas con las decisiones proporcionadas: no se incorporó TRANSFERENCIA_ENTRE_SUCURSALES; DESPACHO se consulta pero no se registra manualmente; se aplican las reglas específicas de motivo en lugar de 10–1000 universal; se conserva la concurrencia existente en lugar de imponer otro esquema de RowVersion. No hay migraciones SQL ni cambios de estructura de entidades/base de datos. Maven conserva un aviso previo por la dependencia duplicada `thymeleaf-extras-springsecurity6` en el POM, ajeno a CU-15.

No quedan pendientes funcionales identificados de CU-15. El alcance verificado usa H2 aislada, MockMvc, renderizado Thymeleaf y pruebas del JavaScript con DOM mínimo; no se ejecutó una sesión de navegador real ni pruebas contra PostgreSQL externo. No se ejecutó la suite completa de todos los CUs, sino las suites de CU-15 y las relacionadas CU-11/CU-14.
