package com.arcarshowcaseserver.exception;

public class AccountNotVerifiedException extends RuntimeException {
    private final String email;
    
    public AccountNotVerifiedException(String email) {
        super("Account not verified. Please check your email for the verification code.");
        this.email = email;
    }
    
    public String getEmail() {
        return email;
    }
}
