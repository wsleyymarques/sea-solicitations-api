package br.com.sea.solicitations.solicitation.adapter.out.search;

import br.com.sea.solicitations.shared.domain.FederativeUnit;
import br.com.sea.solicitations.solicitation.domain.Address;
import br.com.sea.solicitations.solicitation.domain.BasicData;
import br.com.sea.solicitations.solicitation.domain.Confirmation;
import br.com.sea.solicitations.solicitation.domain.Priority;
import br.com.sea.solicitations.solicitation.domain.ServiceType;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.index.MappingBuilder;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SolicitationSearchDocumentTest {

    @Test
    void mapsPartialAndDecidedSolicitationsToOneStableDocumentId() throws Exception {
        UUID id = UUID.randomUUID();
        Solicitation solicitation = new Solicitation(UUID.randomUUID());
        solicitation.completeBasicData(new BasicData(
                ServiceType.INSTALLATION, "Instalacao de equipamento",
                "Solicito instalacao de equipamento no endereco indicado."
        ));

        SolicitationSearchDocument draft = SolicitationSearchDocument.from(id, solicitation);
        assertThat(draft.id()).isEqualTo(id.toString());
        assertThat(draft.status()).isEqualTo("DRAFT");
        assertThat(draft.state()).isNull();
        assertThat(draft.priority()).isNull();

        solicitation.completeAddress(new Address(
                "01001000", "Praca da Se", "Se", "Sao Paulo", FederativeUnit.SP, "100", null
        ));
        solicitation.completeConfirmation(new Confirmation(
                Priority.HIGH, LocalDate.of(2026, 11, 4), new BigDecimal("100.00"), true
        ), LocalDate.of(2026, 11, 2));
        Instant submittedAt = Instant.parse("2026-11-02T12:00:00Z");
        UUID analystId = UUID.randomUUID();
        solicitation.submit(LocalDate.of(2026, 11, 2), submittedAt);
        solicitation.startReview(analystId, submittedAt.plusSeconds(60));
        solicitation.reject(analystId, "Documentacao insuficiente.", submittedAt.plusSeconds(120));

        SolicitationSearchDocument decided = SolicitationSearchDocument.from(id, solicitation);
        assertThat(decided.id()).isEqualTo(draft.id());
        assertThat(decided.status()).isEqualTo("REJECTED");
        assertThat(decided.serviceType()).isEqualTo("INSTALLATION");
        assertThat(decided.title()).isEqualTo("Instalacao de equipamento");
        assertThat(decided.priority()).isEqualTo("HIGH");
        assertThat(decided.state()).isEqualTo("SP");
        assertThat(decided.preferredDate()).isEqualTo(LocalDate.of(2026, 11, 4));
        assertThat(decided.submittedAt()).isEqualTo(submittedAt);
        assertThat(decided.assignedAnalystId()).isEqualTo(analystId.toString());
        assertThat(decided.decidedAt()).isEqualTo(submittedAt.plusSeconds(120));
    }

    @Test
    void declaresTextKeywordAndDateMapping() throws Exception {
        assertThat(SolicitationSearchDocument.class.getAnnotation(Document.class).indexName())
                .isEqualTo("solicitations-v1");
        assertThat(SolicitationSearchDocument.class.getDeclaredField("title")
                .getAnnotation(Field.class).type()).isEqualTo(FieldType.Text);
        assertThat(SolicitationSearchDocument.class.getDeclaredField("description")
                .getAnnotation(Field.class).analyzer()).isEqualTo("brazilian");
        assertThat(SolicitationSearchDocument.class.getDeclaredField("state")
                .getAnnotation(Field.class).type()).isEqualTo(FieldType.Keyword);
        assertThat(SolicitationSearchDocument.class.getDeclaredField("submittedAt")
                .getAnnotation(Field.class).type()).isEqualTo(FieldType.Date);

        SimpleElasticsearchMappingContext context = new SimpleElasticsearchMappingContext();
        context.setInitialEntitySet(Set.of(SolicitationSearchDocument.class));
        context.afterPropertiesSet();
        MappingElasticsearchConverter converter = new MappingElasticsearchConverter(context);
        converter.afterPropertiesSet();
        String generatedMapping = new MappingBuilder(converter)
                .buildPropertyMapping(SolicitationSearchDocument.class);
        assertThat(generatedMapping).contains("\"title\"", "\"analyzer\":\"brazilian\"",
                "\"state\"", "\"type\":\"keyword\"", "\"submittedAt\"",
                "\"type\":\"date\"", "\"sortId\"");

        SolicitationSearchDocument original = new SolicitationSearchDocument(
                UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                "SUBMITTED", "INSTALLATION", "Instalacao de equipamento",
                "Descricao da solicitacao", "MEDIUM", "SP",
                LocalDate.of(2026, 11, 4), Instant.parse("2026-11-01T10:00:00Z"),
                Instant.parse("2026-11-02T12:00:00Z"), null, null
        );
        org.springframework.data.elasticsearch.core.document.Document encoded =
                org.springframework.data.elasticsearch.core.document.Document.create();
        converter.write(original, encoded);
        SolicitationSearchDocument decoded = converter.read(SolicitationSearchDocument.class, encoded);
        assertThat(decoded.id()).isEqualTo(original.id());
        assertThat(decoded.title()).isEqualTo(original.title());
        assertThat(decoded.state()).isEqualTo("SP");
        assertThat(decoded.submittedAt()).isEqualTo(original.submittedAt());
    }
}
