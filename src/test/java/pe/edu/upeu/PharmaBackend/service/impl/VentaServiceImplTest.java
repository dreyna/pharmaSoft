package pe.edu.upeu.PharmaBackend.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import pe.edu.upeu.PharmaBackend.dto.DetalleVentaRequestDTO;
import pe.edu.upeu.PharmaBackend.dto.PaginaResponseDTO;
import pe.edu.upeu.PharmaBackend.dto.VentaRequestDTO;
import pe.edu.upeu.PharmaBackend.dto.VentaResponseDTO;
import pe.edu.upeu.PharmaBackend.entity.Categoria;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * Pruebas unitarias de las reglas de negocio de VentaServiceImpl con
 * los repositorios simulados. El mapper se usa real: es una conversión
 * pura y así se verifica también la forma de la respuesta.
 */
@ExtendWith(MockitoExtension.class)
class VentaServiceImplTest {

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ProductoRepository productoRepository;

    private VentaServiceImpl service;

    private Cliente cliente;
    private Producto paracetamol;
    private Producto ibuprofeno;

    @BeforeEach
    void setUp() {
        service = new VentaServiceImpl(
                ventaRepository,
                clienteRepository,
                productoRepository,
                new VentaMapper());

        cliente = cliente(1L, true);
        paracetamol = producto(10L, "Paracetamol", "2.50", 100, true);
        ibuprofeno = producto(20L, "Ibuprofeno", "4.00", 5, true);
    }

    @Nested
    @DisplayName("registrar")
    class Registrar {

        @Test
        @DisplayName("calcula subtotales y total, descuenta stock y congela el precio")
        void registraVentaCorrectamente() {

            when(clienteRepository.findById(1L))
                    .thenReturn(Optional.of(cliente));
            when(productoRepository.findById(10L))
                    .thenReturn(Optional.of(paracetamol));
            when(productoRepository.findById(20L))
                    .thenReturn(Optional.of(ibuprofeno));
            when(productoRepository.descontarStock(anyLong(), any()))
                    .thenReturn(1);
            when(ventaRepository.save(any(Venta.class)))
                    .thenAnswer(inv -> {
                        Venta v = inv.getArgument(0);
                        v.setId(500L);
                        return v;
                    });

            VentaResponseDTO response = service.registrar(
                    request(1L, item(10L, 4), item(20L, 3)));

            // Respuesta
            assertThat(response.getId()).isEqualTo(500L);
            assertThat(response.getClienteId()).isEqualTo(1L);
            assertThat(response.getClienteNombre()).isEqualTo("Ana Perez");
            assertThat(response.getEstado()).isEqualTo("REGISTRADA");
            assertThat(response.getTotal())
                    .isEqualByComparingTo("22.00"); // 4*2.50 + 3*4.00
            assertThat(response.getDetalles()).hasSize(2);
            assertThat(response.getDetalles().get(0).getSubtotal())
                    .isEqualByComparingTo("10.00");
            assertThat(response.getDetalles().get(1).getSubtotal())
                    .isEqualByComparingTo("12.00");

            // Entidad persistida
            ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
            verify(ventaRepository).save(captor.capture());
            Venta guardada = captor.getValue();

            assertThat(guardada.getCliente()).isSameAs(cliente);
            assertThat(guardada.getEstado()).isEqualTo(EstadoVenta.REGISTRADA);
            assertThat(guardada.getFecha()).isNotNull();
            assertThat(guardada.getTotal()).isEqualByComparingTo("22.00");
            assertThat(guardada.getDetalles())
                    .hasSize(2)
                    .allSatisfy(d -> assertThat(d.getVenta()).isSameAs(guardada));

            DetalleVenta d1 = guardada.getDetalles().get(0);
            assertThat(d1.getProducto()).isSameAs(paracetamol);
            assertThat(d1.getCantidad()).isEqualTo(4);
            assertThat(d1.getPrecio()).isEqualByComparingTo("2.50");

            // Stock descontado con el UPDATE condicional, una vez por ítem,
            // nunca modificando la entidad en memoria (evitaría que el
            // dirty checking pise el valor calculado por la base).
            verify(productoRepository).descontarStock(10L, 4);
            verify(productoRepository).descontarStock(20L, 3);
            assertThat(paracetamol.getStock()).isEqualTo(100);
            assertThat(ibuprofeno.getStock()).isEqualTo(5);
        }

