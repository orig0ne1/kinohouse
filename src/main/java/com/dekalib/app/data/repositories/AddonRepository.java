package com.dekalib.app.data.repositories;

import com.dekalib.app.data.entities.Addon;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddonRepository extends CrudRepository<Addon, Long> {
    @Query("SELECT a FROM Addon a WHERE a.promo = :promo")
    List<Addon> findByPromo(@Param("promo") String promo);
}