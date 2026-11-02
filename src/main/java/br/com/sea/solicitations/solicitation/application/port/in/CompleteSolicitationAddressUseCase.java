package br.com.sea.solicitations.solicitation.application.port.in;

@FunctionalInterface
public interface CompleteSolicitationAddressUseCase {

    CompleteSolicitationAddressResult complete(CompleteSolicitationAddressCommand command);
}
