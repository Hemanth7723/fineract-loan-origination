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

package org.apache.fineract.los.statemachine;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.los.domain.LoanApplication;
import org.apache.fineract.los.domain.enums.LoanApplicationStatus;
import org.apache.fineract.los.exception.LosErrorConstants;
import org.springframework.stereotype.Component;

/**
 * Core state machine governing the lifecycle of a {@link LoanApplication}.
 *
 * This is the only component permitted to call {@link LoanApplication#setStatus}. All other layers
 * must go through this class to change application status — direct status mutation is a design
 * violation.
 *
 * This class is stateless — all state lives in the {@link LoanApplication} entity, which is
 * protected by optimistic locking ({@code @Version}). New transitions are added to
 * {@link LoanStateTransitionValidator} only; this class never changes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoanOriginationStateMachine {

  /** Validator holding the complete transition map. */
  private final LoanStateTransitionValidator validator;

  /**
   * Validates and applies a status transition to the given loan application.
   *
   * The application entity is mutated in-place. The caller is responsible for persisting the
   * entity after this method returns successfully. This method is intentionally not transactional
   * — transaction boundaries are managed by the service layer.
   *
   * @param application the loan application to transition
   * @param targetStatus the desired target status
   * @throws IllegalArgumentException if application or targetStatus is null
   * @throws IllegalStateException if the application is in a terminal state
   * @throws LoanStateTransitionException if the transition is not permitted
   */
  public void transition(
      final LoanApplication application, final LoanApplicationStatus targetStatus) {

    validateInputs(application, targetStatus);

    final LoanApplicationStatus currentStatus = application.getStatus();

    guardAgainstTerminalState(application, currentStatus);

    if (!validator.isValid(currentStatus, targetStatus)) {
      log.warn(
          "Rejected invalid transition: applicationRef={} tenantId={} from={} to={}",
          application.getApplicationRef(),
          application.getTenantId(),
          currentStatus,
          targetStatus);
      throw new LoanStateTransitionException(currentStatus, targetStatus);
    }

    application.setStatus(targetStatus);

    log.info(
        "State transition applied: applicationRef={} tenantId={} from={} to={}",
        application.getApplicationRef(),
        application.getTenantId(),
        currentStatus,
        targetStatus);
  }

  /**
   * Returns all statuses the application can legally transition to from its current status.
   *
   * @param application the loan application to query
   * @return set of permitted next statuses — empty if the application is in a terminal state
   * @throws IllegalArgumentException if application is null
   */
  public Set<LoanApplicationStatus> permittedTransitions(final LoanApplication application) {
    if (application == null) {
      throw new IllegalArgumentException(LosErrorConstants.MSG_APPLICATION_NULL);
    }
    return validator.permittedTransitions(application.getStatus());
  }

  /**
   * Returns whether the given application is in a terminal state.
   *
   * @param application the loan application to check
   * @return true if the application is REJECTED or DISBURSED
   * @throws IllegalArgumentException if application is null
   */
  public boolean isTerminal(final LoanApplication application) {
    if (application == null) {
      throw new IllegalArgumentException(LosErrorConstants.MSG_APPLICATION_NULL);
    }
    return application.getStatus().isTerminal();
  }

  private void validateInputs(
      final LoanApplication application, final LoanApplicationStatus targetStatus) {

    if (application == null) {
      throw new IllegalArgumentException(LosErrorConstants.MSG_APPLICATION_NULL);
    }

    if (targetStatus == null) {
      throw new IllegalArgumentException(LosErrorConstants.MSG_TARGET_STATUS_NULL);
    }

    if (application.getStatus() == null) {
      throw new IllegalStateException(LosErrorConstants.MSG_STATUS_UNINITIALISED);
    }
  }

  private void guardAgainstTerminalState(
      final LoanApplication application, final LoanApplicationStatus currentStatus) {

    if (currentStatus.isTerminal()) {
      log.error(
          "Transition attempted on terminal application: applicationRef={} tenantId={} status={}",
          application.getApplicationRef(),
          application.getTenantId(),
          currentStatus);

      throw new IllegalStateException(
          String.format(
              LosErrorConstants.MSG_TERMINAL_STATE_TEMPLATE,
              application.getApplicationRef(),
              currentStatus));
    }
  }
}