        @Test
        @DisplayName("descuenta el stock antes de guardar la venta")
        void descuentaStockAntesDeGuardar() {

            when(clienteRepository.findById(1L))
                    .thenReturn(Optional.of(cliente));
            when(productoRepository.findById(20L))
                    .thenReturn(Optional.of(ibuprofeno));
            when(productoRepository.descontarStock(20L, 5))
                    .thenReturn(1);
            when(ventaRepository.save(any(Venta.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.registrar(request(1L, item(20L, 5)));

            InOrder enOrden = inOrder(productoRepository, ventaRepository);
            enOrden.verify(productoRepository).descontarStock(20L, 5);
            enOrden.verify(ventaRepository).save(any(Venta.class));
        }

        @Test
        @DisplayName("rechaza cliente inexistente con 404 y no guarda")
        void clienteNoEncontrado() {

            when(clienteRepository.findById(99L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.registrar(request(99L, item(10L, 1))))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("Cliente no encontrado con id: 99");

            verify(ventaRepository, never()).save(any());
        }

        @Test
        @DisplayName("rechaza cliente inactivo y no toca productos")
        void clienteInactivo() {

            when(clienteRepository.findById(1L))
                    .thenReturn(Optional.of(cliente(1L, false)));

            assertThatThrownBy(() ->
                    service.registrar(request(1L, item(10L, 1))))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("cliente inactivo");

            verify(productoRepository, never()).findById(anyLong());
            verify(ventaRepository, never()).save(any());
        }

        @Test
        @DisplayName("rechaza producto inexistente con 404")
        void productoNoEncontrado() {

            when(clienteRepository.findById(1L))
                    .thenReturn(Optional.of(cliente));
            when(productoRepository.findById(77L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.registrar(request(1L, item(77L, 1))))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("Producto no encontrado con id: 77");

            verify(ventaRepository, never()).save(any());
        }

        @Test
        @DisplayName("rechaza producto inactivo")
        void productoInactivo() {

            Producto inactivo = producto(30L, "Amoxicilina", "9.90", 50, false);

            when(clienteRepository.findById(1L))
                    .thenReturn(Optional.of(cliente));
            when(productoRepository.findById(30L))
                    .thenReturn(Optional.of(inactivo));

            assertThatThrownBy(() ->
                    service.registrar(request(1L, item(30L, 1))))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("Amoxicilina")
                    .hasMessageContaining("inactivo");

            verify(productoRepository, never()).descontarStock(anyLong(), any());
            verify(ventaRepository, never()).save(any());
        }

        @Test
        @DisplayName("rechaza stock insuficiente cuando la base no acepta el descuento")
        void stockInsuficiente() {

            when(clienteRepository.findById(1L))
                    .thenReturn(Optional.of(cliente));
            when(productoRepository.findById(20L))
                    .thenReturn(Optional.of(ibuprofeno));
            // La base decide: el UPDATE condicional no afectó filas.
            when(productoRepository.descontarStock(20L, 6))
                    .thenReturn(0);

            assertThatThrownBy(() ->
                    service.registrar(request(1L, item(20L, 6))))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("Stock insuficiente para Ibuprofeno")
                    .hasMessageContaining("Disponible: 5")
                    .hasMessageContaining("solicitado: 6");

            verify(ventaRepository, never()).save(any());
        }

        @Test
        @DisplayName("stock que se agota entre la lectura y el descuento (carrera) se rechaza")
        void carreraDeStock() {

            // En memoria el producto muestra stock 5, pero otra venta
            // concurrente ya se lo llevó: el UPDATE condicional devuelve 0.
            when(clienteRepository.findById(1L))
                    .thenReturn(Optional.of(cliente));
            when(productoRepository.findById(20L))
                    .thenReturn(Optional.of(ibuprofeno));
            when(productoRepository.descontarStock(20L, 5))
                    .thenReturn(0);

            assertThatThrownBy(() ->
                    service.registrar(request(1L, item(20L, 5))))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("Stock insuficiente");

            verify(ventaRepository, never()).save(any());
        }

        @Test
        @DisplayName("si falla un ítem posterior no se persiste nada")
        void fallaEnSegundoItemNoGuarda() {

            when(clienteRepository.findById(1L))
                    .thenReturn(Optional.of(cliente));
            when(productoRepository.findById(10L))
                    .thenReturn(Optional.of(paracetamol));
            when(productoRepository.findById(20L))
                    .thenReturn(Optional.of(ibuprofeno));
            when(productoRepository.descontarStock(10L, 2)).thenReturn(1);
            when(productoRepository.descontarStock(20L, 50)).thenReturn(0);

            assertThatThrownBy(() ->
                    service.registrar(
                            request(1L, item(10L, 2), item(20L, 50))))
                    .isInstanceOf(ReglaNegocioException.class);

            // El descuento del primer ítem ya se ejecutó en la base; la
            // excepción hace que @Transactional lo revierta. Aquí lo que
            // importa es que nunca se llama a save.
            verify(productoRepository).descontarStock(10L, 2);
            verify(ventaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("buscar por id")
    class BuscarPorId {

        @Test
        void devuelveVentaConDetalles() {

            Venta venta = ventaPersistida(7L);
            when(ventaRepository.findById(7L))
                    .thenReturn(Optional.of(venta));

            VentaResponseDTO response = service.buscar(7L);

            assertThat(response.getId()).isEqualTo(7L);
            assertThat(response.getDetalles()).hasSize(1);
            assertThat(response.getDetalles().get(0).getProductoNombre())
                    .isEqualTo("Paracetamol");
        }

        @Test
        void lanza404SiNoExiste() {

            when(ventaRepository.findById(7L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscar(7L))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("Venta no encontrada con id: 7");
        }
    }

    @Nested
    @DisplayName("buscar con filtros")
    class BuscarConFiltros {

        @Test
        @DisplayName("convierte fechas a rango completo del día y arma el Pageable")
        void buscaConFiltrosYPaginacion() {

            Venta venta = ventaPersistida(7L);
            Page<Venta> page = new PageImpl<>(
                    List.of(venta), PageRequest.of(0, 20), 1);

            when(ventaRepository.buscar(
                    eq(1L),
                    eq(EstadoVenta.REGISTRADA),
                    eq(LocalDate.of(2026, 9, 1).atStartOfDay()),
                    eq(LocalDateTime.of(2026, 9, 30, 23, 59, 59, 999_999_999)),
                    any(Pageable.class)))
                    .thenReturn(page);
            when(ventaRepository.cargarDetalles(any()))
                    .thenReturn(List.of(venta));

            PaginaResponseDTO<VentaResponseDTO> resultado = service.buscar(
                    1L,
                    EstadoVenta.REGISTRADA,
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 9, 30),
                    0, 20, "total", "asc");

            assertThat(resultado.getContenido()).hasSize(1);
            assertThat(resultado.getTotalElementos()).isEqualTo(1);
            assertThat(resultado.getPagina()).isZero();
            assertThat(resultado.isUltima()).isTrue();

            ArgumentCaptor<Pageable> captor =
                    ArgumentCaptor.forClass(Pageable.class);
            verify(ventaRepository).buscar(
                    any(), any(), any(), any(), captor.capture());
            Pageable pageable = captor.getValue();

            assertThat(pageable.getPageNumber()).isZero();
            assertThat(pageable.getPageSize()).isEqualTo(20);
            assertThat(pageable.getSort().getOrderFor("total"))
                    .isNotNull()
                    .extracting(Sort.Order::getDirection)
                    .isEqualTo(Sort.Direction.ASC);
        }

        @Test
        @DisplayName("sin filtros envía nulos y no carga detalles de una página vacía")
        void sinFiltrosPaginaVacia() {

            when(ventaRepository.buscar(
                    isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(Page.empty(PageRequest.of(0, 20)));

            PaginaResponseDTO<VentaResponseDTO> resultado =
                    service.listar(0, 20, null, null);

            assertThat(resultado.getContenido()).isEmpty();
            assertThat(resultado.getTotalElementos()).isZero();
            verify(ventaRepository, never()).cargarDetalles(any());
        }

        @Test
        void rechazaRangoDeFechasInvertido() {

            assertThatThrownBy(() -> service.buscar(
                    null, null,
                    LocalDate.of(2026, 9, 30),
                    LocalDate.of(2026, 9, 1),
                    0, 20, "fecha", "desc"))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("rango de fechas es inválido");

            verify(ventaRepository, never())
                    .buscar(any(), any(), any(), any(), any());
        }

        @Test
        void rechazaCampoDeOrdenNoPermitido() {

            assertThatThrownBy(() -> service.buscar(
                    null, null, null, null,
                    0, 20, "cliente.dni", "asc"))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("'cliente.dni' no está permitido");
        }

        @Test
        void rechazaDireccionNoPermitida() {

            assertThatThrownBy(() -> service.buscar(
                    null, null, null, null,
                    0, 20, "fecha", "sideways"))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("'sideways' no está permitida");
        }

        @Test
        void rechazaTamanioDePaginaFueraDeRango() {

            assertThatThrownBy(() -> service.listar(0, 0, null, null))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("entre 1 y 100");

            assertThatThrownBy(() -> service.listar(0, 101, null, null))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("entre 1 y 100");

            assertThatThrownBy(() -> service.listar(-1, 20, null, null))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("no puede ser negativa");
        }
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private static Cliente cliente(Long id, boolean activo) {
        Cliente c = new Cliente();
        c.setId(id);
        c.setDni("12345678");
        c.setNombres("Ana");
        c.setApellidos("Perez");
        c.setEmail("ana@correo.com");
        c.setEstado(activo);
        return c;
    }

    private static Producto producto(Long id, String nombre, String precio,
                                     int stock, boolean activo) {
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("Analgesicos");

        Producto p = new Producto();
        p.setId(id);
        p.setNombre(nombre);
        p.setPrecio(new BigDecimal(precio));
        p.setStock(stock);
        p.setEstado(activo);
        p.setCategoria(categoria);
        return p;
    }

    private Venta ventaPersistida(Long id) {
        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(paracetamol);
        detalle.setCantidad(2);
        detalle.setPrecio(paracetamol.getPrecio());
        detalle.setSubtotal(new BigDecimal("5.00"));

        Venta v = new Venta();
        v.setId(id);
        v.setCliente(cliente);
        v.setFecha(LocalDateTime.of(2026, 9, 15, 10, 0));
        v.setEstado(EstadoVenta.REGISTRADA);
        v.setTotal(new BigDecimal("5.00"));
        v.agregarDetalle(detalle);
        return v;
    }

    private static VentaRequestDTO request(Long clienteId,
                                           DetalleVentaRequestDTO... items) {
        return new VentaRequestDTO(clienteId, List.of(items));
    }

    private static DetalleVentaRequestDTO item(Long productoId, int cantidad) {
        return new DetalleVentaRequestDTO(productoId, cantidad);
    }
}
