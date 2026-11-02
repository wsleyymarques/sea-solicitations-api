package br.com.sea.solicitations.solicitation.application.exception;

public class PostalCodeNotFoundException extends RuntimeException {

    public PostalCodeNotFoundException() {
        super("Postal code was not found");
    }
}
