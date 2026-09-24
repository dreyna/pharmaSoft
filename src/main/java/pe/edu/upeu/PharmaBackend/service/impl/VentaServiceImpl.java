package pe.edu.upeu.PharmaBackend.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.PharmaBackend.dto.DetalleVentaRequestDTO;
import pe.edu.upeu.PharmaBackend.dto.PaginaResponseDTO;
import pe.edu.upeu.PharmaBackend.dto.VentaRequestDTO;
import pe.edu.upeu.PharmaBackend.dto.VentaResponseDTO;
import pe.edu.upeu.PharmaBackend.entity.Cliente;
import pe.edu.upeu.PharmaBackend.entity.DetalleVenta;
import pe.edu.upeu.PharmaBackend.entity.Producto;
import pe.edu.upeu.PharmaBackend.entity.Venta;
import pe.edu.upeu.PharmaBackend.enums.EstadoVenta;
import pe.edu.upeu.PharmaBackend.exception.RecursoNoEncontradoException;
import pe.edu.upeu.PharmaBackend.exception.ReglaNegocioException;
import pe.edu.upeu.PharmaBackend.mapper.VentaMapper;
import pe.edu.upeu.PharmaBackend.repository.ClienteRepository;
import pe.edu.upeu.PharmaBackend.repository.ProductoRepository;
import pe.edu.upeu.PharmaBackend.repository.VentaRepository;
import pe.edu.upeu.PharmaBackend.service.service.VentaService;
import pe.edu.upeu.PharmaBackend.util.PaginacionUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
@Service
public class VentaServiceImpl implements VentaService {

    private static final Logger log =
            LoggerFactory.getLogger(VentaServiceImpl.class);

    /*
     * Lista blanca de campos por los que se permite ordenar la
     * búsqueda. Cualquier otro valor se rechaza con 409 para no
     * exponer la estructura interna de la entidad ni permitir
     * ordenamientos arbitrarios.
     */
    private static final Set<String> CAMPOS_ORDENABLES =
            Set.of("id", "fecha", "total", "estado");

    private static final String ORDEN_POR_DEFECTO = "fecha";

    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final VentaMapper ventaMapper;

    public VentaServiceImpl(
            VentaRepository ventaRepository,
            ClienteRepository clienteRepository,
            ProductoRepository productoRepository,
            VentaMapper ventaMapper) {

        this.ventaRepository = ventaRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.ventaMapper = ventaMapper;
    }

