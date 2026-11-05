package io.correcta.assessment.messaging

import org.springframework.data.jpa.repository.JpaRepository

/* Writes go through JPA, inside the caller's transaction; OutboxRelay reads with plain JDBC. */
interface OutboxMessageRepository : JpaRepository<OutboxMessage, String>
