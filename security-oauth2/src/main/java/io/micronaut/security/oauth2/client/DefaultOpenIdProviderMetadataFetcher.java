/*
 * Copyright 2017-2023 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.security.oauth2.client;

import io.micronaut.context.exceptions.DisabledBeanException;
import io.micronaut.core.annotation.Blocking;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.optim.StaticOptimizations;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.security.oauth2.configuration.OpenIdClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Default implementation of {@link OpenIdProviderMetadataFetcher}.
 *
 * @author Sergio del Amo
 * @since 3.9.0
 */
public class DefaultOpenIdProviderMetadataFetcher implements OpenIdProviderMetadataFetcher {
    public static final Optimizations OPTIMIZATIONS = StaticOptimizations.get(Optimizations.class).orElse(new Optimizations(Collections.emptyMap()));

    private static final Logger LOG = LoggerFactory.getLogger(DefaultOpenIdProviderMetadataFetcher.class);
    private final HttpClient client;
    private final OpenIdClientConfiguration openIdClientConfiguration;

    /**
     * @param openIdClientConfiguration OpenID Client Configuration
     * @param client HTTP Client
     */
    public DefaultOpenIdProviderMetadataFetcher(OpenIdClientConfiguration openIdClientConfiguration,
                                                @Client HttpClient client) {
        this.openIdClientConfiguration = openIdClientConfiguration;
        this.client = client;
    }

    @Override
    @NonNull
    public String getName() {
        return openIdClientConfiguration.getName();
    }

    @Override
    @Blocking
    @NonNull
    public DefaultOpenIdProviderMetadata fetch() {
        return OPTIMIZATIONS.findMetadata(openIdClientConfiguration.getName())
                .map(Supplier::get)
                .orElseGet(fetch(openIdClientConfiguration));
    }

    @NonNull
    private Supplier<DefaultOpenIdProviderMetadata> fetch(@NonNull OpenIdClientConfiguration openIdClientConfiguration) {
        return () -> openIdClientConfiguration.getIssuer()
            .map(this::fetch)
            .orElse(new DefaultOpenIdProviderMetadata());
    }

    @NonNull
    private DefaultOpenIdProviderMetadata fetch(@NonNull URL issuer) {
        try {
            URL configurationUrl = new URL(issuer, StringUtils.prependUri(issuer.getPath(), openIdClientConfiguration.getConfigurationPath()));
            if (LOG.isDebugEnabled()) {
                LOG.debug("Sending request for OpenID configuration for provider [{}] to URL [{}] running in thread {}", openIdClientConfiguration.getName(), configurationUrl, Thread.currentThread().getName());
            }
            return client.toBlocking().retrieve(configurationUrl.toString(), DefaultOpenIdProviderMetadata.class);
        } catch (HttpClientException e) {
            throw new DisabledBeanException("Bean of type " + DefaultOpenIdProviderMetadata.class.getName() + " with name quailfier " + openIdClientConfiguration.getName() + " is disabled. Failed to retrieve OpenID configuration for " + openIdClientConfiguration.getName());
        } catch (MalformedURLException e) {
            throw new DisabledBeanException("Bean of type " + DefaultOpenIdProviderMetadata.class.getName() + " with name quailfier " + openIdClientConfiguration.getName() + " is disabled. Failure parsing issuer URL " + issuer);
        }
    }

    /**
     * AOT Optimizations.
     */
    public static class Optimizations {
        private final Map<String, Supplier<DefaultOpenIdProviderMetadata>> suppliers;

        /**
         * @param suppliers Map with key being the OpenID Name qualifier and
         */
        public Optimizations(Map<String, Supplier<DefaultOpenIdProviderMetadata>> suppliers) {
            this.suppliers = suppliers;
        }

        /**
         * @param name name qualifier
         * @return {@link DefaultOpenIdProviderMetadata} supplier or empty optional if not found for the given name qualifier.
         */
        public Optional<Supplier<DefaultOpenIdProviderMetadata>> findMetadata(String name) {
            return Optional.ofNullable(suppliers.get(name));
        }
    }
}
