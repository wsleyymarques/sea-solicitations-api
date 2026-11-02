package br.com.sea.solicitations.solicitation.adapter.out.viacep;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.application.exception.PostalCodeNotFoundException;
import br.com.sea.solicitations.solicitation.application.exception.PostalCodeServiceUnavailableException;
import br.com.sea.solicitations.solicitation.application.port.out.PostalCodeData;
import br.com.sea.solicitations.solicitation.application.port.out.PostalCodeLookup;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Locale;
import java.util.Objects;

public class ViaCepPostalCodeAdapter implements PostalCodeLookup {

    private final RestClient restClient;

    public ViaCepPostalCodeAdapter(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient, "ViaCEP RestClient is required");
    }

    @Override
    public PostalCodeData find(String postalCode) {
        Objects.requireNonNull(postalCode, "Postal code is required");

        try {
            ViaCepResponse response = restClient.get()
                    .uri("/ws/{postalCode}/json/", postalCode)
                    .retrieve()
                    .onStatus(
                            status -> !status.is2xxSuccessful(),
                            (request, clientResponse) -> {
                                throw new PostalCodeServiceUnavailableException();
                            }
                    )
                    .body(ViaCepResponse.class);

            return toPostalCodeData(response);
        } catch (PostalCodeNotFoundException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw new PostalCodeServiceUnavailableException(exception);
        } catch (RestClientException exception) {
            throw new PostalCodeServiceUnavailableException(exception);
        }
    }

    private PostalCodeData toPostalCodeData(ViaCepResponse response) {
        if (response == null || Boolean.TRUE.equals(response.erro())) {
            throw new PostalCodeNotFoundException();
        }

        try {
            return new PostalCodeData(
                    normalizePostalCode(response.cep()),
                    response.street(),
                    response.neighborhood(),
                    response.city(),
                    parseState(response.state())
            );
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new PostalCodeNotFoundException();
        }
    }

    private String normalizePostalCode(String postalCode) {
        if (postalCode == null) {
            throw new IllegalArgumentException("Postal code is required");
        }
        String normalized = postalCode.replace("-", "");
        if (!normalized.matches("\\d{8}")) {
            throw new IllegalArgumentException("Postal code must have 8 digits");
        }
        return normalized;
    }

    private FederativeUnit parseState(String state) {
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("State is required");
        }
        return FederativeUnit.valueOf(state.trim().toUpperCase(Locale.ROOT));
    }
}
