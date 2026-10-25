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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.apache.fineract.los.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.los.domain.enums.ApprovalDecision;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Records a single approval decision at one stage of the multi-stage approval workflow.
 *
 * Many-to-one with {@code LoanApplication}. One application accumulates multiple records as it
 * progresses through the workflow, forming a full audit trail. This entity is immutable after
 * creation — a new record is created if a stage needs to be reconsidered.
 */
@Entity
@Table(name = "approval_stage")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ApprovalStage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false, updatable = false)
  private Long id;

  /** The loan application this stage belongs to. Loaded lazily. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "application_id", nullable = false, updatable = false)
  private LoanApplication application;

  /** Name of this approval stage. Standard values: LOAN_OFFICER, BRANCH_MANAGER, CREDIT_COMMITTEE. Configurable per institution. */
  @Column(name = "stage_name", nullable = false, length = 100, updatable = false)
  private String stageName;

  /** Username of the officer who recorded this decision, extracted from their JWT claims. */
  @Column(name = "assigned_officer", length = 200, updatable = false)
  private String assignedOfficer;

  /** Decision made at this stage: APPROVE advances the workflow, REJECT terminates it, REFER returns the application to the applicant. */
  @Enumerated(EnumType.STRING)
  @Column(name = "decision", length = 30)
  private ApprovalDecision decision;

  /** Officer comments explaining the decision. Required for REJECT and REFER decisions; enforced at the service layer. */
  @Column(name = "comments", columnDefinition = "TEXT")
  private String comments;

  /** Timestamp when the officer made the decision. Null until a decision is recorded. */
  @Column(name = "decided_at", updatable = false)
  private LocalDateTime decidedAt;

  /** Tenant identifier — must match the parent LoanApplication. */
  @Column(name = "tenant_id", nullable = false, length = 100, updatable = false)
  private String tenantId;

  /** Timestamp of record creation. Set automatically by Spring Data Auditing. */
  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
