package com.github.stephenc.continuous.k2p.source;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.util.Config;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SecretInjectionSource implements InjectionSource {
    private static final Pattern PATH = Pattern.compile("^//(?<namespace>[^/]+)/(?<name>[^/]+)/(?<key>[^/]+)$");

    private final String namespace;
    private final String name;
    private V1Secret values;

    public SecretInjectionSource(String path) {
        final Matcher matcher = PATH.matcher(path);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Malformed path to secret value: " + path);
        }
        namespace = matcher.group("namespace");
        name = matcher.group("name");
    }

    @Override
    public void resolve() throws IOException {
        ApiClient client = Config.defaultClient();
        CoreV1Api api = new CoreV1Api(client);
        try {
            values = api.readNamespacedSecret(name, namespace, null, null, null);
        } catch (ApiException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SecretInjectionSource that = (SecretInjectionSource) o;

        if (!namespace.equals(that.namespace)) {
            return false;
        }
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = namespace.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public int compareTo(InjectionSource o) {
        if (this == o) {
            return 0;
        }
        if (getClass() != o.getClass()) {
            return o.getClass().getName().compareTo(getClass().getName());
        }

        SecretInjectionSource that = (SecretInjectionSource) o;

        int rv = namespace.compareTo(that.namespace);
        if (rv != 0) {
            return rv;
        }
        return name.compareTo(that.name);
    }

    public V1Secret getValues() {
        return values;
    }
}
