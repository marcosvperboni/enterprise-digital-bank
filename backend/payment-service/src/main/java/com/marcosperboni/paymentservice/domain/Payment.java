package com.marcosperboni.paymentservice.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Table("payments")
public class Payment implements Persistable<UUID> {

	@Id
	private UUID id;

	private UUID transactionId;

	private BigDecimal amount;

	private PaymentStatus status;

	private Instant processedAt;

	private Instant createdAt;

	// ponytail: manually-assigned UUID id defeats R2DBC's default isNew() check (non-null id looks
	// "existing"), which turns save() on a brand-new row into a silent no-op UPDATE. This flag,
	// set only via newPayment(), is the standard Persistable fix.
	@Transient
	private boolean isNewEntity = false;

	public static Payment newPayment() {
		Payment payment = new Payment();
		payment.isNewEntity = true;
		return payment;
	}

	@Override
	public boolean isNew() {
		return isNewEntity;
	}

	public void markPersisted() {
		this.isNewEntity = false;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(UUID transactionId) {
		this.transactionId = transactionId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public PaymentStatus getStatus() {
		return status;
	}

	public void setStatus(PaymentStatus status) {
		this.status = status;
	}

	public Instant getProcessedAt() {
		return processedAt;
	}

	public void setProcessedAt(Instant processedAt) {
		this.processedAt = processedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
