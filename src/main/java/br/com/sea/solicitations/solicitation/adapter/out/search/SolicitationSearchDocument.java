package br.com.sea.solicitations.solicitation.adapter.out.search;

import br.com.sea.solicitations.solicitation.domain.Address;
import br.com.sea.solicitations.solicitation.domain.BasicData;
import br.com.sea.solicitations.solicitation.domain.Confirmation;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Document(indexName = "solicitations-v1", createIndex = false)
public record SolicitationSearchDocument(
        @Id String id,
        @Field(type = FieldType.Keyword) String sortId,
        @Field(type = FieldType.Keyword) String status,
        @Field(type = FieldType.Keyword) String serviceType,
        @Field(type = FieldType.Text, analyzer = "brazilian") String title,
        @Field(type = FieldType.Text, analyzer = "brazilian") String description,
        @Field(type = FieldType.Keyword) String priority,
        @Field(type = FieldType.Keyword) String state,
        @Field(type = FieldType.Date) LocalDate preferredDate,
        @Field(type = FieldType.Date) Instant createdAt,
        @Field(type = FieldType.Date) Instant submittedAt,
        @Field(type = FieldType.Keyword) String assignedAnalystId,
        @Field(type = FieldType.Date) Instant decidedAt
) {
    public static SolicitationSearchDocument from(UUID id, Solicitation solicitation) {
        Objects.requireNonNull(id, "Solicitation ID is required");
        Objects.requireNonNull(solicitation, "Solicitation is required");
        BasicData basic = solicitation.getBasicData();
        Address address = solicitation.getAddress();
        Confirmation confirmation = solicitation.getConfirmation();
        return new SolicitationSearchDocument(
                id.toString(),
                id.toString(),
                solicitation.getStatus().name(),
                basic == null ? null : basic.getServiceType().name(),
                basic == null ? null : basic.getTitle(),
                basic == null ? null : basic.getDescription(),
                confirmation == null ? null : confirmation.getPriority().name(),
                address == null ? null : address.getState().name(),
                confirmation == null ? null : confirmation.getPreferredDate(),
                solicitation.getCreatedAt(),
                solicitation.getSubmittedAt(),
                solicitation.getAssignedAnalystId() == null
                        ? null : solicitation.getAssignedAnalystId().toString(),
                solicitation.getDecision() == null
                        ? null : solicitation.getDecision().getDecidedAt()
        );
    }
}
