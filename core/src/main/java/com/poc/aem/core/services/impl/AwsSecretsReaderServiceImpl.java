package com.poc.aem.core.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.poc.aem.core.configs.AwsSecretsConfiguration;
import com.poc.aem.core.models.MailCredentials;
import com.poc.aem.core.services.AwsSecretsReaderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;


/**
 * Default implementation of {@link AwsSecretsReaderService}.
 *
 * <h3>Retry strategy</h3>
 * A plain counted loop retries up to {@code retryAttempts} times on {@link SecretsManagerException}. No sleep or
 * back-off is applied; the AWS SDK handles connection-level timeouts internally. A failure is only logged once all
 * attempts are exhausted.
 *
 * <h3>AWS authentication</h3>
 * Uses {@link DefaultCredentialsProvider}: environment variables, Java system properties, AWS credentials file, then
 * instance metadata. No credentials are present in code or configuration.
 *
 * <h3>Secret format</h3>
 * <pre>{@code
 * {
 *   "username"   : "...",  "password"   : "...",
 *   "smtpHost"   : "...",  "smtpPort"   : "587",
 *   "sslEnabled" : "false","fromAddress": "..."
 * }
 * }</pre>
 */
@Component(service = AwsSecretsReaderService.class, immediate = true)
@Designate(ocd = AwsSecretsConfiguration.class)
public class AwsSecretsReaderServiceImpl implements AwsSecretsReaderService {

  /**
   * JSON key for username used in mail credentials.
   */
  private static final String USERNAME = "username";

  /**
   * JSON key for password used in mail credentials.
   */
  private static final String PASSWORD = "password";

  /**
   * JSON key for SMTP host address.
   */
  private static final String SMTP_HOST = "smtpHost";

  /**
   * JSON key for SMTP port number.
   */
  private static final String SMTP_PORT = "smtpPort";

  /**
   * JSON key indicating whether SSL is enabled.
   */
  private static final String SSL_ENABLED = "sslEnabled";

  /**
   * JSON key for sender email address.
   */
  private static final String FROM_ADDRESS = "fromAddress";

  private static final Logger LOG = LoggerFactory.getLogger(AwsSecretsReaderServiceImpl.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private AwsSecretsConfiguration config;
  private SecretsManagerClient secretsClient;

  /**
   * Builds the AWS client. Called on first activation and on every config change.
   *
   * @param configuration OSGi configuration supplied by the SCR
   */
  @Activate
  @Modified
  protected void activate(AwsSecretsConfiguration configuration) {
    this.config = configuration;
    closeClient();
    this.secretsClient = SecretsManagerClient.builder().region(Region.of(configuration.awsRegion()))
        .credentialsProvider(DefaultCredentialsProvider.create()).build();
  }

  /**
   * Releases the AWS client on component deactivation.
   */
  @Deactivate
  protected void deactivate() {
    closeClient();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MailCredentials readCredentials() {
    int maxAttempts = config.retryAttempts();
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        return fetchAndParse();
      } catch (SecretsManagerException ex) {
        LOG.warn("Attempt {}/{} failed while reading secret '{}'. Error: {}", attempt, maxAttempts,
            config.awsSecretName(), ex.getMessage(), ex);
      }
    }
    return null;
  }

  /**
   * Makes one API call and parses the result into {@link MailCredentials}.
   *
   * @return populated credentials
   * @throws SecretsManagerException  on any AWS-side error
   * @throws IllegalArgumentException if a required JSON field is absent or malformed
   */
  private MailCredentials fetchAndParse() {
    String secretString = secretsClient.getSecretValue(
        GetSecretValueRequest.builder().secretId(config.awsSecretName()).build()).secretString();
    JsonNode root;
    try {
      root = MAPPER.readTree(secretString);
    } catch (JsonProcessingException e) {
      LOG.error("Failed to parse secret '{}' JSON. Error: {}", config.awsSecretName(), e.getMessage(), e);
      return null;
    }
    return new MailCredentials.Builder().username(required(root, USERNAME))
        .password(required(root, PASSWORD)).smtpHost(required(root, SMTP_HOST))
        .smtpPort(Integer.parseInt(Objects.requireNonNull(required(root, SMTP_PORT))))
        .sslEnabled(Boolean.parseBoolean(root.path(SSL_ENABLED).asText("false")))
        .fromAddress(root.path(FROM_ADDRESS).asText(StringUtils.EMPTY)).build();

  }

  /**
   * Returns the non-blank text of a required JSON field.
   *
   * @param root      root JSON node
   * @param fieldName field to look up
   * @return trimmed, non-empty value
   * @throws IllegalArgumentException if the field is absent or blank
   */
  private String required(JsonNode root, String fieldName) {
    String value = root.path(fieldName).asText(StringUtils.EMPTY).trim();
    if (value.isEmpty()) {
      LOG.info("Missing or empty required field '{}' in secret '{}'", fieldName, config.awsSecretName());
      return null;
    }
    return value;
  }

  /**
   * Closes the AWS client if it has been initialised.
   */
  private void closeClient() {
    if (Objects.nonNull(secretsClient)) {
      secretsClient.close();
    }
  }
}