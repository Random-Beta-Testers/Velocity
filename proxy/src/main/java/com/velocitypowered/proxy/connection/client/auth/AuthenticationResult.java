package com.velocitypowered.proxy.connection.client.auth;

import com.velocitypowered.api.util.GameProfile;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The result of an authentication attempt.
 */
public sealed interface AuthenticationResult {

  /**
   * Indicates that authentication succeeded.
   *
   * @param profile the authenticated player's profile
   */
  record Success(GameProfile profile) implements AuthenticationResult {

    public Success {
      Objects.requireNonNull(profile, "profile");
    }
  }

  /**
   * Indicates that the account was not found.
   */
  record NotFound() implements AuthenticationResult {

    public static final NotFound INSTANCE = new NotFound();
  }

  /**
   * Indicates that authentication failed.
   *
   * @param statusCode the HTTP status code, if any
   * @param error the error that caused the failure, if any
   */
  record Failure(@Nullable Integer statusCode, @Nullable Throwable error)
      implements AuthenticationResult {

    public static Failure status(int statusCode) {
      return new Failure(statusCode, null);
    }

    public static Failure error(Throwable error) {
      return new Failure(null, Objects.requireNonNull(error, "error"));
    }
  }
}
