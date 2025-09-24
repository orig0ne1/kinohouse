package com.dekalib.app.data.repositories;

import com.dekalib.app.data.entities.Tarif;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TarifRepository extends CrudRepository<Tarif, String> {
}
