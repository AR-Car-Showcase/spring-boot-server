package com.arcarshowcaseserver.repository;

import com.arcarshowcaseserver.enums.RoleType;
import com.arcarshowcaseserver.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(RoleType name);
    List<Role> findAllByName(RoleType name);
}
