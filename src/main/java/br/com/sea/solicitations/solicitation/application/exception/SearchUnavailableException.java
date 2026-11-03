package br.com.sea.solicitations.solicitation.application.exception;

public class SearchUnavailableException extends RuntimeException {
    public SearchUnavailableException(Throwable cause) {
        super("Search service is temporarily unavailable", cause);
    }
}
