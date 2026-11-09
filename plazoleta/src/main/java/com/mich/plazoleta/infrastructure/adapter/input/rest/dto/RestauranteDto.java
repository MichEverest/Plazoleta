package com.mich.plazoleta.infrastructure.adapter.input.rest.dto;

import com.mich.plazoleta.domain.model.Restaurante;
import com.mich.plazoleta.domain.model.Usuario;

public final class RestauranteDto {

    private RestauranteDto() {
    }

    public record Request(String nombre, String nit, String direccion, String telefono, String urlLogo,
                          Long idPropietario) {

        public Restaurante toDomain() {
            return new Restaurante(null, nombre, nit, direccion, telefono, urlLogo, idPropietario);
        }
    }

    public record Response(Long id, String nombre, String nit, String direccion, String telefono, String urlLogo,
                           Long idPropietario) {

        public static Response de(Restaurante restaurante) {
            return new Response(restaurante.id(), restaurante.nombre(), restaurante.nit(), restaurante.direccion(),
                    restaurante.telefono(), restaurante.urlLogo(), restaurante.idPropietario());
        }
    }

    public record Resumen(Long id, String nombre, String urlLogo) {

        public static Resumen de(Restaurante restaurante) {
            return new Resumen(restaurante.id(), restaurante.nombre(), restaurante.urlLogo());
        }
    }

    public record Empleado(String nombre, String apellido, String numeroDocumento, String celular, String correo,
                           String clave) {

        public Usuario toDomain() {
            return new Usuario(null, nombre, apellido, numeroDocumento, celular, correo, clave, Usuario.Rol.EMPLEADO);
        }
    }

    public record EmpleadoCreado(Long id) {
    }
}
