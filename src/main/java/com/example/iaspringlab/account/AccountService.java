package com.example.iaspringlab.account;

import com.example.iaspringlab.customer.Customer;
import com.example.iaspringlab.customer.CustomerRepository;
import com.example.iaspringlab.exception.ResourceNotFoundException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AccountService implements UserDetailsService {

    static final String DEFAULT_ADMIN = "admin";
    static final String DEFAULT_USER = "user";
    static final String DEFAULT_USER_EMAIL = "user@ia-spring-lab.local";

    private final AppUserRepository appUserRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AppUserRepository appUserRepository, CustomerRepository customerRepository,
                          PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user " + username));
        return User.withUsername(appUser.getUsername())
                .password(appUser.getPassword())
                .roles(appUser.getRole().name())
                .build();
    }

    /**
     * Id of the customer the given account shops for.
     */
    @Transactional(readOnly = true)
    public Long getCustomerId(String username) {
        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + username));
        if (appUser.getCustomer() == null) {
            throw new ResourceNotFoundException("Account '" + username + "' is not linked to a customer.");
        }
        return appUser.getCustomer().getId();
    }

    /**
     * Creates the default accounts (admin/admin and user/user) when they do not exist yet.
     */
    public void createDefaultAccounts() {
        if (!appUserRepository.existsByUsername(DEFAULT_ADMIN)) {
            appUserRepository.save(new AppUser(DEFAULT_ADMIN, passwordEncoder.encode(DEFAULT_ADMIN), Role.ADMIN, null));
        }
        if (!appUserRepository.existsByUsername(DEFAULT_USER)) {
            Customer customer = customerRepository.findByEmailIgnoreCase(DEFAULT_USER_EMAIL)
                    .orElseGet(() -> customerRepository.save(new Customer("Utilisateur", "Démo", DEFAULT_USER_EMAIL)));
            appUserRepository.save(new AppUser(DEFAULT_USER, passwordEncoder.encode(DEFAULT_USER), Role.USER, customer));
        }
    }
}
