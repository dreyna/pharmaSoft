package pe.edu.upeu.PharmaBackend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.PharmaBackend.entity.Producto;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, long id);

    List<Producto> findByCategoriaId(Long categoriaId);
    boolean existsByCategoriaId(Long categoriaId);

    /*
     * La categoría se trae en la misma consulta: la respuesta expone
     * su nombre y sin esto cada producto dispararía una consulta extra.
     */
    @Override
    @EntityGraph(attributePaths = "categoria")
    Page<Producto> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "categoria")
    Optional<Producto> findById(Long id);

    /*
     * Descuento atómico de stock: la condición stock >= cantidad viaja
     * en el mismo UPDATE, así dos ventas concurrentes del mismo producto
     * nunca pueden dejar el stock en negativo. Devuelve 0 si no había
     * stock suficiente en el momento de ejecutar.
     */
    @Modifying
    @Query("""
            update Producto p
               set p.stock = p.stock - :cantidad
             where p.id = :id
               and p.stock >= :cantidad
            """)
    int descontarStock(@Param("id") Long id,
                       @Param("cantidad") Integer cantidad);
}
