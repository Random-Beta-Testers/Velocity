package com.velocitypowered.proxy.connection.client.auth;

import com.google.common.net.UrlEscapers;
import com.google.gson.JsonParseException;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Authenticates players against the Ely.by session server.
 */
public class ElyByAuthenticationProvider implements AuthenticationProvider {
  private static final Logger logger = LogManager.getLogger(ElyByAuthenticationProvider.class);

  private final String sessionServerUrl;
  private final String userAgent;

  public ElyByAuthenticationProvider(String sessionServerUrl, String userAgent) {
    this.sessionServerUrl = Objects.requireNonNull(sessionServerUrl, "sessionServerUrl");
    this.userAgent = Objects.requireNonNull(userAgent, "userAgent");
  }

  @Override
  public String name() {
    return "ElyBy";
  }

  @Override
  public CompletableFuture<AuthenticationResult> authenticate(
      HttpClient httpClient,
      String username,
      String serverId,
      @Nullable String playerIp) {
    final HttpRequest request;
    try {
      String url = buildRequestUrl(username, serverId, playerIp);
      request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("User-Agent", userAgent)
        .GET()
        .build();
    } catch (IllegalArgumentException e) {
      logger.error("Invalid Ely.by session server URL: {}", sessionServerUrl, e);
      return CompletableFuture.completedFuture(new AuthenticationResult.Failure(null, e));
    }

    return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
      .handle((response, throwable) -> handleResponse(username, response, throwable));
  }

  private String buildRequestUrl(String username, String serverId, @Nullable String playerIp) {
    var escaper = UrlEscapers.urlFormParameterEscaper();
    String separator = sessionServerUrl.contains("?") ? "&" : "?";

    StringBuilder url = new StringBuilder(sessionServerUrl.length() + 128)
        .append(separator)
        .append("username=").append(escaper.escape(username))
        .append("&serverId=").append(escaper.escape(serverId));

    if (playerIp != null) {
      url.append("&ip=").append(escaper.escape(playerIp));
    }

    return url.toString();
  }

  private AuthenticationResult handleResponse(
      String username,
      @Nullable HttpResponse<String> response,
      @Nullable Throwable throwable) {
    if (throwable != null) {
      Throwable cause = (throwable instanceof CompletionException && throwable.getCause() != null)
          ? throwable.getCause()
          : throwable;
      return new AuthenticationResult.Failure(null, cause);
    }

    if (response == null) {
      return new AuthenticationResult.Failure(null, null);
    }

    int statusCode = response.statusCode();

    if (statusCode == 200) {
      try {
        GameProfile profile = VelocityServer.GENERAL_GSON.fromJson(
            response.body(), GameProfile.class);

        if (profile == null || profile.getId() == null) {
          logger.error(
              "Ely.by returned HTTP 200 with invalid profile for user {}: {}",
              username, response.body());
          return new AuthenticationResult.Failure(statusCode, null);
        }

        return new AuthenticationResult.Success(profile);
      } catch (JsonParseException e) {
        logger.error("Failed to parse Ely.by session response for user {}", username, e);
        return new AuthenticationResult.Failure(statusCode, e);
      }
    }

    if (statusCode == 204) {
      return new AuthenticationResult.NotFound();
    }

    logger.error(
            "Unexpected status code {} from Ely.by session server for user {}",
            statusCode, username);
    return new AuthenticationResult.Failure(statusCode, null);
  }
}