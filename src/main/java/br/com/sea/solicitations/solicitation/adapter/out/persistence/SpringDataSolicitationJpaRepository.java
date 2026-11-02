package br.com.sea.solicitations.solicitation.adapter.out.persistence;

import br.com.sea.solicitations.solicitation.domain.Solicitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataSolicitationJpaRepository extends JpaRepository<Solicitation, UUID> {
}
