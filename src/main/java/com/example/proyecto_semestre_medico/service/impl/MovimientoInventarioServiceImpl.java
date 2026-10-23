package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.dto.MovimientoInventarioDTO;
import com.example.proyecto_semestre_medico.model.Auditoria;
import com.example.proyecto_semestre_medico.model.InventarioMedicamento;
import com.example.proyecto_semestre_medico.model.MovimientoInventarioMedicamento;
import com.example.proyecto_semestre_medico.model.TipoMovimientoInventario;
import com.example.proyecto_semestre_medico.model.Usuario;
import com.example.proyecto_semestre_medico.repository.AuditoriaRepository;
import com.example.proyecto_semestre_medico.repository.InventarioMedicamentoRepository;
import com.example.proyecto_semestre_medico.repository.MovimientoInventarioMedicamentoRepository;
import com.example.proyecto_semestre_medico.repository.UsuarioRepository;
import com.example.proyecto_semestre_medico.service.MovimientoInventarioService;
import com.example.proyecto_semestre_medico.dto.ResumenMensualInventarioDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.time.YearMonth;

@Service
public class MovimientoInventarioServiceImpl
        implements MovimientoInventarioService {

    private final MovimientoInventarioMedicamentoRepository
            movimientoRepository;

    private final InventarioMedicamentoRepository
            inventarioRepository;

    private final UsuarioRepository
            usuarioRepository;

    private final AuditoriaRepository
            auditoriaRepository;

    public MovimientoInventarioServiceImpl(
            MovimientoInventarioMedicamentoRepository movimientoRepository,
            InventarioMedicamentoRepository inventarioRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaRepository auditoriaRepository
    ) {
        this.movimientoRepository =
                movimientoRepository;

        this.inventarioRepository =
                inventarioRepository;

        this.usuarioRepository =
                usuarioRepository;

        this.auditoriaRepository =
                auditoriaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoInventarioMedicamento>
    listarTodos() {
        validarAcceso();

        return movimientoRepository
                .findAllByOrderByFechaMovimientoDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoInventarioMedicamento>
    listarPorInventario(
            Long inventarioId
    ) {
        validarAcceso();

        if (inventarioId == null) {

            throw new IllegalArgumentException(
                    "Debe indicar un inventario."
            );
        }

        return movimientoRepository
                .findByInventarioIdOrderByFechaMovimientoDesc(
                        inventarioId
                );
    }

    @Override
    @Transactional
    public MovimientoInventarioMedicamento
    registrarMovimiento(
            MovimientoInventarioDTO dto,
            String nombreUsuario
    ) {
        Usuario usuario = validarOperador(nombreUsuario);

        validar(dto);

        InventarioMedicamento inventario =
                inventarioRepository
                        .findById(
                                dto.getInventarioId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Inventario no encontrado."
                                )
                        );

        if (!Boolean.TRUE.equals(
                inventario.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "No se pueden registrar movimientos en un inventario inactivo."
            );
        }

        if (!Boolean.TRUE.equals(
                inventario
                        .getMedicamento()
                        .getActivo()
        )) {

            throw new IllegalArgumentException(
                    "El medicamento se encuentra inactivo."
            );
        }

        if (!Boolean.TRUE.equals(
                inventario
                        .getSucursal()
                        .getActivo()
        )) {

            throw new IllegalArgumentException(
                    "La sucursal se encuentra inactiva."
            );
        }

        TipoMovimientoInventario tipo =
                dto.getTipoMovimiento();

        /*
         * DESPACHO solamente podrá ser generado
         * automáticamente por CU-11.
         */
        if (tipo ==
                TipoMovimientoInventario.DESPACHO) {

            throw new IllegalArgumentException(
                    "El movimiento DESPACHO no puede registrarse manualmente."
            );
        }

        int stockAnterior =
                inventario.getStockActual();

        int cantidad =
                dto.getCantidad();

        int stockNuevo;

        if (esEntrada(tipo)) {

            if (cantidad > Integer.MAX_VALUE - stockAnterior) {
                throw new IllegalArgumentException("El stock resultante excede la capacidad permitida.");
            }
            stockNuevo = stockAnterior + cantidad;

        } else {

            if (cantidad > stockAnterior) {

                throw new IllegalArgumentException(
                        "Stock insuficiente. El stock actual es "
                                + stockAnterior
                                + " unidades."
                );
            }

            stockNuevo =
                    stockAnterior
                            - cantidad;
        }

        /*
         * Primero actualizamos el inventario.
         *
         * Como InventarioMedicamento tiene @Version,
         * Hibernate comprobará que nadie haya
         * modificado el mismo registro concurrentemente.
         */
        inventario.setStockActual(
                stockNuevo
        );

        inventarioRepository.saveAndFlush(
                inventario
        );

        MovimientoInventarioMedicamento movimiento =
                new MovimientoInventarioMedicamento();

        movimiento.setInventario(
                inventario
        );

        movimiento.setTipoMovimiento(
                tipo
        );

        movimiento.setCantidad(
                cantidad
        );

        movimiento.setCostoUnitario(
                tipo == TipoMovimientoInventario.COMPRA
                        ? dto.getCostoUnitario()
                        : null
        );

        movimiento.setReferencia(
                normalizarOpcional(
                        dto.getReferencia()
                )
        );

        movimiento.setMotivo(
                obtenerMotivo(
                        tipo,
                        dto.getMotivo()
                )
        );

        movimiento.setStockAnterior(
                stockAnterior
        );

        movimiento.setStockNuevo(
                stockNuevo
        );

        movimiento.setUsuario(
                usuario
        );

        movimiento.setFechaMovimiento(
                LocalDateTime.now()
        );

        movimiento.setActivo(true);

        movimiento =
                movimientoRepository
                        .save(movimiento);

        registrarAuditoria(
                usuario,
                movimiento
        );

        return movimiento;
    }

    @Override
    @Transactional
    public MovimientoInventarioMedicamento
    registrarDespachoAutomatico(
            Long medicamentoId,
            Long sucursalId,
            Integer cantidad,
            String referencia,
            String motivo,
            String nombreUsuario
    ) {
        Usuario usuario = validarOperador(nombreUsuario);

        if (medicamentoId == null) {

            throw new IllegalArgumentException(
                    "Debe indicar el medicamento a despachar."
            );
        }

        if (sucursalId == null) {

            throw new IllegalArgumentException(
                    "Debe indicar la sucursal del despacho."
            );
        }

        if (cantidad == null
                || cantidad <= 0) {

            throw new IllegalArgumentException(
                    "La cantidad a despachar debe ser mayor que 0."
            );
        }

        validarReferencia(
                referencia
        );

        InventarioMedicamento inventario =
                inventarioRepository
                        .findByMedicamentoIdAndSucursalIdAndActivoTrue(
                                medicamentoId,
                                sucursalId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "No existe inventario activo para el medicamento seleccionado en esta sucursal."
                                )
                        );

        if (!Boolean.TRUE.equals(
                inventario
                        .getMedicamento()
                        .getActivo()
        )) {

            throw new IllegalArgumentException(
                    "El medicamento se encuentra inactivo."
            );
        }

        if (!Boolean.TRUE.equals(
                inventario
                        .getSucursal()
                        .getActivo()
        )) {

            throw new IllegalArgumentException(
                    "La sucursal se encuentra inactiva."
            );
        }

        int stockAnterior =
                inventario.getStockActual();

        if (cantidad > stockAnterior) {

            throw new IllegalArgumentException(
                    "Stock insuficiente. Disponible: "
                            + stockAnterior
                            + ", solicitado: "
                            + cantidad
                            + "."
            );
        }

        int stockNuevo =
                stockAnterior - cantidad;

        /*
         * InventarioMedicamento tiene @Version,
         * por lo que Hibernate protege contra
         * modificaciones concurrentes.
         */
        inventario.setStockActual(
                stockNuevo
        );

        inventarioRepository.saveAndFlush(
                inventario
        );

        MovimientoInventarioMedicamento movimiento =
                new MovimientoInventarioMedicamento();

        movimiento.setInventario(
                inventario
        );

        movimiento.setTipoMovimiento(
                TipoMovimientoInventario.DESPACHO
        );

        movimiento.setCantidad(
                cantidad
        );

        /*
         * DESPACHO no representa una compra,
         * por lo que no registra costo unitario.
         */
        movimiento.setCostoUnitario(
                null
        );

        movimiento.setReferencia(
                normalizarOpcional(
                        referencia
                )
        );

        movimiento.setMotivo(
                normalizarOpcional(
                        motivo
                )
        );

        movimiento.setStockAnterior(
                stockAnterior
        );

        movimiento.setStockNuevo(
                stockNuevo
        );

        movimiento.setUsuario(
                usuario
        );

        movimiento.setFechaMovimiento(
                LocalDateTime.now()
        );

        movimiento.setActivo(
                true
        );

        movimiento =
                movimientoRepository
                        .save(movimiento);

        registrarAuditoria(
                usuario,
                movimiento
        );

        return movimiento;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoInventarioMedicamento>
    listarPorMes(
            int anio,
            int mes
    ) {
        validarAcceso();

        YearMonth periodo =
                validarPeriodo(
                        anio,
                        mes
                );

        LocalDateTime desde =
                periodo
                        .atDay(1)
                        .atStartOfDay();

        LocalDateTime hasta =
                periodo
                        .plusMonths(1)
                        .atDay(1)
                        .atStartOfDay();

        return movimientoRepository
                .findByFechaMovimientoGreaterThanEqualAndFechaMovimientoLessThanOrderByFechaMovimientoDesc(
                        desde,
                        hasta
                );
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenMensualInventarioDTO
    obtenerResumenMensual(
            int anio,
            int mes
    ) {

        List<MovimientoInventarioMedicamento>
                movimientos =
                listarPorMes(
                        anio,
                        mes
                );

        ResumenMensualInventarioDTO resumen =
                new ResumenMensualInventarioDTO();

        resumen.setAnio(anio);
        resumen.setMes(mes);

        resumen.setTotalMovimientos(
                movimientos.size()
        );
        resumen.setMovimientos(movimientos);

        long entradas = 0;
        long salidas = 0;

        long compras = 0;
        long devoluciones = 0;
        long ventas = 0;
        long reclamos = 0;
        long ajustesPositivos = 0;
        long ajustesNegativos = 0;
        long despachos = 0;

        BigDecimal valorCompras =
                BigDecimal.ZERO;

        for (MovimientoInventarioMedicamento movimiento
                : movimientos) {

            int cantidad =
                    movimiento.getCantidad();

            switch (
                    movimiento.getTipoMovimiento()
            ) {

                case COMPRA -> {

                    entradas += cantidad;
                    compras += cantidad;

                    if (movimiento.getCostoUnitario()
                            != null) {

                        valorCompras =
                                valorCompras.add(
                                        movimiento
                                                .getCostoUnitario()
                                                .multiply(
                                                        BigDecimal.valueOf(
                                                                cantidad
                                                        )
                                                )
                                );
                    }
                }

                case DEVOLUCION -> {

                    entradas += cantidad;
                    devoluciones += cantidad;
                }

                case AJUSTE_POSITIVO -> {

                    entradas += cantidad;
                    ajustesPositivos += cantidad;
                }

                case VENTA -> {

                    salidas += cantidad;
                    ventas += cantidad;
                }

                case RECLAMO -> {

                    salidas += cantidad;
                    reclamos += cantidad;
                }

                case AJUSTE_NEGATIVO -> {

                    salidas += cantidad;
                    ajustesNegativos += cantidad;
                }

                case DESPACHO -> {

                    salidas += cantidad;
                    despachos += cantidad;
                }
            }
        }

        resumen.setTotalEntradas(
                entradas
        );

        resumen.setTotalSalidas(
                salidas
        );

        resumen.setCompras(
                compras
        );

        resumen.setDevoluciones(
                devoluciones
        );

        resumen.setVentas(
                ventas
        );

        resumen.setReclamos(
                reclamos
        );

        resumen.setAjustesPositivos(
                ajustesPositivos
        );

        resumen.setAjustesNegativos(
                ajustesNegativos
        );

        resumen.setDespachos(
                despachos
        );

        resumen.setValorTotalCompras(
                valorCompras
        );

        return resumen;
    }

    @Override
    @Transactional(readOnly = true)
    public MovimientoInventarioMedicamento buscarPorId(
            Long id
    ) {
        validarAcceso();

        return movimientoRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Movimiento de inventario no encontrado."
                        )
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoInventarioMedicamento> listarFiltrados(
            Long inventarioId,
            String tipoMovimiento,
            String estado
    ) {

        var filtro = new com.example.proyecto_semestre_medico.dto.FiltroMovimientoInventarioDTO();
        filtro.setInventarioId(inventarioId); filtro.setTipo(tipoMovimiento); filtro.setEstado(estado);
        validarAcceso();
        return movimientoRepository.findAll(especificacion(filtro),
                org.springframework.data.domain.Sort.by("fechaMovimiento", "id").descending());
    }

    @Override
    @Transactional
    public void desactivar(
            Long id,
            String nombreUsuario
    ) {
        validarOperador(nombreUsuario);

        MovimientoInventarioMedicamento movimiento =
                buscarPorId(id);

        if (!Boolean.TRUE.equals(
                movimiento.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "El movimiento ya está inactivo."
            );
        }

        /*
         * IMPORTANTE:
         * No modificamos InventarioMedicamento.
         *
         * El stock ya fue afectado cuando se registró
         * originalmente el movimiento.
         */
        movimiento.setActivo(false);

        movimientoRepository.save(
                movimiento
        );

        registrarAuditoriaEstado(
                nombreUsuario,
                movimiento,
                "DESACTIVAR_MOVIMIENTO_INVENTARIO",
                "Se desactivó lógicamente el movimiento "
                        + movimiento.getId()
                        + ". No se modificó el stock."
        );
    }

    @Override
    @Transactional
    public void reactivar(
            Long id,
            String nombreUsuario
    ) {
        validarOperador(nombreUsuario);

        MovimientoInventarioMedicamento movimiento =
                buscarPorId(id);

        if (Boolean.TRUE.equals(
                movimiento.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "El movimiento ya está activo."
            );
        }

        /*
         * Reactivar tampoco vuelve a sumar/restar stock.
         */
        movimiento.setActivo(true);

        movimientoRepository.save(
                movimiento
        );

        registrarAuditoriaEstado(
                nombreUsuario,
                movimiento,
                "REACTIVAR_MOVIMIENTO_INVENTARIO",
                "Se reactivó lógicamente el movimiento "
                        + movimiento.getId()
                        + ". No se modificó el stock."
        );
    }

    @Override
    @Transactional(readOnly = true)
    public void validarAcceso() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        validarOperador(auth == null ? null : auth.getName());
    }

    private Usuario validarOperador(String nombreEsperado) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || nombreEsperado == null || !auth.getName().equals(nombreEsperado))
            throw new org.springframework.security.access.AccessDeniedException("Usuario no autorizado para la bitácora.");
        Usuario usuario = buscarUsuario(auth.getName());
        if (usuario == null || !Boolean.TRUE.equals(usuario.getActivo()) || usuario.getRol() == null
                || !Boolean.TRUE.equals(usuario.getRol().getActivo())
                || !java.util.Set.of("ADMINISTRADOR", "FARMACEUTICO").contains(usuario.getRol().getCodigo())
                || auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_" + usuario.getRol().getCodigo())))
            throw new org.springframework.security.access.AccessDeniedException("La bitácora requiere un usuario interno activo.");
        return usuario;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<MovimientoInventarioMedicamento> buscar(
            com.example.proyecto_semestre_medico.dto.FiltroMovimientoInventarioDTO filtro, int pagina) {
        validarAcceso();
        return movimientoRepository.findAll(especificacion(filtro), org.springframework.data.domain.PageRequest.of(
                Math.max(0, pagina), 20, org.springframework.data.domain.Sort.by("fechaMovimiento", "id").descending()));
    }

    private org.springframework.data.jpa.domain.Specification<MovimientoInventarioMedicamento> especificacion(
            com.example.proyecto_semestre_medico.dto.FiltroMovimientoInventarioDTO f) {
        if (f.getDesde() != null && f.getHasta() != null && f.getDesde().isAfter(f.getHasta()))
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final.");
        if (f.getEstado() != null && !f.getEstado().isBlank()
                && !java.util.Set.of("ACTIVO", "INACTIVO", "TODOS").contains(f.getEstado().toUpperCase(java.util.Locale.ROOT)))
            throw new IllegalArgumentException("Debe seleccionar un estado válido.");
        TipoMovimientoInventario tipo = null;
        if (f.getTipo() != null && !f.getTipo().isBlank()) {
            try { tipo = TipoMovimientoInventario.valueOf(f.getTipo().trim().toUpperCase(java.util.Locale.ROOT)); }
            catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Debe seleccionar el tipo de movimiento."); }
        }
        final TipoMovimientoInventario tipoValidado = tipo;
        return (raiz, consulta, cb) -> {
            var filtros = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (f.getInventarioId() != null) filtros.add(cb.equal(raiz.get("inventario").get("id"), f.getInventarioId()));
            if (tipoValidado != null) filtros.add(cb.equal(raiz.get("tipoMovimiento"), tipoValidado));
            if ("ACTIVO".equalsIgnoreCase(f.getEstado())) filtros.add(cb.isTrue(raiz.get("activo")));
            if ("INACTIVO".equalsIgnoreCase(f.getEstado())) filtros.add(cb.isFalse(raiz.get("activo")));
            agregarTexto(filtros, cb, raiz.get("inventario").get("medicamento").get("nombre"), f.getMedicamento());
            agregarTexto(filtros, cb, raiz.get("inventario").get("sucursal").get("nombre"), f.getSucursal());
            agregarTexto(filtros, cb, raiz.get("referencia"), f.getReferencia());
            if (f.getUsuario() != null && !f.getUsuario().isBlank())
                agregarTexto(filtros, cb, raiz.join("usuario").get("nombreUsuario"), f.getUsuario());
            if (f.getDesde() != null) filtros.add(cb.greaterThanOrEqualTo(raiz.get("fechaMovimiento"), f.getDesde().atStartOfDay()));
            if (f.getHasta() != null) filtros.add(cb.lessThan(raiz.get("fechaMovimiento"), f.getHasta().plusDays(1).atStartOfDay()));
            return cb.and(filtros.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private void agregarTexto(java.util.List<jakarta.persistence.criteria.Predicate> filtros,
            jakarta.persistence.criteria.CriteriaBuilder cb, jakarta.persistence.criteria.Expression<String> campo, String valor) {
        if (valor != null && !valor.isBlank()) {
            String texto = valor.trim().toLowerCase(java.util.Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_");
            filtros.add(cb.like(cb.lower(campo), "%" + texto + "%", '!'));
        }
    }

    private YearMonth validarPeriodo(
            int anio,
            int mes
    ) {

        if (anio < 2000
                || anio > 2100) {

            throw new IllegalArgumentException(
                    "El año indicado no es válido."
            );
        }

        if (mes < 1
                || mes > 12) {

            throw new IllegalArgumentException(
                    "El mes debe estar entre 1 y 12."
            );
        }

        return YearMonth.of(
                anio,
                mes
        );
    }

    private void validar(
            MovimientoInventarioDTO dto
    ) {

        if (dto == null) {

            throw new IllegalArgumentException(
                    "Datos del movimiento requeridos."
            );
        }

        if (dto.getInventarioId() == null) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un medicamento. Debe seleccionar una sucursal."
            );
        }

        if (dto.getTipoMovimiento() == null) {

            throw new IllegalArgumentException(
                    "Debe seleccionar el tipo de movimiento."
            );
        }

        if (dto.getCantidad() == null
                || dto.getCantidad() <= 0) {

            throw new IllegalArgumentException(
                    "La cantidad debe ser un número entero positivo."
            );
        }

        validarReferencia(
                dto.getReferencia()
        );

        validarReglasPorTipo(dto);
    }

    private void validarReglasPorTipo(
            MovimientoInventarioDTO dto
    ) {

        TipoMovimientoInventario tipo =
                dto.getTipoMovimiento();

        /*
         * COMPRA
         */
        if (tipo ==
                TipoMovimientoInventario.COMPRA) {

            BigDecimal costo =
                    dto.getCostoUnitario();

            if (costo == null) throw new IllegalArgumentException("El costo unitario es obligatorio para compras");
            if (costo.signum() <= 0) throw new IllegalArgumentException("El costo unitario debe ser mayor a 0");
            if (costo.scale() > 2) throw new IllegalArgumentException("El costo unitario debe tener máximo 2 decimales");
            if (costo.compareTo(new BigDecimal("99999999.99")) > 0)
                throw new IllegalArgumentException("El costo unitario excede la capacidad permitida (10,2).");
        }
        if (tipo != TipoMovimientoInventario.VENTA && dto.getMotivo() != null && dto.getMotivo().trim().length() > 500)
            throw new IllegalArgumentException(tipo == TipoMovimientoInventario.AJUSTE_POSITIVO
                    || tipo == TipoMovimientoInventario.AJUSTE_NEGATIVO
                    ? "El motivo debe contener entre 10 y 500 caracteres."
                    : "Las notas o el motivo no pueden exceder 500 caracteres.");

        /*
         * Para cualquier movimiento distinto
         * de COMPRA ignoraremos costoUnitario.
         */

        switch (tipo) {

            case AJUSTE_POSITIVO,
                 AJUSTE_NEGATIVO ->

                    validarMotivoAjuste(dto.getMotivo());

            case DEVOLUCION ->

                    validarMotivoObligatorio(
                            dto.getMotivo(),
                            "Debe indicar el motivo de la devolución."
                    );

            case RECLAMO ->

                    validarMotivoObligatorio(
                            dto.getMotivo(),
                            "Debe indicar el motivo del reclamo."
                    );

            case DESPACHO ->

                    throw new IllegalArgumentException(
                            "DESPACHO solamente puede ser generado automáticamente por el sistema."
                    );

            default -> {
                /*
                 * COMPRA y VENTA no requieren motivo.
                 */
            }
        }
    }

    private void validarMotivoObligatorio(
            String motivo,
            String mensaje
    ) {

        if (motivo == null
                || motivo.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    mensaje
            );
        }
    }

    private void validarMotivoAjuste(String motivo) {
        if (motivo == null || motivo.trim().length() < 10 || motivo.trim().length() > 500) {

            throw new IllegalArgumentException(
                    "El motivo debe contener entre 10 y 500 caracteres."
            );
        }
    }

    private void validarReferencia(
            String referencia
    ) {

        if (referencia != null
                && referencia.trim().length() > 100) {

            throw new IllegalArgumentException(
                    "La referencia no puede exceder 100 caracteres."
            );
        }
    }

    private boolean esEntrada(
            TipoMovimientoInventario tipo
    ) {

        return tipo ==
                TipoMovimientoInventario.COMPRA

                || tipo ==
                TipoMovimientoInventario.DEVOLUCION

                || tipo ==
                TipoMovimientoInventario.AJUSTE_POSITIVO;
    }

    private String obtenerMotivo(
            TipoMovimientoInventario tipo,
            String motivo
    ) {

        return tipo == TipoMovimientoInventario.VENTA ? null : normalizarOpcional(motivo);
    }

    private String normalizarOpcional(
            String valor
    ) {

        if (valor == null
                || valor.isBlank()) {

            return null;
        }

        return valor.trim();
    }

    private Usuario buscarUsuario(
            String nombreUsuario
    ) {

        if (nombreUsuario == null
                || nombreUsuario.isBlank()) {

            return null;
        }

        return usuarioRepository
                .findByNombreUsuarioIgnoreCase(
                        nombreUsuario
                )
                .orElse(null);
    }

    private void registrarAuditoria(
            Usuario usuario,
            MovimientoInventarioMedicamento movimiento
    ) {

        Auditoria auditoria =
                new Auditoria();

        auditoria.setUsuario(
                usuario
        );

        auditoria.setAccion(
                "REGISTRAR_MOVIMIENTO_INVENTARIO"
        );

        auditoria.setTipoEntidad(
                "MOVIMIENTO_INVENTARIO_MEDICAMENTO"
        );

        auditoria.setEntidadId(
                movimiento
                        .getId()
                        .toString()
        );

        String detalle =
                "Movimiento "
                        + movimiento
                        .getTipoMovimiento()
                        .name()
                        + ". Medicamento: "
                        + movimiento
                        .getInventario()
                        .getMedicamento()
                        .getNombre()
                        + ". Sucursal: "
                        + movimiento
                        .getInventario()
                        .getSucursal()
                        .getNombre()
                        + ". Cantidad: "
                        + movimiento
                        .getCantidad()
                        + ". Stock anterior: "
                        + movimiento
                        .getStockAnterior()
                        + ". Stock nuevo: "
                        + movimiento
                        .getStockNuevo()
                        + ".";

        auditoria.setDetalle(
                detalle
        );

        auditoriaRepository.save(
                auditoria
        );
    }

    private void registrarAuditoriaEstado(
            String nombreUsuario,
            MovimientoInventarioMedicamento movimiento,
            String accion,
            String detalle
    ) {

        Usuario usuario =
                buscarUsuario(
                        nombreUsuario
                );

        Auditoria auditoria =
                new Auditoria();

        auditoria.setUsuario(
                usuario
        );

        auditoria.setAccion(
                accion
        );

        auditoria.setTipoEntidad(
                "MOVIMIENTO_INVENTARIO_MEDICAMENTO"
        );

        auditoria.setEntidadId(
                movimiento
                        .getId()
                        .toString()
        );

        auditoria.setDetalle(
                detalle
        );

        auditoriaRepository.save(
                auditoria
        );
    }
}
