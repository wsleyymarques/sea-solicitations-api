package br.com.sea.solicitations.solicitation.adapter.out.viacep;

import br.com.sea.solicitations.solicitation.application.port.out.PostalCodeLookup;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ViaCepProperties.class)
public class ViaCepConfiguration {

    @Bean
    PostalCodeLookup postalCodeLookup(
            RestClient.Builder restClientBuilder,
            ViaCepProperties properties
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        RestClient restClient = restClientBuilder
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .build();

        return new ViaCepPostalCodeAdapter(restClient);
    }
}
