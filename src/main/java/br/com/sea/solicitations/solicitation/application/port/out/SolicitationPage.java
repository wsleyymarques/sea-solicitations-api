package br.com.sea.solicitations.solicitation.application.port.out;

import br.com.sea.solicitations.solicitation.domain.Solicitation;

import java.util.List;
import java.util.Objects;

public record SolicitationPage(
        List<Solicitation> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public SolicitationPage {
        content = List.copyOf(Objects.requireNonNull(content, "Content is required"));
    }
}
