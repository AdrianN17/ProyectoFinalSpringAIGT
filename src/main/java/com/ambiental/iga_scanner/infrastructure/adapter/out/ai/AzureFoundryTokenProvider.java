package com.ambiental.iga_scanner.infrastructure.adapter.out.ai;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.AzureCliCredential;
import com.azure.identity.AzureCliCredentialBuilder;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

// Single Entra ID token source (the local `az login` session) shared by every Azure AI
// Foundry model adapter, regardless of which model provider is active.
@Component
class AzureFoundryTokenProvider {

    private static final String SCOPE = "https://ai.azure.com/.default";

    private final AzureCliCredential credential = new AzureCliCredentialBuilder().build();
    private volatile AccessToken cachedToken;

    String bearerToken() {
        AccessToken token = cachedToken;
        if (token == null || token.getExpiresAt().isBefore(OffsetDateTime.now().plusMinutes(2))) {
            token = credential.getTokenSync(new TokenRequestContext().addScopes(SCOPE));
            cachedToken = token;
        }
        return token.getToken();
    }
}
