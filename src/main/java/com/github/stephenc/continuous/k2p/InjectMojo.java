package com.github.stephenc.continuous.k2p;

import com.github.stephenc.continuous.k2p.injectable.AbstractInjectable;
import com.github.stephenc.continuous.k2p.source.InjectionSource;
import com.github.stephenc.continuous.k2p.util.CommandGCPAuthenticator;
import io.kubernetes.client.util.KubeConfig;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Injects the configured properties into the running build.
 */
@Mojo(
        name = "inject",
        defaultPhase = LifecyclePhase.VALIDATE,
        requiresOnline = false,
        requiresProject = true,
        threadSafe = true
)
public class InjectMojo extends AbstractMojo {
    private static final Pattern VALUE_SCHEME = Pattern.compile("^([a-zA-Z0-9-]+)([?1]?):(.*)$");

    static {
        KubeConfig.registerAuthenticator(new CommandGCPAuthenticator());
    }
    /**
     * Disables injection.
     */
    @Parameter(property = "skipK2p")
    boolean skip;
    /**
     * Inject into system properties by default.
     */
    @Parameter(defaultValue = "true", property = "maven.k2p.setSystemProperties")
    boolean setSystemProperties;
    /**
     * Inject into build properties by default.
     */
    @Parameter(defaultValue = "true", property = "maven.k2p.setBuildProperties")
    boolean setBuildProperties;
    /**
     * List of properties to inject. Values are prefixed with a scheme as follows:
     * <dl>
     *     <dt><code>value:</code></dt>
     *     <dd>Inject the value after the prefix. There are other plugins that can set properties but this can be
     *     useful if setting a bunch of properties from Kubernetes</dd>
     *     <dt><code>file:</code></dt>
     *     <dd>Inject the contents of the named local file. If the file does not exist an empty string will be
     *     injected</dd>
     *     <dt><code>file!:</code></dt>
     *     <dd>Inject the contents of the named local file. If the file does not exist error will be raised</dd>
     *     <dt><code>file?:</code></dt>
     *     <dd>Inject the contents of the named local file. If the file does not exist nothing will be injected</dd>
     *     <dt><code>secret://<i>{namespace}</i>/{name}/{property}</code></code></dt>
     *     <dd>Inject the value of the specific property in the named secret. If the secret does not exist or does
     *     not contain the named property an empty string will be injected</dd>
     *     <dt><code>secret!://<i>{namespace}</i>/{name}/{property}</code></dt>
     *     <dd>Inject the value of the specific property in the named secret. If the secret does not exist or does
     *     not contain the named property an error will be raised</dd>
     *     <dt><code>secret?://<i>{namespace}</i>/{name}/{property}</code></dt>
     *     <dd>Inject the value of the specific property in the named secret. If the secret does not exist or does
     *     not contain the named property nothing will be injected</dd>
     *     <dt><code>configMap://<i>{namespace}</i>/{name}/{property}</code></dt>
     *     <dd>Inject the value of the specific property in the named configMap. If the configMap does not exist or
     *     does not contain the named property an empty string will be injected</dd>
     *     <dt><code>configMap!://<i>{namespace}</i>/{name}/{property}</code></dt>
     *     <dd>Inject the value of the specific property in the named configMap. If the configMap does not exist or
     *     does not contain the named property an error will be raised</dd>
     *     <dt><code>configMap?://<i>{namespace}</i>/{name}/{property}</dt>
     *     <dd>Inject the value of the specific property in the named configMap. If the configMap does not exist or
     *     does not contain the named property nothing will be injected</dd>
     *     </dl>
     */
    @Parameter
    Map<String, String> properties;

