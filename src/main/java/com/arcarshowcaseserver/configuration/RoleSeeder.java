package com.arcarshowcaseserver.configuration;

import com.arcarshowcaseserver.enums.RoleType;
import com.arcarshowcaseserver.model.Role;
import com.arcarshowcaseserver.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class RoleSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RoleSeeder.class);
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedRoleIfMissing(RoleType.DEFAULT);
        seedRoleIfMissing(RoleType.ADMIN);
    }

    private void seedRoleIfMissing(RoleType roleType) {
        List<Role> roles = roleRepository.findAllByName(roleType);
        if (roles.size() > 1) {
            for (int i = 1; i < roles.size(); i++) {
                roleRepository.delete(roles.get(i));
            }
            log.warn(">>> Removed {} duplicate '{}' role rows", roles.size() - 1, roleType);
            return;
        }

        if (roles.size() == 1) {
            return;
        }

        Role role = new Role();
        role.setName(roleType);
        roleRepository.save(role);
        log.info(">>> Seeded missing role: {}", roleType);
    }
}
