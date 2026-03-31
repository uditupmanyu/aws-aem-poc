/*
 * CredentialsPropertiesReaderService.java
 * Service interface for reading credentials from credentials.properties.
 */
package com.poc.aem.core.services;

import com.poc.aem.core.models.MailCredentials;

/**
 * Service contract for reading SMTP credentials from a
 * {@code credentials.properties} file on the AEM server's file system.
 *
 * <p>This service follows exactly the same contract as
 * {@link AwsSecretsReaderService} – both return a {@link MailCredentials}
 * instance – making them interchangeable in
 * {@link com.poc.aem.core.schedulers.MailCredentialsScheduler}.</p>
 *
 * <p>The file path is configurable via
 * {@link com.poc.aem.core.configs.AwsSecretsConfiguration#credentialsPropertiesPath()}.
 * The file is opened fresh on every call, so runtime changes are always
 * reflected without restarting AEM.</p>
 */
public interface CredentialsPropertiesReaderService {

  /**
   * Reads SMTP credentials from the configured {@code credentials.properties}
   * file on the file system.
   *
   * <p>The file is opened and parsed on every invocation so that any
   * in-place update to the file is picked up immediately.</p>
   *
   * @return a fully populated {@link MailCredentials}; never {@code null}
   * @throws RuntimeException if the file is missing, unreadable, or
   *                          required properties are absent
   */
  MailCredentials readCredentials();
}
