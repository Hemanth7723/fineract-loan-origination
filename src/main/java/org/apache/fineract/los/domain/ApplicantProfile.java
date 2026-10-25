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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Stores personal and financial information about the loan applicant.
 *
 * One-to-one with {@code LoanApplication}. Financial fields feed the credit scoring engine:
 * monthly income (income-ratio, 30%), existing obligations (debt-burden, 25%), and employment
 * status and duration (employment-stability, 20%).
 */
@Entity
@Table(name = "applicant_profile")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ApplicantProfile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false, updatable = false)
  private Long id;

  /** The loan application this profile belongs to. Loaded lazily. */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "application_id", nullable = false, updatable = false)
  private LoanApplication application;

  /** Full legal name of the applicant. */
  @Column(name = "full_name", nullable = false, length = 200)
  private String fullName;

  /** National identity document number. Optional where group guarantees are used instead. */
  @Column(name = "national_id", length = 100)
  private String nationalId;

  /** Monthly income in the application currency. Primary input for income-to-loan ratio scoring (30% weight). */
  @Column(name = "monthly_income", precision = 19, scale = 2)
  private BigDecimal monthlyIncome;

  /** Employment status. Expected values: EMPLOYED, SELF_EMPLOYED, UNEMPLOYED, INFORMAL. Used for employment stability scoring (20% weight). */
  @Column(name = "employment_status", length = 50)
  private String employmentStatus;

  /** Duration in current employment, in months. Used for employment stability scoring (20% weight). */
  @Column(name = "employment_duration_months")
  private Integer employmentDurationMonths;

  /** Total monthly obligations across all active loans. Used for debt burden scoring (25% weight). */
  @Column(name = "existing_loan_obligations", precision = 19, scale = 2)
  private BigDecimal existingLoanObligations = BigDecimal.ZERO;

  /** Applicant's Fineract {@code clientId}. Required before disbursement — Fineract needs a pre-existing client record to attach the loan to. */
  @Column(name = "fineract_client_id")
  private Long fineractClientId;

  /** Tenant identifier — must match the parent LoanApplication. */
  @Column(name = "tenant_id", nullable = false, length = 100, updatable = false)
  private String tenantId;

  /** Timestamp of profile creation. Set automatically by Spring Data Auditing. */
  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