    /**
     * The maven project
     */
    @Parameter(readonly = true, defaultValue = "${project}")
    protected MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Injection is skipped");
            return;
        }
        if (properties == null || properties.isEmpty()) {
            getLog().info("No properties configured for injection");
            return;
        }

        Map<String, Class<? extends AbstractInjectable<? extends InjectionSource>>> factories = new HashMap<>();
        List<AbstractInjectable<? extends InjectionSource>> injectables = new ArrayList<>();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            final Matcher matcher = VALUE_SCHEME.matcher(entry.getValue());
            if (!matcher.matches()) {
                throw new MojoFailureException("Property " + entry.getKey() + " value " + entry.getValue()
                        + " does not start with a source prefix");
            }
            MissingValueStrategy strategy;
            switch (matcher.group(1)) {
                case "?":
                    strategy = MissingValueStrategy.UNSET_IF_MISSING;
                    break;
                case "!":
                    strategy = MissingValueStrategy.ERROR_IF_MISSING;
                    break;
                default:
                    strategy = MissingValueStrategy.EMPTY_IF_MISSING;
                    break;
            }
            AbstractInjectable<? extends InjectionSource> source;
            String name = matcher.group(1).toLowerCase(Locale.ENGLISH);
            Class<? extends AbstractInjectable<? extends InjectionSource>> factory = factories.get(name);
            if (factory == null) {
                String className = AbstractInjectable.class.getName();
                className =
                        className.substring(0, className.length() - AbstractInjectable.class.getSimpleName().length());
                className =
                        className + name.substring(0, 1).toUpperCase(Locale.ENGLISH) + name.substring(1) + "Injectable";
                try {
                    factory = (Class<? extends AbstractInjectable<? extends InjectionSource>>)
                            getClass().getClassLoader().loadClass(className);
                } catch (ClassNotFoundException e) {
                    throw new MojoFailureException("Property " + entry.getKey() + " value " + entry.getValue()
                            + " does not start with a known source prefix");
                }
                factories.put(name, factory);
            }
            try {
                final Constructor<? extends AbstractInjectable<? extends InjectionSource>> constructor =
                        factory.getConstructor(String.class, MissingValueStrategy.class, String.class);
                injectables.add(constructor.newInstance(entry.getKey(), strategy, matcher.group(3)));
            } catch (NoSuchMethodException e) {
                throw new MojoFailureException("Missing constructor for " + factory.getName(), e);
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                throw new MojoExecutionException("Could not define injectable for property " + entry.getKey(), e);
            }
        }
        Collections.sort(injectables, new Comparator<AbstractInjectable<? extends InjectionSource>>() {
            @Override
            public int compare(AbstractInjectable o1, AbstractInjectable o2) {
                return o1.getSource().compareTo(o2.getSource());
            }
        });
        InjectionSource lastSource = null;
        INJECTABLES:
        for (AbstractInjectable<? extends InjectionSource> injectable : injectables) {
            String value;
            if (lastSource == null || !lastSource.equals(injectable.getSource())) {
                lastSource = injectable.getSource();
                try {
                    lastSource.resolve();
                    value = injectable.valueOf(lastSource);
                } catch (IOException e) {
                    getLog().debug("Could not resolve source of " + injectable.getName() + " from " + properties
                            .get(injectable.getName()), e);
                    value = null;
                }
            } else {
                value = injectable.valueOf(lastSource);
            }
            if (value == null) {
                switch (injectable.getStrategy()) {
                    case EMPTY_IF_MISSING:
                        getLog().debug(
                                "Property " + injectable.getName() + " from " + properties
                                        .get(injectable.getName()) + " was missing, using empty string");
                        value = "";
                        break;
                    case UNSET_IF_MISSING:
                        getLog().debug(
                                "Property " + injectable.getName() + " from " + properties
                                        .get(injectable.getName()) + " was missing, leaving unmodified");
                        continue INJECTABLES;
                    case ERROR_IF_MISSING:
                        throw new MojoExecutionException(
                                "Could not resolve a value for property " + injectable.getName() + " from " + properties
                                        .get(injectable.getName()));
                }
            }
            if (setBuildProperties) {
                getLog().debug("Setting build property " + injectable.getName() + " to " + (injectable.isRedacted()
                        ? "*REDACTED*"
                        : value));
                project.getProperties().setProperty(injectable.getName(), value);
            }
            if (setSystemProperties) {
                getLog().debug("Setting system property " + injectable.getName() + " to " + (injectable.isRedacted()
                        ? "*REDACTED*"
                        : value));
                System.setProperty(injectable.getName(), value);
            }
        }
        getLog().info("Injected " + injectables.size() + " properties");
    }
}
