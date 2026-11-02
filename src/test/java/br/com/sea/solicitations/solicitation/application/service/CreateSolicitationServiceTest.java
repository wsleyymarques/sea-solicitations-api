package br.com.sea.solicitations.solicitation.application.service;

import br.com.sea.solicitations.solicitation.application.port.in.CreateSolicitationCommand;
import br.com.sea.solicitations.solicitation.application.port.in.CreateSolicitationResult;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationRepository;
import br.com.sea.solicitations.solicitation.domain.ServiceType;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import br.com.sea.solicitations.solicitation.domain.SolicitationStatus;
import br.com.sea.solicitations.solicitation.domain.SolicitationStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateSolicitationServiceTest {

    @Mock
    private SolicitationRepository solicitationRepository;

    private CreateSolicitationService service;

    @BeforeEach
    void setUp() {
        service = new CreateSolicitationService(solicitationRepository);
    }

    @Test
    void createsDraftWithBasicDataAndReturnsNextStep() {
        UUID clientId = UUID.randomUUID();
        UUID solicitationId = UUID.randomUUID();
        CreateSolicitationCommand command = new CreateSolicitationCommand(
                clientId,
                ServiceType.INSTALLATION,
                "Instalacao de equipamento",
                "Solicito a instalacao de um novo equipamento no endereco informado."
        );
        when(solicitationRepository.save(any(Solicitation.class))).thenReturn(solicitationId);

        CreateSolicitationResult result = service.create(command);

        ArgumentCaptor<Solicitation> solicitationCaptor = ArgumentCaptor.forClass(Solicitation.class);
        verify(solicitationRepository).save(solicitationCaptor.capture());
        Solicitation solicitation = solicitationCaptor.getValue();

        assertThat(solicitation.isOwnedBy(clientId)).isTrue();
        assertThat(solicitation.getBasicData().getServiceType()).isEqualTo(ServiceType.INSTALLATION);
        assertThat(solicitation.getBasicData().getTitle()).isEqualTo("Instalacao de equipamento");
        assertThat(solicitation.getBasicData().getDescription())
                .isEqualTo("Solicito a instalacao de um novo equipamento no endereco informado.");
        assertThat(solicitation.getStatus()).isEqualTo(SolicitationStatus.DRAFT);
        assertThat(solicitation.getCurrentStep()).isEqualTo(SolicitationStep.ADDRESS);

        assertThat(result.id()).isEqualTo(solicitationId);
        assertThat(result.status()).isEqualTo(SolicitationStatus.DRAFT);
        assertThat(result.currentStep()).isEqualTo(SolicitationStep.ADDRESS);
    }
}
