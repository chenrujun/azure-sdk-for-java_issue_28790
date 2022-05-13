// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.spring.keyvault.secrets.sample.single.property.source;

import com.azure.core.http.rest.PagedResponse;
import com.azure.core.util.paging.ContinuablePagedIterable;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.azure.security.keyvault.secrets.models.SecretProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@SpringBootApplication
public class PropertySourceApplication implements CommandLineRunner {

    @Value("${client-id}")
    private String clientId;
    @Value("${client-secret}")
    private String clientSecret;
    @Value("${tenant-id}")
    private String tenantId;
    @Value("${vault-uri}")
    private String vaultUri;

    public static void main(String[] args) {
        SpringApplication.run(PropertySourceApplication.class, args);
    }

    public void run(String[] args) {
        ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tenantId(tenantId)
                .build();
        final SecretClient secretClient = new SecretClientBuilder()
                .vaultUrl(vaultUri)
                .credential(clientSecretCredential)
                .buildClient();
        Map<String, String> properties = Optional.of(secretClient)
                .map(SecretClient::listPropertiesOfSecrets)
                .map(ContinuablePagedIterable::iterableByPage)
                .map(i -> StreamSupport.stream(i.spliterator(), false))
                .orElseGet(Stream::empty)
                .map(PagedResponse::getElements)
                .flatMap(i -> StreamSupport.stream(i.spliterator(), false))
                .filter(SecretProperties::isEnabled)
                .map(p -> secretClient.getSecret(p.getName(), p.getVersion()))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        KeyVaultSecret::getName,
                        KeyVaultSecret::getValue
                ));
        properties.forEach((key, value) -> System.out.println(key + ": " + value));
    }

}
