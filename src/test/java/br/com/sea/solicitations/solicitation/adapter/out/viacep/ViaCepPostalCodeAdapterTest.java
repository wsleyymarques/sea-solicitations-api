package br.com.sea.solicitations.solicitation.adapter.out.viacep;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.application.exception.PostalCodeNotFoundException;
import br.com.sea.solicitations.solicitation.application.exception.PostalCodeServiceUnavailableException;
import br.com.sea.solicitations.solicitation.application.port.out.PostalCodeData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ViaCepPostalCodeAdapterTest {

    private MockRestServiceServer server;
    private ViaCepPostalCodeAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://viacep.test");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new ViaCepPostalCodeAdapter(builder.build());
    }

    @Test
    void returnsAddressDataFromViaCep() {
        server.expect(once(), requestTo("https://viacep.test/ws/01001000/json/"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {
                          "cep": "01001-000",
                          "logradouro": "Pra\u00e7a da S\u00e9",
                          "complemento": "lado \u00edmpar",
                          "unidade": "",
                          "bairro": "S\u00e9",
                          "localidade": "S\u00e3o Paulo",
                          "uf": "SP",
                          "estado": "S\u00e3o Paulo",
                          "regiao": "Sudeste",
                          "ibge": "3550308",
                          "gia": "1004",
                          "ddd": "11",
                          "siafi": "7107"
                        }
                        """, MediaType.APPLICATION_JSON));

        PostalCodeData result = adapter.find("01001000");

        assertThat(result.postalCode()).isEqualTo("01001000");
        assertThat(result.street()).isEqualTo("Pra\u00e7a da S\u00e9");
        assertThat(result.neighborhood()).isEqualTo("S\u00e9");
        assertThat(result.city()).isEqualTo("S\u00e3o Paulo");
        assertThat(result.state()).isEqualTo(FederativeUnit.SP);
        server.verify();
    }

    @Test
    void reportsPostalCodeNotFoundWhenViaCepReturnsErrorFlag() {
        server.expect(once(), requestTo("https://viacep.test/ws/99999999/json/"))
                .andRespond(withSuccess("{\"erro\": true}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.find("99999999"))
                .isInstanceOf(PostalCodeNotFoundException.class);
        server.verify();
    }

    @Test
    void reportsUnavailableWhenViaCepReturnsServerError() {
        server.expect(once(), requestTo("https://viacep.test/ws/01001000/json/"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> adapter.find("01001000"))
                .isInstanceOf(PostalCodeServiceUnavailableException.class);
        server.verify();
    }

    @Test
    void reportsUnavailableWhenViaCepRateLimitsTheApplication() {
        server.expect(once(), requestTo("https://viacep.test/ws/01001000/json/"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> adapter.find("01001000"))
                .isInstanceOf(PostalCodeServiceUnavailableException.class);
        server.verify();
    }

    @Test
    void reportsUnavailableWhenViaCepRedirectsUnexpectedly() {
        server.expect(once(), requestTo("https://viacep.test/ws/01001000/json/"))
                .andRespond(withStatus(HttpStatus.FOUND)
                        .location(java.net.URI.create("https://other-provider.test")));

        assertThatThrownBy(() -> adapter.find("01001000"))
                .isInstanceOf(PostalCodeServiceUnavailableException.class);
        server.verify();
    }

    @Test
    void reportsUnavailableWhenTransportFails() {
        server.expect(once(), requestTo("https://viacep.test/ws/01001000/json/"))
                .andRespond(request -> {
                    throw new IOException("read timed out");
                });

        assertThatThrownBy(() -> adapter.find("01001000"))
                .isInstanceOf(PostalCodeServiceUnavailableException.class);
        server.verify();
    }

    @Test
    void rejectsIncompleteAddressReturnedByViaCep() {
        server.expect(once(), requestTo("https://viacep.test/ws/01001000/json/"))
                .andRespond(withSuccess("""
                        {
                          "cep": "01001-000",
                          "logradouro": "",
                          "bairro": "S\u00e9",
                          "localidade": "S\u00e3o Paulo",
                          "uf": "SP"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.find("01001000"))
                .isInstanceOf(PostalCodeNotFoundException.class);
        server.verify();
    }
}
