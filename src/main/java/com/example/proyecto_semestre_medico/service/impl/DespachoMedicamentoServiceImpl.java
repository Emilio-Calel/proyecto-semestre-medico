package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.dto.DespachoDetalleDTO;
import com.example.proyecto_semestre_medico.dto.DespachoMedicamentoDTO;
import com.example.proyecto_semestre_medico.dto.MedicamentoRecetaFarmaciaDTO;
import com.example.proyecto_semestre_medico.model.*;
import com.example.proyecto_semestre_medico.repository.*;
import com.example.proyecto_semestre_medico.service.DespachoMedicamentoService;
import com.example.proyecto_semestre_medico.service.MovimientoInventarioService;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;

@Service
@PreAuthorize("hasRole('FARMACEUTICO') or hasRole('ADMINISTRADOR')")
public class DespachoMedicamentoServiceImpl
        implements DespachoMedicamentoService {

    private final DespachoMedicamentoRepository
            despachoRepository;

    private final RecetaRepository
            recetaRepository;

    private final RecetaDetalleRepository
            recetaDetalleRepository;

    private final SucursalRepository
            sucursalRepository;

    private final MedicamentoRepository
            medicamentoRepository;

    private final InventarioMedicamentoRepository
            inventarioRepository;

    private final UsuarioRepository
            usuarioRepository;

    private final AuditoriaRepository
            auditoriaRepository;

    private final MovimientoInventarioService
            movimientoInventarioService;

    private final NotificacionInternaRepository notificacionRepository;

    private static final String RECETA_INVALIDA = "La receta es inválida. Verifique que el medicamento exista, la dosis sea correcta y la receta no sea anterior a 7 días.";

    public DespachoMedicamentoServiceImpl(
            DespachoMedicamentoRepository despachoRepository,
            RecetaRepository recetaRepository,
            RecetaDetalleRepository recetaDetalleRepository,
            SucursalRepository sucursalRepository,
            MedicamentoRepository medicamentoRepository,
            InventarioMedicamentoRepository inventarioRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaRepository auditoriaRepository,
            MovimientoInventarioService movimientoInventarioService,
            NotificacionInternaRepository notificacionRepository
    ) {

        this.despachoRepository =
                despachoRepository;

        this.recetaRepository =
                recetaRepository;

        this.recetaDetalleRepository =
                recetaDetalleRepository;

        this.sucursalRepository =
                sucursalRepository;

        this.medicamentoRepository =
                medicamentoRepository;

        this.inventarioRepository =
                inventarioRepository;

        this.usuarioRepository =
                usuarioRepository;

        this.auditoriaRepository =
                auditoriaRepository;

        this.movimientoInventarioService =
                movimientoInventarioService;
        this.notificacionRepository = notificacionRepository;
    }

    /*
     * =========================================================
     * DETALLE DE RECETA PARA FARMACIA
     * =========================================================
     */

    @Override
    @Transactional(readOnly = true)
    public List<MedicamentoRecetaFarmaciaDTO>
    obtenerDetalleParaDespacho(
            Long recetaId,
            Long sucursalId
    ) {

        if (recetaId == null) {

            throw new IllegalArgumentException(
                    "Debe indicar la receta."
            );
        }

        if (sucursalId == null) {

            throw new IllegalArgumentException(
                    "Debe indicar la sucursal."
            );
        }

        Receta receta =
                recetaRepository
                        .findById(recetaId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Receta no encontrada."
                                )
                        );

        validarRecetaParaDespacho(
                receta
        );

        Sucursal sucursal =
                sucursalRepository
                        .findById(sucursalId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Sucursal no encontrada."
                                )
                        );

        if (!Boolean.TRUE.equals(
                sucursal.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "La sucursal seleccionada está inactiva."
            );
        }

        List<RecetaDetalle> detalles =
                recetaDetalleRepository
                        .findByRecetaIdAndActivoTrueOrderByIdAsc(
                                recetaId
                        );

        validarContenidoReceta(detalles);

        List<MedicamentoRecetaFarmaciaDTO>
                resultado =
                new ArrayList<>();

        for (RecetaDetalle detalle
                : detalles) {

            Medicamento medicamento =
                    detalle.getMedicamento();

            int cantidadRecetada =
                    detalle.getCantidad();

            int cantidadDespachada =
                    detalle.getCantidadDespachada() == null
                            ? 0
                            : detalle.getCantidadDespachada();

            int cantidadPendiente =
                    Math.max(
                            cantidadRecetada
                                    - cantidadDespachada,
                            0
                    );

            var registroInventario = inventarioRepository
                            .findByMedicamentoIdAndSucursalIdAndActivoTrue(
                                    medicamento.getId(),
                                    sucursalId
                            );
            int stockDisponible = registroInventario
                            .map(
                                    InventarioMedicamento::getStockActual
                            )
                            .orElse(0);

            MedicamentoRecetaFarmaciaDTO dto =
                    new MedicamentoRecetaFarmaciaDTO();

            dto.setInventarioRegistrado(registroInventario.isPresent());

            dto.setRecetaDetalleId(
                    detalle.getId()
            );

            dto.setMedicamentoId(
                    medicamento.getId()
            );

            dto.setMedicamento(
                    medicamento.getNombre()
            );

            dto.setDosis(
                    detalle.getDosis()
            );

            dto.setIndicaciones(
                    detalle.getIndicaciones()
            );

            dto.setUnidad(
                    medicamento.getUnidad()
            );

            dto.setCantidadRecetada(
                    cantidadRecetada
            );

            dto.setCantidadDespachada(
                    cantidadDespachada
            );

            dto.setCantidadPendiente(
                    cantidadPendiente
            );

            dto.setStockDisponible(
                    stockDisponible
            );

            dto.setStockMinimo(
                    medicamento.getStockMinimo()
            );

            dto.setPrecioUnitario(
                    medicamento.getPrecio()
            );

            resultado.add(
                    dto
            );
        }

        return resultado;
    }

    /*
     * =========================================================
     * REGISTRAR DESPACHO
     * =========================================================
     */

    @Override
    @Transactional
    @PreAuthorize("(hasRole('FARMACEUTICO') or hasRole('ADMINISTRADOR')) and #nombreUsuario == authentication.name")
    public DespachoMedicamento registrarDespacho(
            DespachoMedicamentoDTO dto,
            String nombreUsuario
    ) {

        validarDTO(
                dto
        );

        Receta receta =
                recetaRepository
                        .buscarParaDespacho(dto.getRecetaId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Receta no encontrada."
                                )
                        );

        validarRecetaParaDespacho(
                receta
        );

        validarContenidoReceta(recetaDetalleRepository.findByRecetaIdAndActivoTrueOrderByIdAsc(receta.getId()));

        Sucursal sucursal =
                sucursalRepository
                        .findById(dto.getSucursalId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Sucursal no encontrada."
                                )
                        );

        if (!Boolean.TRUE.equals(
                sucursal.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "La sucursal seleccionada está inactiva."
            );
        }

        Usuario usuario =
                buscarUsuario(
                        nombreUsuario
                );

        DespachoMedicamento despacho =
                new DespachoMedicamento();

        despacho.setReceta(
                receta
        );

        despacho.setSucursal(
                sucursal
        );

        despacho.setUsuario(
                usuario
        );

        despacho.setFechaDespacho(
                LocalDateTime.now()
        );

        despacho.setActivo(
                true
        );

        BigDecimal montoTotal =
                BigDecimal.ZERO;

        List<DespachoMedicamentoDetalle>
                detallesDespacho =
                new ArrayList<>();

        int lineasDespachadas = 0;

        for (DespachoDetalleDTO detalleDTO
                : dto.getDetalles()) {

            /*
             * Permitimos cantidad 0 o null para que
             * farmacia pueda dejar una línea pendiente
             * durante un despacho parcial.
             */
            if (detalleDTO == null
                    || detalleDTO.getCantidad() == null
                    || detalleDTO.getCantidad() == 0) {

                continue;
            }

            if (detalleDTO.getCantidad() < 0) {

                throw new IllegalArgumentException(
                        "La cantidad a despachar no puede ser negativa."
                );
            }

            if (detalleDTO.getRecetaDetalleId()
                    == null) {

                throw new IllegalArgumentException(
                        "Detalle de receta inválido."
                );
            }

            RecetaDetalle recetaDetalle =
                    recetaDetalleRepository
                            .findById(
                                    detalleDTO
                                            .getRecetaDetalleId()
                            )
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Detalle de receta no encontrado."
                                    )
                            );

            /*
             * Evitamos que alguien envíe un detalle
             * perteneciente a otra receta.
             */
            if (recetaDetalle.getReceta() == null
                    || recetaDetalle
                    .getReceta()
                    .getId() == null
                    || !recetaDetalle
                    .getReceta()
                    .getId()
                    .equals(
                            receta.getId()
                    )) {

                throw new IllegalArgumentException(
                        "El medicamento indicado no pertenece a esta receta."
                );
            }

            if (!Boolean.TRUE.equals(
                    recetaDetalle.getActivo()
            )) {

                throw new IllegalArgumentException(
                        "El detalle de receta se encuentra inactivo."
                );
            }

            int yaDespachado =
                    recetaDetalle
                            .getCantidadDespachada() == null
                            ? 0
                            : recetaDetalle
                            .getCantidadDespachada();

            if (detalleDTO.getCantidadDespachadaPrevia() == null
                    || detalleDTO.getCantidadDespachadaPrevia() != yaDespachado) {
                throw new IllegalArgumentException("La receta fue actualizada por otro despacho. Vuelva a cargarla antes de continuar.");
            }

            int pendiente =
                    recetaDetalle.getCantidad()
                            - yaDespachado;

            if (pendiente <= 0) {

                throw new IllegalArgumentException(
                        "El medicamento "
                                + recetaDetalle
                                .getMedicamento()
                                .getNombre()
                                + " ya fue despachado completamente."
                );
            }

            int cantidadSolicitada =
                    detalleDTO.getCantidad();

            if (cantidadSolicitada
                    > pendiente) {

                throw new IllegalArgumentException(
                        "No puede despachar "
                                + cantidadSolicitada
                                + " unidades de "
                                + recetaDetalle
                                .getMedicamento()
                                .getNombre()
                                + ". Cantidad pendiente: "
                                + pendiente
                                + "."
                );
            }

            Long medicamentoEntregadoId =
                    detalleDTO
                            .getMedicamentoDespachadoId();

            if (medicamentoEntregadoId == null) {

                medicamentoEntregadoId =
                        recetaDetalle
                                .getMedicamento()
                                .getId();
            }

            Medicamento medicamentoEntregado =
                    medicamentoRepository
                            .findById(
                                    medicamentoEntregadoId
                            )
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Medicamento a entregar no encontrado."
                                    )
                            );

            if (!Boolean.TRUE.equals(
                    medicamentoEntregado.getActivo()
            )) {

                throw new IllegalArgumentException(
                        "El medicamento a entregar se encuentra inactivo."
                );
            }

            if (medicamentoEntregado.getPrecio() == null || medicamentoEntregado.getPrecio().signum() < 0
                    || medicamentoEntregado.getPrecio().stripTrailingZeros().scale() > 2) {
                throw new IllegalArgumentException("El precio del medicamento no es válido.");
            }

            InventarioMedicamento inventario = inventarioRepository
                    .findByMedicamentoIdAndSucursalIdAndActivoTrue(medicamentoEntregadoId, sucursal.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Sin inventario registrado"));
            if (inventario.getStockActual() == null || inventario.getStockActual() < cantidadSolicitada) {
                throw new IllegalArgumentException("Stock insuficiente para " + medicamentoEntregado.getNombre() + ".");
            }

            boolean sustituido =
                    !medicamentoEntregado
                            .getId()
                            .equals(
                                    recetaDetalle
                                            .getMedicamento()
                                            .getId()
                            );

            if (sustituido != Boolean.TRUE.equals(detalleDTO.getSustituir())) {
                throw new IllegalArgumentException("Marque la sustitución y seleccione un medicamento alternativo distinto del original.");
            }

            String motivoSustitucion =
                    normalizarOpcional(
                            detalleDTO
                                    .getMotivoSustitucion()
                    );

            if (sustituido) {

                if (motivoSustitucion == null) {

                    throw new IllegalArgumentException(
                            "Debe indicar el motivo de la sustitución del medicamento."
                    );
                }

                if (motivoSustitucion.length() < 10
                        || motivoSustitucion.length() > 500) {

                    throw new IllegalArgumentException(
                            "El motivo de sustitución debe contener entre 10 y 500 caracteres."
                    );
                }

            } else {

                /*
                 * Si no hubo sustitución, no guardamos
                 * un motivo accidental enviado por HTML.
                 */
                motivoSustitucion =
                        null;
            }

            /*
             * Este método de CU-15 valida nuevamente:
             *
             * - inventario activo
             * - sucursal activa
             * - medicamento activo
             * - stock suficiente
             *
             * y además descuenta realmente el stock.
             */
            MovimientoInventarioMedicamento movimiento = movimientoInventarioService
                    .registrarDespachoAutomatico(
                            medicamentoEntregado.getId(),
                            sucursal.getId(),
                            cantidadSolicitada,
                            "RECETA-"
                                    + receta.getId(),
                            sustituido
                                    ? "Despacho con sustitución. "
                                    + motivoSustitucion
                                    : "Despacho de receta "
                                    + receta.getId(),
                            nombreUsuario
                    );

            Integer minimo = medicamentoEntregado.getStockMinimo();
            if (minimo != null && movimiento.getStockNuevo() <= minimo) {
                despacho.getAvisos().add("ALERTA: El medicamento " + medicamentoEntregado.getNombre()
                        + " ha alcanzado el nivel de stock mínimo (" + movimiento.getStockNuevo()
                        + " unidades restantes). Se recomienda generar orden de reabastecimiento.");
            }
            if (sustituido) {
                String mensaje = "Medicamento " + recetaDetalle.getMedicamento().getNombre()
                        + " sustituido por " + medicamentoEntregado.getNombre()
                        + ". El médico tratante será notificado de la sustitución.";
                despacho.getAvisos().add(mensaje);
                NotificacionInterna aviso = new NotificacionInterna();
                aviso.setUsuarioDestino(receta.getConsultaMedica().getMedico().getPersonal().getUsuario());
                aviso.setTipo("SUSTITUCION_MEDICAMENTO"); aviso.setTitulo("Sustitución en farmacia");
                aviso.setMensaje(mensaje); aviso.setReferenciaTipo("RECETA");
                aviso.setReferenciaId(receta.getId().toString());
                notificacionRepository.save(aviso);
            }

            DespachoMedicamentoDetalle
                    detalleDespacho =
                    new DespachoMedicamentoDetalle();

            detalleDespacho.setDespacho(
                    despacho
            );

            detalleDespacho.setRecetaDetalle(
                    recetaDetalle
            );

            detalleDespacho.setMedicamentoDespachado(
                    medicamentoEntregado
            );

            detalleDespacho.setCantidad(
                    cantidadSolicitada
            );

            detalleDespacho.setPrecioUnitario(
                    medicamentoEntregado
                            .getPrecio()
            );

            detalleDespacho.setSustituido(
                    sustituido
            );

            detalleDespacho.setMotivoSustitucion(
                    motivoSustitucion
            );

            detallesDespacho.add(
                    detalleDespacho
            );

            /*
             * cantidadDespachada pertenece al medicamento
             * ORIGINAL recetado, incluso si hubo
             * sustitución.
             */
            recetaDetalle.setCantidadDespachada(
                    yaDespachado
                            + cantidadSolicitada
            );

            recetaDetalleRepository.save(
                    recetaDetalle
            );

            BigDecimal subtotal =
                    medicamentoEntregado
                            .getPrecio()
                            .multiply(
                                    BigDecimal.valueOf(
                                            cantidadSolicitada
                                    )
                            );

            montoTotal =
                    montoTotal.add(
                            subtotal
                    );

            lineasDespachadas++;
        }

        if (lineasDespachadas == 0) {

            throw new IllegalArgumentException(
                    "Debe indicar al menos un medicamento con cantidad mayor que 0."
            );
        }

        if (montoTotal.setScale(2).precision() > 12) {
            throw new IllegalArgumentException("El total del despacho supera el monto permitido.");
        }
        despacho.setMontoTotal(
                montoTotal
        );

        despacho.setDetalles(
                detallesDespacho
        );

        despacho =
                despachoRepository.save(
                        despacho
                );

        actualizarEstadoReceta(
                receta
        );

        recetaRepository.save(
                receta
        );

        registrarAuditoria(
                usuario,
                despacho
        );

        return despacho;
    }

    /*
     * =========================================================
     * HISTORIAL
     * =========================================================
     */

    @Override
    @Transactional(readOnly = true)
    public List<DespachoMedicamento>
    listarDespachosPorReceta(
            Long recetaId
    ) {

        if (recetaId == null) {

            throw new IllegalArgumentException(
                    "Debe indicar la receta."
            );
        }

        return despachoRepository
                .findByRecetaIdOrderByFechaDespachoDesc(
                        recetaId
                );
    }

    /*
     * =========================================================
     * VALIDACIONES
     * =========================================================
     */

    private void validarDTO(
            DespachoMedicamentoDTO dto
    ) {

        if (dto == null) {

            throw new IllegalArgumentException(
                    "Datos del despacho requeridos."
            );
        }

        if (dto.getRecetaId() == null) {

            throw new IllegalArgumentException(
                    "Debe indicar la receta."
            );
        }

        if (dto.getSucursalId() == null) {

            throw new IllegalArgumentException(
                    "Debe indicar la sucursal."
            );
        }

        if (dto.getDetalles() == null
                || dto.getDetalles().isEmpty()) {

            throw new IllegalArgumentException(
                    "Debe indicar al menos un medicamento."
            );
        }
        var lineasUnicas = new HashSet<Long>();
        for (DespachoDetalleDTO linea : dto.getDetalles()) {
            if (linea != null && linea.getCantidad() != null && linea.getCantidad() > 0
                    && !lineasUnicas.add(linea.getRecetaDetalleId())) {
                throw new IllegalArgumentException("No puede repetir un medicamento de la misma receta en el despacho.");
            }
        }
    }

    private void validarContenidoReceta(List<RecetaDetalle> detalles) {
        if (detalles.isEmpty()) throw new IllegalArgumentException(RECETA_INVALIDA);
        for (RecetaDetalle detalle : detalles) {
            Medicamento med = detalle.getMedicamento();
            if (med == null || med.getId() == null || !Boolean.TRUE.equals(med.getActivo())
                    || !medicamentoRepository.existsById(med.getId())
                    || detalle.getDosis() == null || detalle.getDosis().isBlank()
                    || detalle.getDosis().length() > 200
                    || detalle.getCantidad() == null || detalle.getCantidad() <= 0) {
                throw new IllegalArgumentException(RECETA_INVALIDA);
            }
        }
    }

    private void validarRecetaParaDespacho(
            Receta receta
    ) {

        if (!Boolean.TRUE.equals(
                receta.getActivo()
        )) {

            throw new IllegalArgumentException(
                    "La receta se encuentra inactiva."
            );
        }

        /*
         * Antes de cualquier despacho verificamos
         * la vigencia real.
         */
        if (receta.isVencidaParaDespacho()) {
            throw new IllegalArgumentException(receta.getMensajeVencida());
        }
        if (receta.getFechaEmision() == null || receta.getFechaEmision().isAfter(LocalDateTime.now())
                || receta.getFechaVencimiento() == null || receta.getConsultaMedica() == null) {
            throw new IllegalArgumentException(RECETA_INVALIDA);
        }

        if (receta.getEstado()
                == EstadoReceta.DESPACHADA) {

            throw new IllegalArgumentException(
                    "La receta ya fue despachada completamente."
            );
        }

        if (receta.getEstado()
                == EstadoReceta.NO_ADQUIRIDA) {

            throw new IllegalArgumentException(
                    "La receta fue marcada como no adquirida."
            );
        }
        if (receta.getEstado() != EstadoReceta.PENDIENTE && receta.getEstado() != EstadoReceta.PARCIAL) {
            throw new IllegalArgumentException(RECETA_INVALIDA);
        }
    }

    /*
     * =========================================================
     * ESTADO DE RECETA
     * =========================================================
     */

    private void actualizarEstadoReceta(
            Receta receta
    ) {

        List<RecetaDetalle> detalles =
                recetaDetalleRepository
                        .findByRecetaIdAndActivoTrueOrderByIdAsc(
                                receta.getId()
                        );

        boolean todosDespachados =
                true;

        boolean algunoDespachado =
                false;

        for (RecetaDetalle detalle
                : detalles) {

            int cantidadDespachada =
                    detalle
                            .getCantidadDespachada() == null
                            ? 0
                            : detalle
                            .getCantidadDespachada();

            if (cantidadDespachada > 0) {

                algunoDespachado =
                        true;
            }

            if (cantidadDespachada
                    < detalle.getCantidad()) {

                todosDespachados =
                        false;
            }
        }

        if (todosDespachados
                && !detalles.isEmpty()) {

            receta.setEstado(
                    EstadoReceta.DESPACHADA
            );

        } else if (algunoDespachado) {

            receta.setEstado(
                    EstadoReceta.PARCIAL
            );

        } else {

            receta.setEstado(
                    EstadoReceta.PENDIENTE
            );
        }
    }

    /*
     * =========================================================
     * UTILIDADES
     * =========================================================
     */

    private Usuario buscarUsuario(
            String nombreUsuario
    ) {

        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            throw new IllegalArgumentException("El usuario de farmacia es obligatorio.");
        }

        return usuarioRepository
                .findByNombreUsuarioIgnoreCase(
                        nombreUsuario
                )
                .orElseThrow(() -> new IllegalArgumentException("El usuario de farmacia no existe."));
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

    /*
     * =========================================================
     * AUDITORÍA
     * =========================================================
     */

    private void registrarAuditoria(
            Usuario usuario,
            DespachoMedicamento despacho
    ) {

        Auditoria auditoria =
                new Auditoria();

        auditoria.setUsuario(
                usuario
        );

        auditoria.setAccion(
                "REGISTRAR_DESPACHO_MEDICAMENTO"
        );

        auditoria.setTipoEntidad(
                "DESPACHO_MEDICAMENTO"
        );

        auditoria.setEntidadId(
                despacho
                        .getId()
                        .toString()
        );

        auditoria.setDetalle(
                "Se registró el despacho "
                        + despacho.getId()
                        + " de la receta "
                        + despacho
                        .getReceta()
                        .getId()
                        + ". Sucursal: "
                        + despacho
                        .getSucursal()
                        .getNombre()
                        + ". Monto total: Q "
                        + despacho
                        .getMontoTotal()
                        + "."
        );

        auditoriaRepository.save(
                auditoria
        );
    }
}
