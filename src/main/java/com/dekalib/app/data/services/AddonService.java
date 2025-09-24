package com.dekalib.app.data.services;

import com.dekalib.app.data.entities.Addon;
import com.dekalib.app.data.repositories.AddonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AddonService {
    private final AddonRepository addonRepository;

    @Autowired
    public AddonService(AddonRepository addonRepository) {
        this.addonRepository = addonRepository;
    }

    public void saveAddon(Addon addon) {
        addonRepository.save(addon);
    }

    public List<Addon> getByPromo(String promo) {
        return addonRepository.findByPromo(promo);
    }

    public void initDefaultAddons(String promo) {
        String[] defaults = {"Свечи:200", "Попкорн:150", "Пицца:600", "Кальян:1000"};
        for (String def : defaults) {
            String[] parts = def.split(":");
            Addon addon = new Addon();
            addon.setId(UUID.randomUUID().getLeastSignificantBits());  // Simple ID gen
            addon.setPromo(promo);
            addon.setAddonId(parts[0].toLowerCase().replace(" ", "_"));
            addon.setAddonName(parts[0]);
            addon.setPrice(Integer.parseInt(parts[1]));
            saveAddon(addon);
        }
    }
}