package com.example.iaspringlab.account;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DefaultAccountsInitializer implements ApplicationRunner {

    private final AccountService accountService;

    public DefaultAccountsInitializer(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public void run(ApplicationArguments args) {
        accountService.createDefaultAccounts();
    }
}
