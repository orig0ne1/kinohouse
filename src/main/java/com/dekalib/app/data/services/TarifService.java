package com.dekalib.app.data.services;

import com.dekalib.app.data.entities.Tarif;
import com.dekalib.app.data.repositories.TarifRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TarifService {
    private final TarifRepository tarifRepository;

    @Autowired
    public TarifService(TarifRepository tarifRepository) {
        this.tarifRepository = tarifRepository;
    }

    public Tarif getByPromo(String promo) {
        return tarifRepository.findById(promo).orElseGet(() -> {
            Tarif defaultTarif = new Tarif();
            defaultTarif.setPromo(promo);
            defaultTarif.setStandart(2900);
            defaultTarif.setPlus(3500);
            defaultTarif.setVip(4900);
            return defaultTarif;
        });
    }

    public void saveTarif(Tarif tarif) {
        tarifRepository.save(tarif);
    }

    public void changeTarif(String promo, String tarifName, int newValue) {
        Tarif tarif = getByPromo(promo);
        switch (tarifName.toLowerCase()) {
            case "standart":
                tarif.setStandart(newValue);
                break;
            case "plus":
                tarif.setPlus(newValue);
                break;
            case "vip":
                tarif.setVip(newValue);
                break;
            default:
                throw new IllegalArgumentException("Unknown tariff: " + tarifName);
        }
        tarifRepository.save(tarif);
    }

}