package com.poc.aem.core.services.impl;

import com.poc.aem.core.configs.AwsSecretsConfiguration;
import com.poc.aem.core.models.MailCredentials;
import com.poc.aem.core.services.CredentialsPropertiesReaderService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

@Component(service = CredentialsPropertiesReaderService.class, immediate = true)
@Designate(ocd = AwsSecretsConfiguration.class)
public class CredentialsPropertiesReaderServiceImpl implements CredentialsPropertiesReaderService {

  private static final Logger LOG = LoggerFactory.getLogger(CredentialsPropertiesReaderServiceImpl.class);

  private static final String PROP_HOST = "smtp.host";
  private static final String PROP_PORT = "smtp.port";
  private static final String PROP_USERNAME = "smtp.username";
  private static final String PROP_PASSWORD = "smtp.password";
  private static final String PROP_SSL_ENABLED = "smtp.ssl.enabled";
  private static final String PROP_FROM_ADDRESS = "smtp.from.address";

  private AwsSecretsConfiguration config;
  private S3Client s3Client;

  @Activate
  @Modified
  protected void activate(AwsSecretsConfiguration configuration) {
    this.config = configuration;
    closeClient();
    this.s3Client = S3Client.builder().region(Region.of(configuration.awsRegion()))
        .credentialsProvider(DefaultCredentialsProvider.create()).build();
  }

  @Deactivate
  protected void deactivate() {
    closeClient();
  }

  @Override
  public MailCredentials readCredentials() {
    Properties props = new Properties();
    try (ResponseInputStream<GetObjectResponse> stream = s3Client.getObject(
        GetObjectRequest.builder().bucket(config.credentialsBucketName()).key(config.credentialsS3Key()).build())) {
      props.load(stream);

    } catch (S3Exception ex) {
      LOG.error("Failed to fetch credentials from S3 bucket '{}' and key '{}'", config.credentialsBucketName(),
          config.credentialsS3Key(), ex);
      return null;

    } catch (IOException ex) {
      LOG.error("Failed to load properties from S3 stream", ex);
      return null;
    }
    String host = required(props, PROP_HOST);
    String portStr = required(props, PROP_PORT);
    String username = required(props, PROP_USERNAME);
    String password = required(props, PROP_PASSWORD);
    if (Objects.isNull(host) || Objects.isNull(portStr) || Objects.isNull(username) || Objects.isNull(password)) {
      LOG.error("Missing required SMTP properties in S3 credentials file");
      return null;
    }
    Integer port = parsePort(portStr);
    if (Objects.isNull(port)) {
      return null;
    }
    return new MailCredentials.Builder().smtpHost(host).smtpPort(port).username(username).password(password)
        .sslEnabled(Boolean.parseBoolean(props.getProperty(PROP_SSL_ENABLED, "false")))
        .fromAddress(props.getProperty(PROP_FROM_ADDRESS, "")).build();
  }

  private String required(Properties props, String key) {
    String value = props.getProperty(key, "").trim();
    if (value.isEmpty()) {
      LOG.error("Required property '{}' is missing or empty in S3 credentials file", key);
      return null;
    }
    return value;
  }

  private Integer parsePort(String portStr) {
    return Integer.parseInt(portStr);
  }

  private void closeClient() {
    if (Objects.nonNull(s3Client)) {
      s3Client.close();
    }
  }
}