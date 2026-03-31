package com.poc.aem.core.configs;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * OSGi configuration for all AWS Secrets and mail credential services.
 *
 * <h3>AEM Cloud secret injection</h3>
 * On AEM as a Cloud Service, sensitive values such as the secret name and the
 * S3 bucket name are never hard-coded. Instead the cfg.json references them
 * via the {@code $[secret:variableName]} placeholder syntax. Cloud Manager
 * injects the real value at runtime from the environment's secret variables.
 *
 * <h3>credentials.properties on AWS</h3>
 * The file does not live on the AEM server's local disk. It is stored in an
 * Amazon S3 bucket and fetched by {@code CredentialsPropertiesReaderService}
 * using the AWS SDK. Two config values identify the object: the bucket name
 * ({@link #credentialsBucketName()}) and the object key
 * ({@link #credentialsS3Key()}).
 *
 * <p>Bind with {@code @Designate(ocd = AwsSecretsConfiguration.class)}.</p>
 */
@ObjectClassDefinition(
    name        = "AEM - AWS Secrets and Mail Credentials",
    description = "Configures how AEM reads SMTP credentials from AWS Secrets Manager "
        + "and the fallback credentials.properties file stored in Amazon S3."
)
public @interface AwsSecretsConfiguration {

  /**
   * @return AWS region for both Secrets Manager and S3, e.g. {@code us-east-1}
   */
  @AttributeDefinition(name = "AWS Region")
  String awsRegion() default "us-east-1";

  /**
   * Name or ARN of the AWS Secrets Manager secret that holds SMTP credentials.
   *
   * <p><strong>AEM Cloud:</strong> set this to {@code $[secret:smtpSecretName]}
   * in the cfg.json. Cloud Manager resolves the placeholder at runtime from the
   * environment's secret variable named {@code smtpSecretName}.</p>
   *
   * @return secret name or ARN; injected by Cloud Manager on AEM as a Cloud Service
   */
  @AttributeDefinition(name = "Secret Name")
  String awsSecretName();


  /**
   * Maximum read attempts for the AWS Secrets Manager call before failing.
   *
   * @return attempt count; minimum 1
   */
  @AttributeDefinition(name = "Retry Attempts", type = AttributeType.INTEGER)
  int retryAttempts();

  /**
   * Name of the S3 bucket that contains the {@code credentials.properties} file.
   *
   * <p><strong>AEM Cloud:</strong> set this to {@code $[secret:credentialsBucketName]}
   * in the cfg.json so the bucket name is never committed to source control.</p>
   *
   * @return S3 bucket name; injected by Cloud Manager on AEM as a Cloud Service
   */
  @AttributeDefinition(name = "Credentials S3 Bucket Name")
  String credentialsBucketName();

  /**
   * S3 object key (path) of the credentials.properties file inside the bucket,
   * e.g. {@code config/credentials.properties}.
   *
   * @return S3 object key
   */
  @AttributeDefinition(name = "Credentials S3 Object Key")
  String credentialsS3Key();


  /**
   * @return Sling Scheduler CRON expression controlling the refresh interval
   */
  @AttributeDefinition(name = "Scheduler Expression")
  String schedulerExpression();

  /**
   * @return {@code true} to prefer AWS Secrets Manager;
   *         {@code false} to prefer the S3 credentials.properties file
   */
  @AttributeDefinition(name = "Prefer AWS Secrets Manager", type = AttributeType.BOOLEAN)
  boolean preferAwsSecrets();
}