package com.dekalib.app.data.repositories;

import com.dekalib.app.data.entities.Domen;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DomenRepository extends CrudRepository<Domen, String> {
}
