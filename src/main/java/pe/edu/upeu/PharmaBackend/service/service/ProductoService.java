package pe.edu.upeu.PharmaBackend.service.service;

import pe.edu.upeu.PharmaBackend.dto.PaginaResponseDTO;
import pe.edu.upeu.PharmaBackend.dto.ProductoRequestDTO;
import pe.edu.upeu.PharmaBackend.dto.ProductoResponseDTO;

public interface ProductoService {

    ProductoResponseDTO crear(ProductoRequestDTO request);

    ProductoResponseDTO actualizar(Long id, ProductoRequestDTO request);

    ProductoResponseDTO buscar(Long id);

    void eliminar(Long id);

    PaginaResponseDTO<ProductoResponseDTO> listar(
            int pagina,
            int tamanio,
            String ordenarPor,
            String direccion);
}
