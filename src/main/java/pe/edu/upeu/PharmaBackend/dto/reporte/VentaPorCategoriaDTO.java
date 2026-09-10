package pe.edu.upeu.PharmaBackend.dto.reporte;

import java.math.BigDecimal;

/*
 * Total facturado y unidades vendidas agrupadas por categoría.
 *
 * Se proyecta directamente desde la consulta con select new, por lo
 * que el orden y el tipo de los componentes deben coincidir con el
 * de la cláusula select del repositorio.
 */
public record VentaPorCategoriaDTO(

        Long categoriaId,
        String categoriaNombre,
        Long cantidadVendida,
        BigDecimal montoTotal) {
}
