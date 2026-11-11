package io.correcta.assessment.messaging

import org.springframework.data.jpa.repository.JpaRepository

/** Writes [OutboxMessage] rows through JPA, inside the caller's transaction. [OutboxRelay] reads them with plain JDBC. */
interface OutboxMessageRepository : JpaRepository<OutboxMessage, String>