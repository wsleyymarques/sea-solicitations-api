package br.com.sea.solicitations.audit.adapter.in.aop;

import br.com.sea.solicitations.audit.domain.AuditAction;
import br.com.sea.solicitations.audit.domain.AuditLog;
import br.com.sea.solicitations.audit.domain.AuditOutcome;
import br.com.sea.solicitations.audit.domain.AuditResourceType;
import br.com.sea.solicitations.solicitation.application.port.in.CompleteSolicitationAddressCommand;
import br.com.sea.solicitations.solicitation.application.port.in.CompleteSolicitationConfirmationCommand;
import br.com.sea.solicitations.solicitation.application.port.in.CreateSolicitationCommand;
import br.com.sea.solicitations.solicitation.application.port.in.CreateSolicitationResult;
import br.com.sea.solicitations.solicitation.application.port.in.DecideSolicitationCommand;
import br.com.sea.solicitations.solicitation.application.port.in.DecisionOutcome;
import br.com.sea.solicitations.solicitation.application.port.in.StartSolicitationReviewCommand;
import br.com.sea.solicitations.solicitation.application.port.in.SubmitSolicitationCommand;
import br.com.sea.solicitations.user.application.port.in.CreateInternalUserCommand;
import br.com.sea.solicitations.user.application.port.in.CreateInternalUserResult;
import br.com.sea.solicitations.user.application.port.in.RegisterClientResult;
import br.com.sea.solicitations.user.domain.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Component
public class AuditLogFactory {

    private final Clock clock;

    public AuditLogFactory(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "Clock is required");
    }

    public List<AuditLog> create(AuditAction declaredAction, Object command, Object result, Throwable failure) {
        AuditAction action = actionFor(declaredAction, command);
        UUID actorId = authenticatedActorId();
        if (actorId == null) {
            actorId = commandActorId(command, result);
        }
        UUID resourceId = resourceId(command, result);
        String requestId = requestId();
        AuditOutcome outcome = failure == null ? AuditOutcome.SUCCESS : AuditOutcome.FAILURE;
        Map<String, Object> metadata = metadataFor(command, failure);
        AuditResourceType type = action == AuditAction.USER_CREATED
                || action == AuditAction.ANALYST_STATES_ASSIGNED
                ? AuditResourceType.USER : AuditResourceType.SOLICITATION;

        List<AuditLog> logs = new ArrayList<>();
        logs.add(new AuditLog(actorId, action, type, resourceId, outcome,
                requestId, metadata, clock.instant()));
        if (failure == null && result instanceof CreateSolicitationResult created) {
            logs.add(new AuditLog(actorId, AuditAction.BASIC_DATA_SAVED,
                    AuditResourceType.SOLICITATION, created.id(), AuditOutcome.SUCCESS,
                    requestId, metadata, clock.instant()));
        }
        if (failure == null && result instanceof CreateInternalUserResult created
                && created.role() == UserRole.ANALYST) {
            logs.add(new AuditLog(actorId, AuditAction.ANALYST_STATES_ASSIGNED,
                    AuditResourceType.USER, created.id(), AuditOutcome.SUCCESS,
                    requestId, Map.of("coveredStates", sortedStates(created.coveredStates())), clock.instant()));
        }
        return List.copyOf(logs);
    }

    private static AuditAction actionFor(AuditAction declared, Object command) {
        if (declared == AuditAction.SOLICITATION_APPROVED
                && command instanceof DecideSolicitationCommand decision
                && decision.outcome() == DecisionOutcome.REJECTED) {
            return AuditAction.SOLICITATION_REJECTED;
        }
        return declared;
    }

    private static UUID authenticatedActorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt && authentication.isAuthenticated()) {
            try {
                String subject = jwt.getToken().getSubject();
                return subject == null ? null : UUID.fromString(subject);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private static UUID commandActorId(Object command, Object result) {
        if (command instanceof CreateSolicitationCommand created) return created.clientId();
        if (command instanceof CompleteSolicitationAddressCommand address) return address.clientId();
        if (command instanceof CompleteSolicitationConfirmationCommand confirmation) return confirmation.clientId();
        if (command instanceof SubmitSolicitationCommand submitted) return submitted.clientId();
        if (command instanceof StartSolicitationReviewCommand review) return review.analystId();
        if (command instanceof DecideSolicitationCommand decision) return decision.analystId();
        if (result instanceof RegisterClientResult registered) return registered.id();
        return null;
    }

    private static UUID resourceId(Object command, Object result) {
        if (result instanceof RegisterClientResult registered) return registered.id();
        if (result instanceof CreateInternalUserResult created) return created.id();
        if (result instanceof CreateSolicitationResult created) return created.id();
        if (command instanceof CompleteSolicitationAddressCommand address) return address.solicitationId();
        if (command instanceof CompleteSolicitationConfirmationCommand confirmation) return confirmation.solicitationId();
        if (command instanceof SubmitSolicitationCommand submitted) return submitted.solicitationId();
        if (command instanceof StartSolicitationReviewCommand review) return review.solicitationId();
        if (command instanceof DecideSolicitationCommand decision) return decision.solicitationId();
        return null;
    }

    private static Map<String, Object> metadataFor(Object command, Throwable failure) {
        Map<String, Object> metadata = new HashMap<>();
        if (command instanceof CreateInternalUserCommand user) {
            metadata.put("role", user.role().name());
        } else if (command instanceof CreateSolicitationCommand solicitation) {
            metadata.put("serviceType", solicitation.serviceType().name());
        } else if (command instanceof CompleteSolicitationConfirmationCommand confirmation) {
            metadata.put("priority", confirmation.priority().name());
        } else if (command instanceof DecideSolicitationCommand decision) {
            metadata.put("outcome", decision.outcome().name());
        }
        if (failure != null) {
            metadata.put("errorType", failure.getClass().getSimpleName());
        }
        return metadata;
    }

    private static List<String> sortedStates(java.util.Set<? extends Enum<?>> states) {
        return states.stream().map(Enum::name).sorted().toList();
    }

    private static String requestId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String supplied = request.getHeader("X-Request-Id");
            if (supplied != null && supplied.matches("[A-Za-z0-9._:-]{1,100}")) {
                return supplied;
            }
        }
        return UUID.randomUUID().toString();
    }
}
