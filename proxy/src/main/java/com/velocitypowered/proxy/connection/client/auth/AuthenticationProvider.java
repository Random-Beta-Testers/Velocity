package com.velocitypowered.proxy.connection.client.auth;

import java.net.http.HttpClient;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Authenticates players against an external session server.
 */
public interface AuthenticationProvider {

  String name();

  CompletableFuture<AuthenticationResult> authenticate(
      HttpClient httpClient,
      String username,
      String serverId,
      @Nullable String playerIp);
}
