package com.bibliotec.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.bibliotec.model.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

}
