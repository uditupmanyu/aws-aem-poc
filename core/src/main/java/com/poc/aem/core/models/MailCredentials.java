package com.poc.aem.core.models;

import org.apache.commons.lang3.StringUtils;

/**
 * Immutable value object holding SMTP credentials. Construct via the nested {@link Builder}.
 *
 * <p>Uses plain empty-string literals instead of Apache Commons
 * {@code StringUtils.EMPTY} to avoid an unnecessary dependency.</p>
 */
public final class MailCredentials {

  private final String smtpHost;
  private final int smtpPort;
  private final String username;
  private final String password;
  private final boolean sslEnabled;
  private final String fromAddress;

  private MailCredentials(final Builder builder) {
    this.smtpHost = builder.smtpHost;
    this.smtpPort = builder.smtpPort;
    this.username = builder.username;
    this.password = builder.password;
    this.sslEnabled = builder.sslEnabled;
    this.fromAddress = builder.fromAddress;
  }

  /**
   * @return SMTP server hostname
   */
  public String getSmtpHost() {
    return smtpHost;
  }

  /**
   * @return SMTP port
   */
  public int getSmtpPort() {
    return smtpPort;
  }

  /**
   * @return SMTP username
   */
  public String getUsername() {
    return username;
  }

  /**
   * @return SMTP password
   */
  public String getPassword() {
    return password;
  }

  /**
   * @return true if SSL/TLS is enabled
   */
  public boolean isSslEnabled() {
    return sslEnabled;
  }

  /**
   * @return sender "From" address
   */
  public String getFromAddress() {
    return fromAddress;
  }

  /**
   * Fluent builder for {@link MailCredentials}.
   */
  public static final class Builder {

    private String smtpHost = StringUtils.EMPTY;

    private int smtpPort = 587;
    private String username = StringUtils.EMPTY;

    private String password = StringUtils.EMPTY;

    private boolean sslEnabled = false;
    private String fromAddress = StringUtils.EMPTY;

    public Builder smtpHost(final String smtpHost) {
      this.smtpHost = smtpHost;
      return this;
    }

    public Builder smtpPort(final int smtpPort) {
      this.smtpPort = smtpPort;
      return this;
    }

    public Builder username(final String username) {
      this.username = username;
      return this;
    }

    public Builder password(final String password) {
      this.password = password;
      return this;
    }

    public Builder sslEnabled(final boolean sslEnabled) {
      this.sslEnabled = sslEnabled;
      return this;
    }

    public Builder fromAddress(final String fromAddress) {
      this.fromAddress = fromAddress;
      return this;
    }

    /**
     * @return a new immutable {@link MailCredentials}
     */
    public MailCredentials build() {
      return new MailCredentials(this);
    }
  }
}