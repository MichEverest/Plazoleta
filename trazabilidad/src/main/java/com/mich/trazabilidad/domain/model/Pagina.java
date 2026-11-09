package com.mich.trazabilidad.domain.model;

import com.mich.trazabilidad.domain.exception.ExcepcionesDominio.ValidacionException;

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

        public <T> Pagina<T> paginar(List<T> elementos) {
            int desde = Math.min(pagina * tamano, elementos.size());
            int hasta = Math.min(desde + tamano, elementos.size());
            int totalPaginas = (elementos.size() + tamano - 1) / tamano;
            return new Pagina<>(elementos.subList(desde, hasta), pagina, tamano, elementos.size(), totalPaginas);
        }
    }
}
