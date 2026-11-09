package com.mich.plazoleta.infrastructure.config;

import com.mich.plazoleta.infrastructure.adapter.output.persistence.PlatoPersistenceAdapter.CategoriaJpaRepository;
import com.mich.plazoleta.infrastructure.adapter.output.persistence.entity.CategoriaEntity;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private final CategoriaJpaRepository categoriaJpaRepository;

    public DataInitializer(CategoriaJpaRepository categoriaJpaRepository) {
        this.categoriaJpaRepository = categoriaJpaRepository;
    }

    @Override
    public void run(String... args) {
        crearCategoria("ENTRADAS", "Entradas y aperitivos");
        crearCategoria("PLATOS FUERTES", "Platos principales");
        crearCategoria("POSTRES", "Postres y dulces");
        crearCategoria("BEBIDAS", "Bebidas frías y calientes");
    }

    private void crearCategoria(String nombre, String descripcion) {
        if (!categoriaJpaRepository.existsByNombre(nombre)) {
            categoriaJpaRepository.save(new CategoriaEntity(nombre, descripcion));
        }
    }
}
