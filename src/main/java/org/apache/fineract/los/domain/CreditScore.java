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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.los.domain.enums.RiskCategory;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Stores the computed credit score and individual factor contributions for a loan application.
 *
 * One-to-one with {@code LoanApplication}. Computed automatically when the application enters
 * UNDER_REVIEW status. Immutable after creation. Individual factor scores are stored to support
 * score explainability, as required for audit and regulatory purposes.
 */
@Entity
@Table(name = "credit_score")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class CreditScore {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false, updatable = false)
  private Long id;

  /** The loan application this score belongs to. Loaded lazily. */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "application_id", nullable = false, updatable = false)
  private LoanApplication application;

  /** Composite credit score, 0-100. Weighted sum of all factor scores. Higher is better. */
  @Column(name = "score", nullable = false)
  private Integer score;

  /** Risk classification derived from the composite score: LOW (70-100), MEDIUM (40-69), HIGH (0-39). */
  @Enumerated(EnumType.STRING)
  @Column(name = "risk_category", nullable = false, length = 20)
  private RiskCategory riskCategory;

  /** Score from income-to-loan ratio factor. Max 30 points. */
  @Column(name = "income_ratio_score")
  private Integer incomeRatioScore;

  /** Score from existing debt burden factor. Max 25 points. */
  @Column(name = "debt_burden_score")
  private Integer debtBurdenScore;

  /** Score from employment stability factor. Max 20 points. */
  @Column(name = "employment_score")
  private Integer employmentScore;

  /** Score from repayment history factor. Max 15 points. */
  @Column(name = "repayment_history_score")
  private Integer repaymentHistoryScore;

  /** Score from loan purpose risk factor. Max 10 points. */
  @Column(name = "loan_purpose_score")
  private Integer loanPurposeScore;

  /** Timestamp when scoring was computed. Immutable after creation. */
  @Column(name = "scored_at", nullable = false, updatable = false)
  private LocalDateTime scoredAt;

  /** Tenant identifier — must match the parent LoanApplication. */
  @Column(name = "tenant_id", nullable = false, length = 100, updatable = false)
  private String tenantId;
}
