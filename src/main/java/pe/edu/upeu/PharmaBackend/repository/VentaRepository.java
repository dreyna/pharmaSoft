package pe.edu.upeu.PharmaBackend.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.PharmaBackend.entity.Venta;
import pe.edu.upeu.PharmaBackend.enums.EstadoVenta;

import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepository
        extends JpaRepository<Venta, Long> {

    /*
     * Búsqueda con filtros combinados.
     *
     * Los cuatro parámetros admiten null: cuando uno llega nulo su
     * condición se neutraliza y el filtro deja de aplicarse, de modo
     * que este único método cubre todas las combinaciones posibles.
     *
     * El Sort se recibe como último argumento y Spring Data lo traduce
     * a la cláusula order by sobre el alias v.
     *
     * Los left join fetch traen cliente, detalles y producto en una
     * sola consulta para evitar el N+1 al armar el VentaResponseDTO.
     */
    @Query("""
            select distinct v
            from Venta v
            left join fetch v.cliente c
            left join fetch v.detalles d
            left join fetch d.producto p
            where (:clienteId is null or c.id = :clienteId)
              and (:estado    is null or v.estado = :estado)
              and (:desde     is null or v.fecha >= :desde)
              and (:hasta     is null or v.fecha <= :hasta)
            """)
    List<Venta> buscar(
            @Param("clienteId") Long clienteId,
            @Param("estado") EstadoVenta estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Sort sort);
}
