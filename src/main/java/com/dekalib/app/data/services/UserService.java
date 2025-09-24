package com.dekalib.app.data.services;

import com.dekalib.app.data.entities.UserApplication;
import com.dekalib.app.data.repositories.UserApplicationRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserApplicationRepository userRepository;
    @Autowired
    public UserService(UserApplicationRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserApplication getById(long id) {
        return userRepository.findById(id).orElseThrow();
    }

    public long getIdByPromo(String promo) {
        return userRepository.findIdByPromo(promo);
    }

    public List<UserApplication> getUsers() {
        return (List<UserApplication>) userRepository.findAll();
    }
}