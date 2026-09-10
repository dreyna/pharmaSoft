package pe.edu.upeu.PharmaBackend.dto.reporte;

import java.math.BigDecimal;

/*
 * Unidades vendidas y monto facturado por producto, usado para el
 * ranking de productos más vendidos.
 */
public record ProductoMasVendidoDTO(

        Long productoId,
        String productoNombre,
        String categoriaNombre,
        Long cantidadVendida,
        BigDecimal montoTotal) {
}
