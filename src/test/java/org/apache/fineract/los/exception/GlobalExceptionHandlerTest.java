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

package org.apache.fineract.los.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.fineract.los.domain.enums.LoanApplicationStatus;
import org.apache.fineract.los.statemachine.LoanStateTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Unit tests for {@link GlobalExceptionHandler}. */
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  @DisplayName("LoanStateTransitionException maps to 409 Conflict with the transition message")
  void invalidStateTransitionReturnsConflict() {
    final LoanStateTransitionException ex =
        new LoanStateTransitionException(
            LoanApplicationStatus.APPROVED, LoanApplicationStatus.SUBMITTED);

    final ResponseEntity<ErrorResponse> response = handler.handleInvalidStateTransition(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(response.getBody().getError()).isEqualTo(HttpStatus.CONFLICT.getReasonPhrase());
    assertThat(response.getBody().getMessage())
        .isEqualTo(ex.getMessage())
        .contains("APPROVED", "SUBMITTED");
  }
}
