package com.example.proyecto_semestre_medico.service.impl;

import com.example.proyecto_semestre_medico.model.Rol;
import com.example.proyecto_semestre_medico.model.Usuario;
import com.example.proyecto_semestre_medico.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Protecciones compartidas de los CRUD administrativos; utiliza la transacción y caché existentes. */
@Component
public class ControlCatalogo {
    @PersistenceContext private EntityManager em;
    private final UsuarioRepository usuarios;
    private final CacheManager caches;

    public ControlCatalogo(UsuarioRepository usuarios, CacheManager caches) {
        this.usuarios = usuarios;
        this.caches = caches;
    }

    public void iniciar(String usuario, String permiso) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !auth.getName().equals(usuario)
                || auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"))
                || auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals(permiso))) {
            throw new AccessDeniedException("No tiene permiso para esta operación de catálogo.");
        }
        // Todos los escritores administrativos toman el mismo bloqueo persistente antes de comprobar
        // unicidad. Funciona entre instancias del HIS y no necesita otra tabla ni borrar duplicados históricos.
        em.createQuery("select r from Rol r where r.codigo = 'ADMINISTRADOR'", Rol.class)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).getSingleResult();
        usuario(usuario);
    }

    public Usuario usuario(String nombre) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.getName().equals(nombre)) throw new AccessDeniedException("Usuario auditor inválido.");
        Usuario usuario = usuarios.findByNombreUsuarioIgnoreCase(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("Usuario auditor inexistente."));
        if (!Boolean.TRUE.equals(usuario.getActivo()) || usuario.getRol() == null
                || !Boolean.TRUE.equals(usuario.getRol().getActivo())
                || !"ADMINISTRADOR".equals(usuario.getRol().getCodigo())) {
            throw new AccessDeniedException("Se requiere un administrador activo.");
        }
        return usuario;
    }

    public <T> void nombreUnico(Class<T> tipo, Long id, String nombre) {
        String normalizado = nombre == null ? "" : nombre.trim();
        if (normalizado.isEmpty()) throw new IllegalArgumentException("El nombre es obligatorio.");
        var consulta = em.createQuery("select count(c) from " + tipo.getSimpleName()
                + " c where c.activo = true and lower(trim(c.nombre)) = lower(:nombre)"
                + (id == null ? "" : " and c.id <> :id"), Long.class).setParameter("nombre", normalizado);
        if (id != null) consulta.setParameter("id", id);
        if (consulta.getSingleResult() > 0) throw new IllegalArgumentException(
                "Ya existe un registro con el nombre " + normalizado + " en este catálogo.");
    }

    public <T> java.util.Optional<T> inactivo(Class<T> tipo, String nombre) {
        return em.createQuery("select c from " + tipo.getSimpleName()
                + " c where c.activo = false and lower(trim(c.nombre)) = lower(:nombre) order by c.id", tipo)
                .setParameter("nombre", nombre.trim()).setMaxResults(1).getResultStream().findFirst();
    }

    public <T> java.util.Optional<T> porNombre(Class<T> tipo, String nombre) {
        return em.createQuery("select c from " + tipo.getSimpleName()
                + " c where lower(trim(c.nombre)) = lower(:nombre) order by c.activo desc, c.id", tipo)
                .setParameter("nombre", nombre.trim()).setMaxResults(1).getResultStream().findFirst();
    }

    public void invalidarTrasCommit(String... nombres) {
        if (nombres.length == 0) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                for (String nombre : nombres) {
                    var cache = caches.getCache(nombre);
                    if (cache != null) cache.clear();
                }
            }
        });
    }
}
