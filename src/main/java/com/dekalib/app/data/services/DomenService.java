package com.dekalib.app.data.services;

import com.dekalib.app.data.entities.Domen;
import com.dekalib.app.data.repositories.DomenRepository;
import com.dekalib.app.data.repositories.UserApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DomenService {
    private final DomenRepository domenRepository;
    private final UserApplicationRepository userRepository;
    @Autowired
    public DomenService(DomenRepository domenRepository,
                        UserApplicationRepository userRepository) {
        this.domenRepository = domenRepository;
        this.userRepository = userRepository;
    }

    public Iterable<Domen> current() {
        List<Domen> list = (List<Domen>) domenRepository.findAll();
        List<Domen> res = new ArrayList<>();
        for(Domen domen : list) {
            if (domen.isCurrent()) {
                res.add(domen);
            }
        }
        return res;
    }

    public void add(String domenName) {
        Domen domen = new Domen();
        domen.setCurrent(true);
        domen.setDomeName(domenName);
        domenRepository.save(domen);
    }

    public String links(long id) {
        if (userRepository.findById(id).isPresent()) {
            StringBuilder sb = new StringBuilder();
            String promo = userRepository.findById(id).get().getPromo();
            List<Domen> domens = (List<Domen>) current();
            for(Domen domen : domens) {
                sb.append(domen.getDomeName()).append("/main?promo=").append(promo);
            }
            return sb.toString();
        }
        return "-";
    }
}
