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

package org.apache.fineract.los.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Rate limiting filter using the token bucket algorithm to prevent brute-force attacks and API abuse.
 *
 * Auth endpoints (/api/v1/auth/**): 100 requests/minute per IP. Admin endpoints
 * (/api/v1/admin/**): 20 requests/minute per IP. All other endpoints: 100 requests/minute per IP.
 * Buckets are tracked per client IP and expire after 15 minutes of inactivity.
 * Requests exceeding the limit receive HTTP 429 with a Retry-After header.
 */
@Slf4j
@Component
public class RateLimitFilter implements Filter {

  private static final int AUTH_MAX_REQUESTS = 100;
  private static final Duration AUTH_WINDOW = Duration.ofMinutes(1);

  private static final int ADMIN_MAX_REQUESTS = 20;
  private static final Duration ADMIN_WINDOW = Duration.ofMinutes(1);

  private static final int GENERAL_MAX_REQUESTS = 100;
  private static final Duration GENERAL_WINDOW = Duration.ofMinutes(1);

  private final Cache<String, Bucket> authBuckets =
      Caffeine.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).maximumSize(10000).build();

  private final Cache<String, Bucket> adminBuckets =
      Caffeine.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).maximumSize(10000).build();

  private final Cache<String, Bucket> generalBuckets =
      Caffeine.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).maximumSize(10000).build();

  @Override
  public void doFilter(
      final ServletRequest request, final ServletResponse response, final FilterChain chain)
      throws IOException, ServletException {

    final HttpServletRequest httpRequest = (HttpServletRequest) request;
    final HttpServletResponse httpResponse = (HttpServletResponse) response;

    final String clientIp = getClientIp(httpRequest);
    final String requestUri = httpRequest.getRequestURI();

    Bucket bucket;
    long retryAfterSeconds;

    if (requestUri.startsWith("/api/v1/auth/")) {
      bucket = authBuckets.get(clientIp, k -> createAuthBucket());
      retryAfterSeconds = AUTH_WINDOW.toSeconds();
    } else if (requestUri.startsWith("/api/v1/admin/")) {
      bucket = adminBuckets.get(clientIp, k -> createAdminBucket());
      retryAfterSeconds = ADMIN_WINDOW.toSeconds();
    } else {
      bucket = generalBuckets.get(clientIp, k -> createGeneralBucket());
      retryAfterSeconds = GENERAL_WINDOW.toSeconds();
    }

    if (bucket.tryConsume(1)) {
      chain.doFilter(request, response);
    } else {
      log.warn("Rate limit exceeded for IP [{}] on endpoint [{}]", clientIp, requestUri);

      httpResponse.setStatus(429);
      httpResponse.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
      httpResponse.setContentType("application/json");
      httpResponse
          .getWriter()
          .write(
              "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\",\"retryAfterSeconds\":"
                  + retryAfterSeconds
                  + "}");
    }
  }

  /** Creates a bucket for authentication endpoints — 100 requests per minute. */
  private Bucket createAuthBucket() {
    return Bucket.builder()
        .addLimit(
            Bandwidth.classic(AUTH_MAX_REQUESTS, Refill.greedy(AUTH_MAX_REQUESTS, AUTH_WINDOW)))
        .build();
  }

  /** Creates a bucket for admin endpoints — 20 requests per minute. */
  private Bucket createAdminBucket() {
    return Bucket.builder()
        .addLimit(
            Bandwidth.classic(ADMIN_MAX_REQUESTS, Refill.greedy(ADMIN_MAX_REQUESTS, ADMIN_WINDOW)))
        .build();
  }

  /** Creates a bucket for general endpoints — 100 requests per minute. */
  private Bucket createGeneralBucket() {
    return Bucket.builder()
        .addLimit(
            Bandwidth.classic(
                GENERAL_MAX_REQUESTS, Refill.greedy(GENERAL_MAX_REQUESTS, GENERAL_WINDOW)))
        .build();
  }

  /**
   * Extracts the client IP, checking X-Forwarded-For first for requests behind proxies,
   * then falling back to the remote address.
   */
  private String getClientIp(final HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip != null && !ip.isEmpty()) {
      ip = ip.split(",")[0].trim();
    } else {
      ip = request.getRemoteAddr();
    }
    return ip;
  }
}
