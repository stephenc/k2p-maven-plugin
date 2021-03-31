package com.github.stephenc.continuous.k2p.util;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.util.KubeConfig;
import io.kubernetes.client.util.authenticators.GCPAuthenticator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A GCP Authenticator that works if there is a {@code cmd-path} and optional {@code cmd-args} that returns the JSON
 * format of the token.
 */
public class CommandGCPAuthenticator extends GCPAuthenticator {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandGCPAuthenticator.class);

    @Override
    public Map<String, Object> refresh(Map<String, Object> config) {
        if (config.containsKey("cmd-path")) {
            String cmdPath = (String) config.get("cmd-path");
            // see https://github.com/kubernetes/client-go/blob/67a7335497345f7562589ecaecccc0c7f6953c76/plugin/pkg
            // /client/auth/gcp/gcp.go#L96
            String tokenKey = asJsonPath(config, "token-key", "{.access_token}");
            // see https://github.com/kubernetes/client-go/blob/67a7335497345f7562589ecaecccc0c7f6953c76/plugin/pkg
            // /client/auth/gcp/gcp.go#L100-L101
            String expiryKey = asJsonPath(config, "expiry-key", "{.token_expiry}");
            List<String> cmdArgs;
            Object args = config.get("cmd-args");
            if (args instanceof List) {
                cmdArgs = new ArrayList<>(((List<?>) args).size());
                for (Object o : (List<?>) args) {
                    cmdArgs.add(Objects.toString(o));
                }
            } else {
                cmdArgs = Arrays.asList(Objects.toString(args).split("\\s+"));
            }
            CommandLine commandline = new CommandLine(cmdPath);
            commandline.addArguments(cmdArgs.toArray(new String[0]));

            Executor exec = new DefaultExecutor();
            exec.setWatchdog(new ExecuteWatchdog(TimeUnit.MINUTES.toMillis(1)));
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            int exitCode = -1;
            try (OutputStream stderr = new LineRedirectOutputStream(new Sink<String>() {
                @Override
                public void accept(String s) {
                    LOGGER.error(s);
                }
            })) {
                PumpStreamHandler psh = new PumpStreamHandler(stdout, stderr, new ByteArrayInputStream(new byte[0]));
                exec.setStreamHandler(psh);
                try {
                    psh.start();
                    exitCode = exec.execute(commandline);
                } finally {
                    psh.stop();
                }
            } catch (IOException e) {
                LOGGER.error("Could not refresh authentication", e);
            }
            if (exitCode != 0) {
                LOGGER.error("Could not refresh authentication, {} returned {}", commandline, exitCode);
            } else {
                Object document = Configuration.defaultConfiguration().jsonProvider().parse(new String(stdout.toByteArray(), StandardCharsets.UTF_8));
                Map<String,Object> result = new HashMap<>();
                result.put("access-token", JsonPath.read(document, tokenKey));
                result.put("expiry", JsonPath.read(document, expiryKey));
                return result;
            }
        }
        return super.refresh(config);
    }

    private static String asJsonPath(Map<String, Object> config, String s, String s2) {
        String expr = StringUtils.defaultIfBlank((String) config.get(s), s2);
        if (expr.startsWith("{") && expr.endsWith("}")) {
            // some strange form of JSONPath only known to kubernetes client_go
            return "$"+expr.substring(1, expr.length()-1);
        }
        return expr;
    }
}
