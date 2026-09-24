package pe.edu.upeu.PharmaBackend.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.PharmaBackend.dto.ClienteRequestDTO;
import pe.edu.upeu.PharmaBackend.dto.ClienteResponseDTO;
import pe.edu.upeu.PharmaBackend.dto.PaginaResponseDTO;
import pe.edu.upeu.PharmaBackend.entity.Cliente;
import pe.edu.upeu.PharmaBackend.exception.RecursoNoEncontradoException;
import pe.edu.upeu.PharmaBackend.exception.ReglaNegocioException;
import pe.edu.upeu.PharmaBackend.mapper.ClienteMapper;
import pe.edu.upeu.PharmaBackend.repository.ClienteRepository;
import pe.edu.upeu.PharmaBackend.service.service.ClienteService;
import pe.edu.upeu.PharmaBackend.util.PaginacionUtil;

import java.util.Set;

@Service
public class ClienteServiceImpl
        implements ClienteService {

    private static final Logger log =
            LoggerFactory.getLogger(ClienteServiceImpl.class);

    private static final Set<String> CAMPOS_ORDENABLES =
            Set.of("id", "dni", "nombres", "apellidos", "email");

    private static final String ORDEN_POR_DEFECTO = "id";

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;

    public ClienteServiceImpl(
            ClienteRepository clienteRepository,
            ClienteMapper clienteMapper) {
        this.clienteRepository = clienteRepository;
        this.clienteMapper = clienteMapper;
    }

    @Override
    @Transactional
    public ClienteResponseDTO crear(
            ClienteRequestDTO request) {

        log.info(
                "Registrando cliente con DNI={}",
                request.getDni()
        );

        String dni = request.getDni().trim();
        String email = request.getEmail()
                .trim()
                .toLowerCase();

        // Regla de negocio 1
        if (clienteRepository.existsByDni(dni)) {
            throw new ReglaNegocioException(
                    "Ya existe un cliente con el DNI: " + dni
            );
        }

        // Regla de negocio 2
        if (clienteRepository.existsByEmailIgnoreCase(email)) {
            throw new ReglaNegocioException(
                    "Ya existe un cliente con el correo: " + email
            );
        }

        Cliente cliente = new Cliente();

        cliente.setDni(dni);
        cliente.setNombres(
                request.getNombres().trim()
        );
        cliente.setApellidos(
                request.getApellidos().trim()
        );
        cliente.setEmail(email);
        cliente.setTelefono(
                normalizar(request.getTelefono())
        );
        cliente.setDireccion(
                normalizar(request.getDireccion())
        );
        cliente.setEstado(request.getEstado());

        Cliente guardado =
                clienteRepository.save(cliente);

        log.info(
                "Cliente registrado correctamente id={}",
                guardado.getId()
        );

        return clienteMapper.toResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO buscar(Long id) {

        log.info("Buscando cliente id={}", id);

        Cliente cliente =
                clienteRepository.findById(id)
                        .orElseThrow(() ->
                                new RecursoNoEncontradoException(
                                        "Cliente no encontrado con id: " + id
                                )
                        );

        return clienteMapper.toResponse(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponseDTO<ClienteResponseDTO> listar(
            int pagina,
            int tamanio,
            String ordenarPor,
            String direccion) {

        long inicio = System.currentTimeMillis();
        log.info("Inicio listar clientes | pagina={} | tamanio={} | "
                        + "ordenarPor={} | direccion={}",
                pagina, tamanio, ordenarPor, direccion);

        Pageable pageable = PaginacionUtil.construir(
                pagina, tamanio, ordenarPor, direccion,
                CAMPOS_ORDENABLES, ORDEN_POR_DEFECTO);

        Page<ClienteResponseDTO> resultado =
                clienteRepository.findAll(pageable)
                        .map(clienteMapper::toResponse);

        log.info("Fin listar clientes | filas={} | total={} | duracionMs={}",
                resultado.getNumberOfElements(),
                resultado.getTotalElements(),
                System.currentTimeMillis() - inicio);

        return PaginaResponseDTO.de(resultado);
    }

    @Override
    @Transactional
    public ClienteResponseDTO actualizar(
            Long id,
            ClienteRequestDTO request) {

        Cliente cliente =
                clienteRepository.findById(id)
                        .orElseThrow(() ->
                                new RecursoNoEncontradoException(
                                        "Cliente no encontrado con id: " + id
                                )
                        );

        String dni = request.getDni().trim();
        String email = request.getEmail()
                .trim()
                .toLowerCase();

        // DNI de otro cliente
        if (clienteRepository
                .existsByDniAndIdNot(dni, id)) {

            throw new ReglaNegocioException(
                    "Ya existe otro cliente con el DNI: "
                            + dni
            );
        }

        // Email de otro cliente
        if (clienteRepository
                .existsByEmailIgnoreCaseAndIdNot(
                        email,
                        id)) {

            throw new ReglaNegocioException(
                    "Ya existe otro cliente con el correo: "
                            + email
            );
        }

        cliente.setDni(dni);
        cliente.setNombres(
                request.getNombres().trim()
        );
        cliente.setApellidos(
                request.getApellidos().trim()
        );
        cliente.setEmail(email);
        cliente.setTelefono(
                normalizar(request.getTelefono())
        );
        cliente.setDireccion(
                normalizar(request.getDireccion())
        );
        cliente.setEstado(request.getEstado());

        Cliente actualizado =
                clienteRepository.save(cliente);

        log.info(
                "Cliente id={} actualizado correctamente",
                id
        );

        return clienteMapper.toResponse(actualizado);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {

        Cliente cliente =
                clienteRepository.findById(id)
                        .orElseThrow(() ->
                                new RecursoNoEncontradoException(
                                        "Cliente no encontrado con id: " + id
                                )
                        );

        // Baja lógica: el cliente queda referenciado desde ventas.cliente_id.
        if (!Boolean.TRUE.equals(cliente.getEstado())) {
            throw new ReglaNegocioException(
                    "El cliente con id " + id + " ya se encuentra inactivo"
            );
        }

        cliente.setEstado(false);
        clienteRepository.save(cliente);

        log.info(
                "Cliente id={} dado de baja correctamente",
                id
        );
    }

    private String normalizar(String valor) {

        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }
}
