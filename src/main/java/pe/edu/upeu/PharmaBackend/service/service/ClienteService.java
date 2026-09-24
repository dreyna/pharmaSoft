package pe.edu.upeu.PharmaBackend.service.service;

import pe.edu.upeu.PharmaBackend.dto.ClienteRequestDTO;
import pe.edu.upeu.PharmaBackend.dto.ClienteResponseDTO;
import pe.edu.upeu.PharmaBackend.dto.PaginaResponseDTO;

public interface ClienteService {

    ClienteResponseDTO crear(ClienteRequestDTO request);

    ClienteResponseDTO actualizar(Long id, ClienteRequestDTO request);

    ClienteResponseDTO buscar(Long id);

    void eliminar(Long id);

    PaginaResponseDTO<ClienteResponseDTO> listar(
            int pagina,
            int tamanio,
            String ordenarPor,
            String direccion);
}
