package com.dekalib.app.data.services;

import com.dekalib.app.data.entities.Address;
import com.dekalib.app.data.repositories.AddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AddressService {
    private final AddressRepository addressRepository;

    @Autowired
    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public void insert(String addressName, String promo) {
        Address address = new Address();
        address.setAddressName(addressName);
        address.setPromo(promo);
        addressRepository.save(address);
    }

    public String showAddresses(String promo) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ваши адреса: \n");
        for(Address a : addressRepository.findAllByPromo(promo)) {
            sb.append("""
                    %s. %s
                    """.formatted(a.getId(), a.getAddressName()));
        }
        return sb.toString();
    }

}
