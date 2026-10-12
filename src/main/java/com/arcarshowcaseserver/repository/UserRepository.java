package com.arcarshowcaseserver.repository;

import com.arcarshowcaseserver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByExternalSubject(String externalSubject);
    Optional<User> findByExternalEmail(String externalEmail);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    Boolean existsByEmailIgnoreCase(String email);

    /*
     * The preference collections are lazy @ElementCollections. Reading them off a
     * detached User only works while open-session-in-view holds a session open for
     * the request, which makes every caller outside an HTTP request fail. These
     * projections fetch the same rows without needing a session afterwards, so the
     * recommendation path can snapshot preferences and then make its HTTP call with
     * no transaction held open across the network.
     */

    @Query("SELECT b FROM User u JOIN u.favBrands b WHERE u.id = :userId")
    List<String> findFavBrands(@Param("userId") Long userId);

    @Query("SELECT t FROM User u JOIN u.preferredBodyTypes t WHERE u.id = :userId")
    List<String> findPreferredBodyTypes(@Param("userId") Long userId);

    @Query("SELECT t FROM User u JOIN u.preferredFuelTypes t WHERE u.id = :userId")
    List<String> findPreferredFuelTypes(@Param("userId") Long userId);

    @Query("SELECT t FROM User u JOIN u.preferredTransmissions t WHERE u.id = :userId")
    List<String> findPreferredTransmissions(@Param("userId") Long userId);
}
