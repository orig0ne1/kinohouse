package com.dekalib.app.data.services;

import com.dekalib.app.data.entities.UserApplication;
import com.dekalib.app.data.repositories.UserApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AdminService {
    private final UserApplicationRepository userRepository;
    @Value("${application_bot.owner_id}")
    private long ownerId;
    @Autowired
    public AdminService(UserApplicationRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void changeRole(long id, String role, long userId) {
        if (id == ownerId && userRepository.findById(userId).isPresent()) {
            UserApplication user = userRepository.findById(userId).get();
            String previousRole = user.getRole();
            user.setRole(role);
        }
    }
}
