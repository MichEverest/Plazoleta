package com.mich.plazoleta.domain.model;

import com.mich.plazoleta.domain.exception.ExcepcionesDominio.ValidacionException;

import java.util.List;

public record Pagina<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    public record Solicitud(int pagina, int tamano) {

        private static final int TAMANO_MAXIMO = 100;
        private static final String MENSAJE_PAGINACION_INVALIDA = "La página debe ser mayor o igual a 0 y el tamaño entre 1 y 100";

        public Solicitud {
            if (pagina < 0 || tamano < 1 || tamano > TAMANO_MAXIMO) {
                throw new ValidacionException(MENSAJE_PAGINACION_INVALIDA);
            }
        }
    }
}
