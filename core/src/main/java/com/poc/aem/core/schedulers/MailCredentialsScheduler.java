package com.poc.aem.core.schedulers;

import com.poc.aem.core.configs.AwsSecretsConfiguration;
import com.poc.aem.core.models.MailCredentials;
import com.poc.aem.core.services.AwsSecretsReaderService;
import com.poc.aem.core.services.CredentialsPropertiesReaderService;
import com.poc.aem.core.services.MailCredentialsUpdaterService;
import java.util.Objects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling Scheduler that periodically refreshes SMTP credentials in the Day CQ Mail Service.
 *
 * <h3>Source priority</h3>
 * Controlled by {@link AwsSecretsConfiguration#preferAwsSecrets()}:
 * <ul>
 *   <li>{@code true}  - AWS primary, credentials.properties fallback</li>
 *   <li>{@code false} - credentials.properties primary, AWS fallback</li>
 * </ul>
 * Only if both sources fail is an error logged and the run skipped;
 * the previous credentials remain active until the next scheduled run.
 *
 * <h3>Scheduling</h3>
 * The CRON expression from {@link AwsSecretsConfiguration#schedulerExpression()}
 * is applied via the {@code scheduler.expression} SCR property.
 * An eager refresh also runs on activation so credentials are valid from startup.
 *
 * <h3>Concurrency</h3>
 * {@code scheduler.concurrent=false} prevents overlapping executions.
 */
@Component(service = Runnable.class, immediate = true, property = {"scheduler.name=AEM Mail Credentials Refresh",
    "scheduler.concurrent:Boolean=false"})
@Designate(ocd = AwsSecretsConfiguration.class)
public class MailCredentialsScheduler implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(MailCredentialsScheduler.class);

  @Reference
  private AwsSecretsReaderService awsSecretsReaderService;

  @Reference
  private CredentialsPropertiesReaderService credentialsPropertiesReaderService;

  @Reference
  private MailCredentialsUpdaterService mailCredentialsUpdaterService;

  private volatile AwsSecretsConfiguration config;

  /**
   * Stores configuration and runs an immediate refresh on startup.
   *
   * @param configuration OSGi configuration supplied by the SCR
   */
  @Activate
  @Modified
  protected void activate(final AwsSecretsConfiguration configuration) {
    this.config = configuration;
    run();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void run() {
    MailCredentials credentials = null;
    if (config.preferAwsSecrets()) {
      credentials = awsSecretsReaderService.readCredentials();
      if (Objects.isNull(credentials)) {
        LOG.warn("AWS credentials unavailable. Falling back to properties file.");
        credentials = credentialsPropertiesReaderService.readCredentials();
      }

    } else {
      credentials = credentialsPropertiesReaderService.readCredentials();
      if (Objects.isNull(credentials)) {
        LOG.warn("Properties credentials unavailable. Falling back to AWS Secrets.");
        credentials = awsSecretsReaderService.readCredentials();
      }
    }
    if (Objects.isNull(credentials)) {
      LOG.error("Credential refresh failed - both sources returned null.");
      return;
    }
    mailCredentialsUpdaterService.updateMailServiceCredentials(credentials);
  }
}