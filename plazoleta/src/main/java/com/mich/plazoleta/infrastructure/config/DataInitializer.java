package com.mich.plazoleta.infrastructure.config;

import com.mich.plazoleta.infrastructure.adapter.output.persistence.PlatoPersistenceAdapter.CategoriaJpaRepository;
import com.mich.plazoleta.infrastructure.adapter.output.persistence.PlatoPersistenceAdapter.PlatoJpaRepository;
import com.mich.plazoleta.infrastructure.adapter.output.persistence.RestaurantePersistenceAdapter.RestauranteJpaRepository;
import com.mich.plazoleta.infrastructure.adapter.output.persistence.entity.CategoriaEntity;
import com.mich.plazoleta.infrastructure.adapter.output.persistence.entity.PlatoEntity;
import com.mich.plazoleta.infrastructure.adapter.output.persistence.entity.RestauranteEntity;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Siembra solo lo mínimo para empezar: las categorías (no hay endpoint para crearlas) y el restaurante de Cartethyia
 * (id 2 en la siembra de Usuarios) con su empleada Mari (id 4) y 5 platos exóticos, que se crean solo si el restaurante
 * aún no tiene platos. Pedidos y todo lo demás lo crean los usuarios.
 * Cada bloque solo se crea si aún no existe, así que reiniciar no duplica nada.
 */
@Component
@ConditionalOnProperty(name = "seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private static final Long ID_PROPIETARIO = 2L;
    private static final Long ID_EMPLEADA_MARI = 4L;
    private static final String URL_IMAGENES = "http://localhost:8082/img/";
    private static final String INDICATIVO_COLOMBIA = "+57";
    private static final String TELEFONO_EMPRESA = "3022249736";
    private static final String TELEFONO_ANTERIOR = "6044441234";

    private static final String ENTRADAS = "ENTRADAS";
    private static final String PLATOS_FUERTES = "PLATOS FUERTES";
    private static final String POSTRES = "POSTRES";
    private static final String BEBIDAS = "BEBIDAS";

    private final CategoriaJpaRepository categoriaJpaRepository;
    private final RestauranteJpaRepository restauranteJpaRepository;
    private final PlatoJpaRepository platoJpaRepository;

    public DataInitializer(CategoriaJpaRepository categoriaJpaRepository,
                           RestauranteJpaRepository restauranteJpaRepository,
                           PlatoJpaRepository platoJpaRepository) {
        this.categoriaJpaRepository = categoriaJpaRepository;
        this.restauranteJpaRepository = restauranteJpaRepository;
        this.platoJpaRepository = platoJpaRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        crearCategoria(ENTRADAS, "Entradas y aperitivos");
        crearCategoria(PLATOS_FUERTES, "Platos principales");
        crearCategoria(POSTRES, "Postres y dulces");
        crearCategoria(BEBIDAS, "Bebidas frías y calientes");
        restauranteJpaRepository.findAll().stream()
                .filter(restaurante -> restaurante.getTelefono().startsWith(INDICATIVO_COLOMBIA))
                .forEach(restaurante -> restaurante.setTelefono(
                        restaurante.getTelefono().substring(INDICATIVO_COLOMBIA.length())));

        RestauranteEntity restaurante = restauranteJpaRepository.findByIdPropietario(ID_PROPIETARIO)
                .orElseGet(() -> restauranteJpaRepository.save(new RestauranteEntity(null, "Sabores de Cartethyia",
                        "900123456", "Calle 10 # 5-20, Medellín", TELEFONO_EMPRESA, URL_IMAGENES + "logo.svg",
                        ID_PROPIETARIO)));
        if (TELEFONO_ANTERIOR.equals(restaurante.getTelefono())) {
            restaurante.setTelefono(TELEFONO_EMPRESA);
        }
        if (restauranteJpaRepository.buscarIdPorEmpleado(ID_EMPLEADA_MARI).isEmpty()) {
            restaurante.getEmpleados().add(ID_EMPLEADA_MARI);
        }
        if (!platoJpaRepository.existsByRestauranteId(restaurante.getId())) {
            crearMenu(restaurante);
        }
    }

    private void crearMenu(RestauranteEntity restaurante) {
        plato(restaurante, PLATOS_FUERTES, "Wagyu A5 con trufa negra",
                "Lomo de wagyu japonés A5 sellado a la piedra, láminas de trufa negra del Périgord y puré de papa criolla ahumada",
                185000, "wagyu.svg");
        plato(restaurante, ENTRADAS, "Ceviche de langosta del Pacífico",
                "Langosta de Bahía Solano en leche de tigre de maracuyá, ají amazónico y chips de plátano",
                68000, "langosta.svg");
        plato(restaurante, ENTRADAS, "Hormigas culonas de Santander",
                "Hormigas culonas tostadas sobre crocante de yuca, crema de aguacate y sal de limón mandarino",
                42000, "hormigas.svg");
        plato(restaurante, POSTRES, "Esfera de chocolate amazónico y oro",
                "Esfera de cacao amazónico al 72 % que se abre con salsa caliente de copoazú, decorada con hoja de oro comestible",
                55000, "chocolate.svg");
        plato(restaurante, BEBIDAS, "Cóctel de viche y lulo",
                "Viche del Pacífico, lulo fresco, panela y espuma de borojó",
                38000, "viche.svg");
    }

    private void plato(RestauranteEntity restaurante, String categoria, String nombre, String descripcion, int precio,
                       String imagen) {
        CategoriaEntity categoriaEntity = categoriaJpaRepository.findByNombre(categoria).orElseThrow();
        platoJpaRepository.save(new PlatoEntity(null, nombre, categoriaEntity, descripcion, precio,
                URL_IMAGENES + imagen, true, restaurante));
    }

    private void crearCategoria(String nombre, String descripcion) {
        if (!categoriaJpaRepository.existsByNombre(nombre)) {
            categoriaJpaRepository.save(new CategoriaEntity(nombre, descripcion));
        }
    }
}
