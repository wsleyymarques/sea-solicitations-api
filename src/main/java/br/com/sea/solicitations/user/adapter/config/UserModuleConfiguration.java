package br.com.sea.solicitations.user.adapter.config;

import br.com.sea.solicitations.user.application.port.in.AuthenticateUserUseCase;
import br.com.sea.solicitations.user.application.port.in.CreateInternalUserUseCase;
import br.com.sea.solicitations.user.application.port.in.RegisterClientUseCase;
import br.com.sea.solicitations.user.application.port.out.AccessTokenGenerator;
import br.com.sea.solicitations.user.application.port.out.PasswordHasher;
import br.com.sea.solicitations.user.application.port.out.UserRepository;
import br.com.sea.solicitations.user.application.service.AuthenticateUserService;
import br.com.sea.solicitations.user.application.service.CreateInternalUserService;
import br.com.sea.solicitations.user.application.service.RegisterClientService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class UserModuleConfiguration {

    @Bean
    RegisterClientUseCase registerClientUseCase(
            UserRepository userRepository,
            PasswordHasher passwordHasher
    ) {
        return new RegisterClientService(userRepository, passwordHasher);
    }

    @Bean
    CreateInternalUserUseCase createInternalUserUseCase(
            UserRepository userRepository,
            PasswordHasher passwordHasher
    ) {
        return new CreateInternalUserService(userRepository, passwordHasher);
    }

    @Bean
    AuthenticateUserUseCase authenticateUserUseCase(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            AccessTokenGenerator accessTokenGenerator
    ) {
        return new AuthenticateUserService(
                userRepository,
                passwordHasher,
                accessTokenGenerator
        );
    }
}
