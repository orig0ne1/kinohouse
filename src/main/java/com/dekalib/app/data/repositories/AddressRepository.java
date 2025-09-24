package com.dekalib.app.data.repositories;

import com.dekalib.app.data.entities.Address;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepository extends CrudRepository<Address, String> {
    List<Address> findAllByPromo(String promo);
}