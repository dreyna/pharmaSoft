package pe.edu.upeu.PharmaBackend.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import pe.edu.upeu.PharmaBackend.exception.ReglaNegocioException;

import java.util.Set;

/*
 * Arma el Pageable a partir de los parámetros de consulta, validando
 * el rango de página/tamaño y el campo de ordenamiento contra la
 * lista blanca que define cada servicio. Así ningún endpoint expone
 * la estructura interna de la entidad ni permite ordenamientos
 * arbitrarios.
 */
public final class PaginacionUtil {

    public static final int TAMANIO_MAXIMO = 100;

    private PaginacionUtil() {
    }

    public static Pageable construir(
            int pagina,
            int tamanio,
            String ordenarPor,
            String direccion,
            Set<String> camposOrdenables,
            String ordenPorDefecto) {

        if (pagina < 0) {
            throw new ReglaNegocioException(
                    "La página no puede ser negativa");
        }

        if (tamanio < 1 || tamanio > TAMANIO_MAXIMO) {
            throw new ReglaNegocioException(
                    "El tamaño de página debe estar entre 1 y "
                            + TAMANIO_MAXIMO);
        }

        return PageRequest.of(
                pagina,
                tamanio,
                construirSort(ordenarPor, direccion,
                        camposOrdenables, ordenPorDefecto));
    }

    public static Sort construirSort(
            String ordenarPor,
            String direccion,
            Set<String> camposOrdenables,
            String ordenPorDefecto) {

        String campo = (ordenarPor == null || ordenarPor.isBlank())
                ? ordenPorDefecto
                : ordenarPor.trim();

        if (!camposOrdenables.contains(campo)) {

            throw new ReglaNegocioException(
                    "El campo de ordenamiento '"
                            + campo
                            + "' no está permitido. Campos válidos: "
                            + camposOrdenables);
        }

        String sentido = (direccion == null || direccion.isBlank())
                ? "desc"
                : direccion.trim();

        if (!sentido.equalsIgnoreCase("asc")
                && !sentido.equalsIgnoreCase("desc")) {

            throw new ReglaNegocioException(
                    "La dirección de ordenamiento '"
                            + sentido
                            + "' no está permitida. Valores válidos: asc, desc");
        }

        return sentido.equalsIgnoreCase("asc")
                ? Sort.by(campo).ascending()
                : Sort.by(campo).descending();
    }
}
