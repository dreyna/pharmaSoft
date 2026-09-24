package pe.edu.upeu.PharmaBackend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.PharmaBackend.dto.reporte.ProductoMasVendidoDTO;
import pe.edu.upeu.PharmaBackend.dto.reporte.VentaPorCategoriaDTO;
import pe.edu.upeu.PharmaBackend.entity.Venta;
import pe.edu.upeu.PharmaBackend.enums.EstadoVenta;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VentaRepository
        extends JpaRepository<Venta, Long> {


    /*
     * Búsqueda paginada. Se trae el cliente en la misma consulta; los
     * detalles se cargan aparte con cargarDetalles() porque un fetch
     * join sobre una colección obliga a Hibernate a paginar en memoria.
     */
    @Query(value = """
            select v
            from Venta v
            join fetch v.cliente c
            where (:clienteId is null or c.id = :clienteId)
              and (:estado    is null or v.estado = :estado)
              and (:desde     is null or v.fecha >= :desde)
              and (:hasta     is null or v.fecha <= :hasta)
            """,
            countQuery = """
            select count(v)
            from Venta v
            where (:clienteId is null or v.cliente.id = :clienteId)
              and (:estado    is null or v.estado = :estado)
              and (:desde     is null or v.fecha >= :desde)
              and (:hasta     is null or v.fecha <= :hasta)
            """)
    Page<Venta> buscar(
            @Param("clienteId") Long clienteId,
            @Param("estado") EstadoVenta estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable);

    /*
     * Hidrata detalles y producto de las ventas de una página en una
     * sola consulta. Las instancias devueltas son las mismas del
     * contexto de persistencia, así que las de la página quedan
     * inicializadas.
     */
    @Query("""
            select distinct v
            from Venta v
            left join fetch v.detalles d
            left join fetch d.producto p
            where v in :ventas
            """)
    List<Venta> cargarDetalles(@Param("ventas") Collection<Venta> ventas);

    @Override
    @EntityGraph(attributePaths = {"cliente", "detalles", "detalles.producto"})
    Optional<Venta> findById(Long id);


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
