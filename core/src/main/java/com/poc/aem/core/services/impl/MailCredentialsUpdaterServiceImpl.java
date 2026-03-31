package com.poc.aem.core.services.impl;

import com.poc.aem.core.models.MailCredentials;
import com.poc.aem.core.services.MailCredentialsUpdaterService;
import java.util.Objects;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link MailCredentialsUpdaterService}.
 *
 * <p>Retrieves the OSGi {@link Configuration} for PID
 * {@value #DAY_CQ_MAILER_PID} and overlays only the credential properties, leaving every other key in the existing
 * configuration (e.g. {@code debug.email}, {@code from.name}) untouched.</p>
 */
@Component(service = MailCredentialsUpdaterService.class, immediate = true)
public class MailCredentialsUpdaterServiceImpl implements MailCredentialsUpdaterService {

  /**
   * PID of the Day CQ Mail Service.
   */
  static final String DAY_CQ_MAILER_PID = "com.day.cq.mailer.DefaultMailService";
  private static final String SMTP_HOST = "smtp.host";
  private static final String SMTP_PORT = "smtp.port";
  private static final String SMTP_USER = "smtp.user";
  private static final String SMTP_PASSWORD = "smtp.password";
  private static final String SMTP_SSL = "smtp.ssl";
  private static final String FROM_ADDRESS = "from.address";

  private static final Logger LOG = LoggerFactory.getLogger(MailCredentialsUpdaterServiceImpl.class);

  @Reference
  private ConfigurationAdmin configurationAdmin;

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateMailServiceCredentials(MailCredentials credentials) {
    if (Objects.isNull(credentials)) {
      LOG.error("MailCredentials is null. Skipping update for PID '{}'", DAY_CQ_MAILER_PID);
      return;
    }
    try {
      Configuration cfg = configurationAdmin.getConfiguration(DAY_CQ_MAILER_PID, null);
      cfg.update(mergeProperties(cfg, credentials));
      LOG.info("Successfully updated mail service credentials for PID '{}'", DAY_CQ_MAILER_PID);

    } catch (IOException e) {
      LOG.error("Failed to update mail service credentials for PID '{}'", DAY_CQ_MAILER_PID, e);
    }
  }

  /**
   * Copies all existing properties, then overlays credential keys.
   *
   * @param cfg         existing OSGi {@link Configuration}
   * @param credentials fresh credentials to apply
   * @return merged dictionary ready for {@link Configuration#update(Dictionary)}
   */
  private Dictionary<String, Object> mergeProperties(Configuration cfg, MailCredentials credentials) {
    Dictionary<String, Object> merged = new Hashtable<>();
    Dictionary<String, Object> existing = cfg.getProperties();
    if (Objects.nonNull(existing)) {
      Enumeration<String> keys = existing.keys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement();
        merged.put(key, existing.get(key));
      }
    }
    merged.put(SMTP_HOST, credentials.getSmtpHost());
    merged.put(SMTP_PORT, credentials.getSmtpPort());
    merged.put(SMTP_USER, credentials.getUsername());
    merged.put(SMTP_PASSWORD, credentials.getPassword());
    merged.put(SMTP_SSL, credentials.isSslEnabled());
    if (!credentials.getFromAddress().isEmpty()) {
      merged.put(FROM_ADDRESS, credentials.getFromAddress());
    }
    return merged;
  }
}