/*
 * AwsSecretsReaderService.java
 * Service interface for reading credentials from AWS Secrets Manager.
 */
package com.poc.aem.core.services;

import com.poc.aem.core.models.MailCredentials;

/**
 * Service contract for reading SMTP credentials from AWS Secrets Manager
 * in real time.
 *
 * <p>Every invocation of {@link #readCredentials()} fetches the secret
 * directly from AWS – no local caching – ensuring that password rotations
 * in Secrets Manager are reflected on the very next call.</p>
 *
 * <p>Implementations are expected to apply retry logic with exponential
 * back-off as configured via
 * {@link com.poc.aem.core.configs.AwsSecretsConfiguration}.</p>
 *
 * <p>Register as an OSGi service so that
 * {@link com.poc.aem.core.schedulers.MailCredentialsScheduler}
 * can consume it via {@code @Reference}.</p>
 */
public interface AwsSecretsReaderService {

  /**
   * Reads the current SMTP credentials from AWS Secrets Manager.
   *
   * <p>The implementation retries up to
   * {@code AwsSecretsConfiguration#retryAttempts()} times before
   * throwing. The delay between attempts starts at
   * {@code AwsSecretsConfiguration#retryDelayMs()} and doubles
   * on each subsequent attempt.</p>
   *
   * @return a fully populated {@link MailCredentials}; never {@code null}
   * @throws RuntimeException if the secret cannot be retrieved after
   *                          all configured retry attempts are exhausted
   */
  MailCredentials readCredentials();
}
