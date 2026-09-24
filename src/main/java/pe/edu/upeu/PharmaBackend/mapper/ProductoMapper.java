package pe.edu.upeu.PharmaBackend.mapper;

import org.springframework.stereotype.Component;
import pe.edu.upeu.PharmaBackend.dto.ProductoResponseDTO;
import pe.edu.upeu.PharmaBackend.entity.Producto;

/*
 * Requiere que producto.categoria ya esté cargada (entity graph);
 * no dispara consultas por su cuenta.
 */
@Component
public class ProductoMapper {

    public ProductoResponseDTO toResponse(Producto producto) {
        return new ProductoResponseDTO(
                producto.getId(),
                producto.getNombre(),
                producto.getPrecio(),
                producto.getStock(),
                producto.getEstado(),
                producto.getCategoria().getId(),
                producto.getCategoria().getNombre(),
                producto.getFechaCreacion(),
                producto.getFechaModificacion()
        );
    }
}
