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

package org.apache.fineract.los.scoring;

import org.apache.fineract.los.scoring.model.ApplicantScoringProfile;
import org.apache.fineract.los.scoring.model.FactorScore;

/**
 * Strategy interface for a single credit scoring factor.
 *
 * Each implementation evaluates one dimension of creditworthiness and returns a bounded
 * {@link FactorScore}. Implementations must be pure (no side effects), bounded (score never
 * exceeds {@code maxPoints()}), and stateless (safe for concurrent singleton use).
 *
 * New factors can be added by implementing this interface and annotating with {@code @Component}
 * — {@link DefaultCreditScoringStrategy} discovers all factors via constructor injection.
 */
public interface ScoringFactor {

  /**
   * Evaluates this factor against the given applicant profile.
   *
   * @param profile the applicant data to evaluate
   * @return a factor score with points and explanation, never null
   */
  FactorScore score(ApplicantScoringProfile profile);

  /**
   * Returns the maximum points this factor can contribute, sourced from the configured weight.
   *
   * @return maximum points, equal to the configured weight
   */
  int maxPoints();

  /**
   * Returns a unique, stable identifier for this factor. Used as the key in
   * {@link org.apache.fineract.los.scoring.model.CreditScoreResult#getFactorScores()}.
   *
   * @return factor identifier, e.g. "income-ratio"
   */
  String factorName();
}
