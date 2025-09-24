package com.dekalib.app.data.repositories;

import com.dekalib.app.data.entities.UserApplication;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserApplicationRepository extends CrudRepository<UserApplication, Long> {
    @Query("SELECT u.userId FROM UserApplication u WHERE u.promo = :promo")
    Long findIdByPromo(@Param("promo") String promo);

}