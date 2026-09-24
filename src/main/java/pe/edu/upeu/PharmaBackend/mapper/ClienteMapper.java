package pe.edu.upeu.PharmaBackend.mapper;

import org.springframework.stereotype.Component;
import pe.edu.upeu.PharmaBackend.dto.ClienteResponseDTO;
import pe.edu.upeu.PharmaBackend.entity.Cliente;

@Component
public class ClienteMapper {

    public ClienteResponseDTO toResponse(Cliente cliente) {
        return new ClienteResponseDTO(
                cliente.getId(),
                cliente.getDni(),
                cliente.getNombres(),
                cliente.getApellidos(),
                cliente.getEmail(),
                cliente.getTelefono(),
                cliente.getDireccion(),
                cliente.getEstado(),
                cliente.getFechaCreacion(),
                cliente.getFechaModificacion()
        );
    }
}
