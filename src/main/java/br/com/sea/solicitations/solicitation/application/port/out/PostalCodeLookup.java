package br.com.sea.solicitations.solicitation.application.port.out;

@FunctionalInterface
public interface PostalCodeLookup {

    PostalCodeData find(String postalCode);
}
