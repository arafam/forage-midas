package com.jpmc.midascore.component;

import com.jpmc.midascore.repository.UserRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DebugUtils {
    private final UserRepository userRepository;

    public DebugUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void printWaldorfBalanceOnStartup() {
        userRepository.findAll().forEach(u -> {
            if ("waldorf".equals(u.getName())) {
                System.out.println(">>>> WALDORF BALANCE (DEBUG): " + u.getBalance());
            }
        });
    }
}


