package br.com.sea.solicitations.solicitation.application.exception;

public class PostalCodeServiceUnavailableException extends RuntimeException {

    public PostalCodeServiceUnavailableException() {
        super("Postal code service is temporarily unavailable");
    }

    public PostalCodeServiceUnavailableException(Throwable cause) {
        super("Postal code service is temporarily unavailable", cause);
    }
}
