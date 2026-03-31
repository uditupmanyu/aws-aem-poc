/*
 * MailCredentialsUpdaterService.java
 * Service interface for pushing new credentials to Day CQ Mail Service.
 */
package com.poc.aem.core.services;

import com.poc.aem.core.models.MailCredentials;

/**
 * Service contract for applying SMTP credentials to the Day CQ Mail
 * Service ({@code com.day.cq.mailer.DefaultMailService}) at runtime.
 *
 * <p>Updates are applied via the OSGi {@link org.osgi.service.cm.ConfigurationAdmin}
 * so the mail service picks up new values immediately without any
 * AEM restart or bundle refresh.</p>
 *
 * <p>The operation is designed to be idempotent: applying the same
 * credentials twice produces no observable side-effect.</p>
 */
public interface MailCredentialsUpdaterService {

  /**
   * Applies the supplied credentials to the Day CQ Mail Service OSGi
   * configuration.
   *
   * <p>Only the following properties are overwritten; all other
   * properties in the existing configuration are preserved:
   * <ul>
   *   <li>{@code smtp.host}</li>
   *   <li>{@code smtp.port}</li>
   *   <li>{@code smtp.user}</li>
   *   <li>{@code smtp.password}</li>
   *   <li>{@code smtp.ssl}</li>
   *   <li>{@code from.address} (only when non-empty)</li>
   * </ul>
   * </p>
   *
   * @param credentials the credentials to apply; must not be {@code null}
   * @throws IllegalArgumentException if {@code credentials} is {@code null}
   * @throws RuntimeException         if the OSGi configuration update fails
   */
  void updateMailServiceCredentials(MailCredentials credentials);
}
