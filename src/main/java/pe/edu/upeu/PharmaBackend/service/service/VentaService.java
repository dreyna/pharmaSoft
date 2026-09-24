package pe.edu.upeu.PharmaBackend.service.service;

import pe.edu.upeu.PharmaBackend.dto.PaginaResponseDTO;
import pe.edu.upeu.PharmaBackend.dto.VentaRequestDTO;
import pe.edu.upeu.PharmaBackend.dto.VentaResponseDTO;
import pe.edu.upeu.PharmaBackend.enums.EstadoVenta;

import java.time.LocalDate;

public interface VentaService {

    VentaResponseDTO registrar(VentaRequestDTO request);

    VentaResponseDTO buscar(Long id);

    PaginaResponseDTO<VentaResponseDTO> listar(
            int pagina,
            int tamanio,
            String ordenarPor,
            String direccion);

    PaginaResponseDTO<VentaResponseDTO> buscar(
            Long clienteId,
            EstadoVenta estado,
            LocalDate desde,
            LocalDate hasta,
            int pagina,
            int tamanio,
            String ordenarPor,
            String direccion);
}
