package com.example.demo.exceptions;

import org.springframework.security.core.AuthenticationException;

public class AuthExc extends AuthenticationException {
    public AuthExc(String msg) {
        super(msg);
    }
}
