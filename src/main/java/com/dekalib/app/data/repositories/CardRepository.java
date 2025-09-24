package com.dekalib.app.data.repositories;

import com.dekalib.app.data.entities.Card;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardRepository extends CrudRepository<Card, String> {
    @Query("SELECT c FROM Card c WHERE c.country = :country")
    Card getCardByCountry(String country);
}
