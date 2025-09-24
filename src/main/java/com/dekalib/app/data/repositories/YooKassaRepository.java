package com.dekalib.app.data.repositories;

import com.dekalib.app.data.entities.YooKassa;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface YooKassaRepository extends CrudRepository<YooKassa, String> {
}