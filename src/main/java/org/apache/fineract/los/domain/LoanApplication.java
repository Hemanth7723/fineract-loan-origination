/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.fineract.los.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.los.domain.enums.FineractIntegrationStatus;
import org.apache.fineract.los.domain.enums.LoanApplicationStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Root aggregate of the Loan Origination System.
 *
 * Represents a loan application from submission through disbursement. All lifecycle transitions are
 * enforced exclusively by {@code LoanOriginationStateMachine} — no external code may call
 * {@code setStatus} directly.
 *
 * Every query must filter by {@code tenantId}, extracted from the
 * {@code X-Fineract-Platform-TenantId} request header. {@code version} provides optimistic locking
 * to prevent silent data corruption on concurrent updates.
 */
@Entity
@Table(name = "loan_application")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class LoanApplication {

  /** Internal surrogate key. Never exposed in API responses — use {@code applicationRef}. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false, updatable = false)
  private Long id;

  /** Human-readable reference exposed to clients. Format: LOS-{YEAR}-{SEQUENCE}, e.g. LOS-2026-00101. */
  @Column(name = "application_ref", nullable = false, unique = true, length = 50, updatable = false)
  private String applicationRef;

  /** Current lifecycle state. Transitions enforced exclusively by {@code LoanOriginationStateMachine}. */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private LoanApplicationStatus status;

  /** Amount requested by the applicant. Precision 19, scale 2 — matches Fineract monetary convention. */
  @Column(name = "requested_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal requestedAmount;

  /** ISO 4217 currency code, e.g. USD, INR, KES. Defaults to USD. */
  @Column(name = "currency", nullable = false, length = 10)
  private String currency = "USD";

  /** Purpose of the loan, e.g. AGRICULTURE, EDUCATION, BUSINESS. Used as a factor in credit scoring (10% weight). */
  @Column(name = "loan_purpose", length = 100)
  private String loanPurpose;

  /** Requested repayment period in months. Used by the disbursement bridge when constructing the Fineract POST /loans payload. */
  @Column(name = "tenor_months")
  private Integer tenorMonths;

  /** Fineract loan product identifier. Required before the disbursement bridge can run. */
  @Column(name = "fineract_loan_product_id")
  private Long fineractLoanProductId;

  /** Fineract {@code loanId} assigned after successful loan creation. Null until the disbursement bridge completes step 1. */
  @Column(name = "fineract_loan_id")
  private Long fineractLoanId;

  /** Fineract {@code clientId} of the applicant. Links this application to the Fineract client record. */
  @Column(name = "fineract_client_id")
  private Long fineractClientId;

  /** Tracks Fineract integration progress to support idempotent disbursement retries. */
  @Enumerated(EnumType.STRING)
  @Column(name = "fineract_integration_status", length = 30)
  private FineractIntegrationStatus fineractIntegrationStatus;

  /** Tenant identifier. Every repository query must filter by this field. */
  @Column(name = "tenant_id", nullable = false, length = 100, updatable = false)
  private String tenantId;

  /** Optimistic locking version counter, incremented by JPA on every update. */
  @Version
  @Column(name = "version")
  private Long version;

  /** Timestamp of initial creation. Set automatically by Spring Data Auditing. */
  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /** Timestamp of last modification. Updated automatically by Spring Data Auditing. */
  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