    @Override
    @Transactional
    public VentaResponseDTO registrar(VentaRequestDTO request) {

        long inicio = System.currentTimeMillis();

        log.info("Inicio registrar venta | clienteId={} | items={}",
                request.getClienteId(),
                request.getDetalles() == null
                        ? 0
                        : request.getDetalles().size());

        Cliente cliente = clienteRepository.findById(request.getClienteId())
                        .orElseThrow(() ->new RecursoNoEncontradoException("Cliente no encontrado con id: "+ request.getClienteId()));

        if (!Boolean.TRUE.equals(cliente.getEstado())) {
            throw new ReglaNegocioException("No se puede registrar una venta para un cliente inactivo");
        }
        Venta venta = new Venta();

        venta.setCliente(cliente);
        venta.setFecha(LocalDateTime.now());
        venta.setEstado(EstadoVenta.REGISTRADA);

        BigDecimal total = BigDecimal.ZERO;

        for (DetalleVentaRequestDTO item: request.getDetalles()) {
            Producto producto = productoRepository.findById(item.getProductoId()).orElseThrow(() ->
                                    new RecursoNoEncontradoException("Producto no encontrado con id: "+ item.getProductoId()));

            if (!Boolean.TRUE.equals(producto.getEstado())) {
                throw new ReglaNegocioException("El producto "+ producto.getNombre()+ " se encuentra inactivo");
            }

            /*
             * El descuento es un UPDATE condicional en la base (ver
             * ProductoRepository.descontarStock): si devuelve 0 es que
             * otra venta se llevó el stock entre la lectura y este
             * punto, o simplemente no alcanzaba. La excepción revierte
             * toda la transacción, incluidos los descuentos de los
             * ítems anteriores.
             */
            int filas = productoRepository.descontarStock(
                    producto.getId(), item.getCantidad());

            if (filas == 0) {

                throw new ReglaNegocioException("Stock insuficiente para "+ producto.getNombre()+ ". Disponible: "+ producto.getStock()
                                + ", solicitado: "+ item.getCantidad());
            }

            BigDecimal subtotal = producto.getPrecio().multiply(BigDecimal.valueOf(item.getCantidad()));

            DetalleVenta detalle = new DetalleVenta();

            detalle.setProducto(producto);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecio(producto.getPrecio());
            detalle.setSubtotal(subtotal);

            venta.agregarDetalle(detalle);

            total = total.add(subtotal);
        }

        venta.setTotal(total);

        Venta guardada =ventaRepository.save(venta);

        log.info("Fin registrar venta | ventaId={} | total={} | "
                        + "filas={} | duracionMs={}",
                guardada.getId(),
                guardada.getTotal(),
                guardada.getDetalles().size(),
                System.currentTimeMillis() - inicio);

        return ventaMapper.toResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public VentaResponseDTO buscar(Long id) {

        long inicio = System.currentTimeMillis();
        log.info("Inicio buscar venta por id | id={}", id);

        Venta venta = ventaRepository.findById(id).orElseThrow(() ->
                                new RecursoNoEncontradoException("Venta no encontrada con id: "+ id));

        log.info("Fin buscar venta por id | id={} | filas={} | duracionMs={}",
                id, 1, System.currentTimeMillis() - inicio);

        return ventaMapper.toResponse(venta);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponseDTO<VentaResponseDTO> listar(
            int pagina,
            int tamanio,
            String ordenarPor,
            String direccion) {

        return buscar(null, null, null, null,
                pagina, tamanio, ordenarPor, direccion);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponseDTO<VentaResponseDTO> buscar(
            Long clienteId,
            EstadoVenta estado,
            LocalDate desde,
            LocalDate hasta,
            int pagina,
            int tamanio,
            String ordenarPor,
            String direccion) {

        long inicio = System.currentTimeMillis();

        log.info("Inicio buscar ventas | clienteId={} | estado={} | "
                        + "desde={} | hasta={} | pagina={} | tamanio={} | "
                        + "ordenarPor={} | direccion={}",
                clienteId, estado, desde, hasta,
                pagina, tamanio, ordenarPor, direccion);

        if (desde != null
                && hasta != null
                && desde.isAfter(hasta)) {

            throw new ReglaNegocioException(
                    "El rango de fechas es inválido: 'desde' ("
                            + desde
                            + ") es posterior a 'hasta' ("
                            + hasta + ")");
        }

        Pageable pageable = PaginacionUtil.construir(
                pagina, tamanio, ordenarPor, direccion,
                CAMPOS_ORDENABLES, ORDEN_POR_DEFECTO);

        LocalDateTime desdeHora = (desde == null)
                ? null
                : desde.atStartOfDay();

        LocalDateTime hastaHora = (hasta == null)
                ? null
                : hasta.atTime(LocalTime.MAX);

        Page<Venta> ventas = ventaRepository
                .buscar(clienteId, estado, desdeHora, hastaHora, pageable);

        // Segunda consulta: detalles y productos de toda la página de
        // una vez, en lugar de una consulta por venta al convertir.
        if (ventas.hasContent()) {
            ventaRepository.cargarDetalles(ventas.getContent());
        }

        Page<VentaResponseDTO> resultado =
                ventas.map(ventaMapper::toResponse);

        log.info("Fin buscar ventas | clienteId={} | estado={} | "
                        + "desde={} | hasta={} | orden={} {} | "
                        + "filas={} | total={} | duracionMs={}",
                clienteId, estado, desde, hasta, ordenarPor, direccion,
                resultado.getNumberOfElements(),
                resultado.getTotalElements(),
                System.currentTimeMillis() - inicio);

        return PaginaResponseDTO.de(resultado);
    }
}
