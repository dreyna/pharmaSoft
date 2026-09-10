package pe.edu.upeu.PharmaBackend.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.PharmaBackend.dto.reporte.ProductoMasVendidoDTO;
import pe.edu.upeu.PharmaBackend.dto.reporte.VentaPorCategoriaDTO;
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

    /*
     * Reporte: total facturado y unidades vendidas por categoría.
     *
     * Proyecta con select new sobre el record, agrupa por categoría y
     * considera únicamente las ventas REGISTRADA, de modo que las
     * ventas anuladas nunca suman al reporte.
     */
    @Query("""
            select new pe.edu.upeu.PharmaBackend.dto.reporte.VentaPorCategoriaDTO(
                       cat.id,
                       cat.nombre,
                       sum(d.cantidad),
                       sum(d.subtotal))
            from DetalleVenta d
            join d.venta v
            join d.producto p
            join p.categoria cat
            where v.estado = pe.edu.upeu.PharmaBackend.enums.EstadoVenta.REGISTRADA
              and (:desde is null or v.fecha >= :desde)
              and (:hasta is null or v.fecha <= :hasta)
            group by cat.id, cat.nombre
            order by sum(d.subtotal) desc
            """)
    List<VentaPorCategoriaDTO> reporteVentasPorCategoria(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    /*
     * Reporte: ranking de productos más vendidos.
     *
     * Mismo criterio de exclusión de anuladas; ordena por unidades
     * vendidas de mayor a menor.
     */
    @Query("""
            select new pe.edu.upeu.PharmaBackend.dto.reporte.ProductoMasVendidoDTO(
                       p.id,
                       p.nombre,
                       cat.nombre,
                       sum(d.cantidad),
                       sum(d.subtotal))
            from DetalleVenta d
            join d.venta v
            join d.producto p
            join p.categoria cat
            where v.estado = pe.edu.upeu.PharmaBackend.enums.EstadoVenta.REGISTRADA
              and (:desde is null or v.fecha >= :desde)
              and (:hasta is null or v.fecha <= :hasta)
            group by p.id, p.nombre, cat.nombre
            order by sum(d.cantidad) desc
            """)
    List<ProductoMasVendidoDTO> reporteProductosMasVendidos(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
