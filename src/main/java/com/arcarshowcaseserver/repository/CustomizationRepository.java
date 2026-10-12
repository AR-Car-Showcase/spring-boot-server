package com.arcarshowcaseserver.repository;

import com.arcarshowcaseserver.model.Customization;
import com.arcarshowcaseserver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomizationRepository extends JpaRepository<Customization, UUID> {
    List<Customization> findByUser(User user);

    /**
     * Scoped by owner on purpose: a delete can never reach another user's row, so there
     * is no separate authorization step to forget.
     */
    Optional<Customization> findByIdAndUser(UUID id, User user);

    long countByUser(User user);
}
