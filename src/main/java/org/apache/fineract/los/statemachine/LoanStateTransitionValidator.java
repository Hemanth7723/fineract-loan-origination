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

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.apache.fineract.los.domain.enums.LoanApplicationStatus;
import org.springframework.stereotype.Component;

/**
 * Defines and validates all permitted state transitions for a {@link
 * org.apache.fineract.los.domain.LoanApplication}.
 *
 * The transition map is the single source of truth for the loan origination lifecycle. Adding a
 * new valid transition requires only a one-line change in {@link #buildTransitionMap()}.
 *
 * Valid transitions:
 * DRAFT -> SUBMITTED
 * SUBMITTED -> UNDER_REVIEW
 * UNDER_REVIEW -> APPROVED, REJECTED, REFERRED
 * REFERRED -> SUBMITTED, UNDER_REVIEW
 * APPROVED -> DISBURSED
 *
 * Terminal states (REJECTED, DISBURSED) have no outgoing transitions.
 */
@Component
public class LoanStateTransitionValidator {

  /** Immutable map of all valid state transitions. Key: current status. Value: permitted next statuses. */
  private final Map<LoanApplicationStatus, Set<LoanApplicationStatus>> transitionMap;

  /** Constructs the validator and initialises the transition map on startup. */
  public LoanStateTransitionValidator() {
    this.transitionMap = buildTransitionMap();
  }

  /**
   * Returns whether a transition from {@code from} to {@code to} is permitted.
   *
   * @param from current status of the application
   * @param to target status being requested
   * @return true if the transition is valid
   */
  public boolean isValid(final LoanApplicationStatus from, final LoanApplicationStatus to) {
    final Set<LoanApplicationStatus> permitted = transitionMap.get(from);
    return permitted != null && permitted.contains(to);
  }

  /**
   * Returns the set of statuses the application can legally transition to from the given status.
   *
   * @param from current status
   * @return permitted target statuses — never null, empty for terminal states
   */
  public Set<LoanApplicationStatus> permittedTransitions(final LoanApplicationStatus from) {
    return transitionMap.getOrDefault(from, EnumSet.noneOf(LoanApplicationStatus.class));
  }

  private Map<LoanApplicationStatus, Set<LoanApplicationStatus>> buildTransitionMap() {

    final Map<LoanApplicationStatus, Set<LoanApplicationStatus>> map =
        new EnumMap<>(LoanApplicationStatus.class);

    map.put(LoanApplicationStatus.DRAFT, EnumSet.of(LoanApplicationStatus.SUBMITTED));

    map.put(LoanApplicationStatus.SUBMITTED, EnumSet.of(LoanApplicationStatus.UNDER_REVIEW));

    map.put(
        LoanApplicationStatus.UNDER_REVIEW,
        EnumSet.of(
            LoanApplicationStatus.APPROVED,
            LoanApplicationStatus.REJECTED,
            LoanApplicationStatus.REFERRED));

    map.put(
        LoanApplicationStatus.REFERRED,
        EnumSet.of(LoanApplicationStatus.SUBMITTED, LoanApplicationStatus.UNDER_REVIEW));

    map.put(LoanApplicationStatus.APPROVED, EnumSet.of(LoanApplicationStatus.DISBURSED));

    return map;
  }
}
