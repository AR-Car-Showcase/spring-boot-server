package com.arcarshowcaseserver.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", 
    uniqueConstraints = { 
      @UniqueConstraint(columnNames = "username"),
      @UniqueConstraint(columnNames = "email") 
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 20)
    private String username;

    @NotBlank
    @Size(max = 50)
    @Email
    private String email;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @NotBlank
    @Size(max = 120)
    private String password;

    @Size(max = 20)
    @Column(name = "auth_provider", length = 20)
    private String authProvider = "LOCAL";

    @Size(max = 100)
    @Column(name = "external_subject", unique = true, length = 100)
    private String externalSubject;

    @Size(max = 50)
    @Column(name = "external_email", length = 50)
    private String externalEmail;

    @Column(name = "external_email_verified")
    private Boolean externalEmailVerified = false;

    @Column(name = "profile_completed", nullable = false)
    private Boolean profileCompleted = true;

    @Size(max = 20)
    private String phoneNumber;

    @Size(max = 60)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(columnDefinition = "TEXT")
    private String profilePic;

    @ElementCollection
    private Set<String> favBrands = new HashSet<>();

    @ElementCollection
    private Set<String> preferredBodyTypes = new HashSet<>();
    
    @ElementCollection
    private Set<String> preferredFuelTypes = new HashSet<>();

    @ElementCollection
    private Set<String> preferredTransmissions = new HashSet<>();

    @Size(max = 50)
    private String drivingCondition;

    private Double maxBudget;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(  name = "user_roles", 
        joinColumns = @JoinColumn(name = "user_id"), 
        inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<com.arcarshowcaseserver.model.Role> roles = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "permission", length = 100)
    private Set<String> permissions = new HashSet<>();

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public void setExternalSubject(String externalSubject) {
        this.externalSubject = externalSubject;
    }

    public void setExternalEmail(String externalEmail) {
        this.externalEmail = externalEmail;
    }

    public void setExternalEmailVerified(Boolean externalEmailVerified) {
        this.externalEmailVerified = externalEmailVerified;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public String getExternalSubject() {
        return externalSubject;
    }

    public String getExternalEmail() {
        return externalEmail;
    }

    public Boolean getExternalEmailVerified() {
        return externalEmailVerified;
    }

    @PrePersist
    @PreUpdate
    void ensureIdentityDefaults() {
        if (authProvider == null || authProvider.isBlank()) {
            authProvider = "LOCAL";
        }
        if (emailVerified == null) {
            emailVerified = false;
        }
        if (externalEmailVerified == null) {
            externalEmailVerified = false;
        }
        if (profileCompleted == null) {
            profileCompleted = true;
        }
    }
}
