/*
 * Copyright (C) 2023 Zhenya Leonov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package software.leonov.config;

import static com.typesafe.config.ConfigValueType.BOOLEAN;
import static com.typesafe.config.ConfigValueType.LIST;
import static com.typesafe.config.ConfigValueType.NUMBER;
import static com.typesafe.config.ConfigValueType.OBJECT;
import static com.typesafe.config.ConfigValueType.STRING;
import static com.typesafe.config.impl.ConfigValues.getValue;
import static com.typesafe.config.impl.ConfigValues.toConfig;
import static com.typesafe.config.impl.ConfigValues.toDirectory;
import static com.typesafe.config.impl.ConfigValues.toDouble;
import static com.typesafe.config.impl.ConfigValues.toEnum;
import static com.typesafe.config.impl.ConfigValues.toInteger;
import static com.typesafe.config.impl.ConfigValues.toLong;
import static com.typesafe.config.impl.ConfigValues.toPath;
import static com.typesafe.config.impl.ConfigValues.toRegularFile;
import static com.typesafe.config.impl.ConfigValues.toShort;
import static com.typesafe.config.impl.ConfigValues.transformElement;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;

/**
 * This class offers users a better way to handle <i>optional</i> properties in the configuration library
 * <a href="https://github.com/lightbend/config" target="_blank">Typesafe Config</a>. It is a companion to the Typesafe
 * Config's {@link Config} class.
 * <p>
 * An instance of {@link OptionalConfig} is immutable and thread-safe. It can be constructed by passing a {@link Config}
 * object to the {@link #from(Config)} method. The {@link #setStrictMode(boolean)} determines how this class treats
 * properties with {@code null} values.
 * <p>
 * <strong>Overview:</strong>
 * <p>
 * Typesafe Config is one of the more popular configuration libraries for JVM languages. Unfortunately it doesn't offer
 * a practical way to handle optional properties. This project is aims to elevate optional properties to a first-class
 * feature by using Java's {@link Optional} class to offer a fluent and elegant solution.
 * <p>
 * For example:
 * 
 * <pre> 
 * final int nthreads = conf.getOptionalInteger("nthreads")
 *                          .orElse(Runtime.getRuntime().availableProcessors());
 *       
 * final ExecutorService exec = Executors.newFixedThreadPool(nthreads);
 * </pre>
 * <p>
 * <b>Please see</b> <a href="https://github.com/zleonov/optional-config" target="_blank">Optional Config on GitHub</a>
 * and Optional Config's <a href="https://github.com/zleonov/optional-config/wiki" target="_blank">Wiki</a> for further
 * discussion, details, API examples, and FAQ.
 * <p>
 * <strong>Specifications:</strong>
 * <p>
 * The table below compares and contrasts this class to the {@link Config} class from Typesafe Config. <pre>
 * <table border="1" cellpadding="5" cellspacing="1">
 *  <tr>
 *      <th>Method</th><th>Property value</th><th>{@link Config} result</th><th>Strict mode</th><th>OptionalConfig result</th>
 *  </tr>
 *  <tr>
 *      <td rowspan=
"6">getXXXX</td><td>Valid value</td><td>Returns the property value</td><td>On/Off</td><td>Returns the property value</td>
 *  </tr>
 *  <tr>
 *      <td>Null value</td><td>Exception</td><td>Off</td><td>Returns null</td>
 *  </tr>
 *  <tr>
 *      <td>Null value</td><td>Exception</td><td>On</td><td>Exception</td>
 *  </tr>
 *  <tr>
 *      <td>Invalid value</td><td>Exception</td><td>On/Off</td><td>Exception</td>
 *  </tr>
 *  <tr>
 *      <td>Missing property</td><td>Exception</td><td>On/Off</td><td>Exception</td>
 *  </tr>
 *  <tr>
 *      <td>Missing value</td><td>Exception</td><td>On/Off</td><td>Exception</td>
 *  </tr>
 *  <tr>
 *      <td rowspan=
"8">getXXXXList</td><td>Valid list (no null values)</td><td>Returns the list of values</td><td>On/Off</td><td>Returns the list of values</td>
 *  </tr>
 *  <tr>
 *      <td>Valid list (contains null values)</td><td>Exception</td><td>Off</td><td>Returns the list of values (including nulls)</td>
 *  </tr>
 *  <tr>
 *      <td>Valid list (contains null values)</td><td>Exception</td><td>On</td><td>Exception</td>
 *  </tr>
 *  <tr>
 *      <td>Null list</td><td>Exception</td><td>Off</td><td>Returns null</td>
 *  </tr>
 *  <tr>
 *      <td>Null list</td><td>Exception</td><td>On</td><td>Exception</td>
 *  </tr>
 *  <tr>
 *      <td>Invalid list</td><td>Exception</td><td>On/Off</td><td>Exception</td>
 *  </tr>
 *  <tr>
 *      <td>Missing property</td><td>Exception</td><td>On/Off</td><td>Exception</td>
 *  </tr>
 *  <tr>
 *      <td>Missing value</td><td>Exception</td><td>On/Off</td><td>Exception</td>
 *  </tr>
 *  <tr>
 *      <td rowspan=
"5">getOptionalXXXX</td><td>Valid value</td><td>N/A</td><td>On/Off</td><td>Returns an {@code Optional} containing the value</td>
 *  </tr>
 *  <tr>
 *      <td>Null value</td><td>N/A</td><td>On/Off</td><td>Returns {@link Optional#empty()}</td>
 *  </tr>
 *  <tr>
 *      <td>Missing property</td><td>N/A</td><td>On/Off</td><td>Returns {@link Optional#empty()}</td>
 *  </tr>
 *  <tr>
 *      <td>Invalid value</td><td>N/A</td><td>On/Off</td><td>Exception</td>
 *  </tr>
 *  <tr>
 *      <td>Missing value</td><td>N/A</td><td>On/Off</td><td>Exception</td>
 *  </tr>  
 *  <tr>
 *      <td rowspan=
"7">getOptionalXXXXList</td><td>Valid list (no null values)</td><td>N/A</td><td>On/Off</td><td>Returns an Optional containing the list of values</td>
 *  </tr>
 *  <tr>
 *      <td>Valid list (contains null values)</td><td>N/A</td><td>Off</td><td>Returns an Optional containing the list of values (including nulls)</td>
 *  </tr>
 *  <tr>
 *      <td>Valid list (contains null values)</td><td>N/A</td><td>On</td><td>Exception</td>
 *  </tr>
 *  <tr>
 *      <td>Null list</td><td>N/A</td><td>On/Off</td><td>Returns {@link Optional#empty()}</td>
 *  </tr>
 *  <tr>
 *      <td>Missing property</td><td>N/A</td><td>On/Off</td><td>Returns {@link Optional#empty()}</td>
 *  </tr>
 *  <tr>
 *      <td>Invalid list</td><td>N/A</td><td>On/Off</td><td>Exception</td>
 *  </tr>
 *  <tr>
 *      <td>Missing value</td><td>N/A</td><td>On/Off</td><td>Exception</td>
 *  </tr>
 * </table>
 * </pre> Note that the {@code Config.getEnumList} method is re-implemented as {@code OptionalConfig.getEnumSet} method.
 * <p>
 * <strong>Additional functionality:</strong>
 * <p>
 * This class offers a handful of additional methods which have no equivalent functionality in the {@link Config} class,
 * described below.
 * 
 * <pre>
 * <table border="1" cellpadding="5" cellspacing="1">
 *  <tr>
 *      <th>Method</th><th>Functionality</th>
 *  </tr>
 *  <tr>
 *      <td>{@link #getShort(String)}</td><td rowspan=
"4">Offering methods which operate on properties referencing {@code Short} values (for completeness).<br>These methods behave similarly to their Integer and Long counterparts.</td>
 *  </tr>
 *  <tr>
 *      <td>{@link #getOptionalShort(String)}</td>
 *  </tr>
 *  <tr>
 *      <td>{@link #getShortList(String)}</td>
 *  </tr>
 *  <tr>
 *      <td>{@link #getOptionalShortList(String)}</td>
 *  </tr>
 *  <tr>
 *      <td>{@link #getPath(String)}</td><td rowspan=
"4">Offering methods which operate on properties referencing {@code Path} values.<br>A valid {@code java.nio.file.Path} may refer to an existing file or a directory or may not exist on the filesystem.</td>
 *  </tr>
 *  <tr>
 *      <td>{@link #getOptionalPath(String)}</td>
 *  </tr>
 *  <tr>
 *      <td>{@link #getPathList(String)}</td>
 *  </tr>
 *  <tr>
 *      <td>{@link #getOptionalPathList(String)}</td>
 *  </tr>
 *  <tr>
 *      <td>{@link #getFile(String, LinkOption...)}</td><td rowspan=
"4">Offering methods which operate on properties referencing {@code Path} values.<br>A valid {@code java.nio.file.Path} may refer to an existing file or a directory or may not exist on the filesystem.</td>
 *  </tr>
 *  <tr>
 *      <td>{@link #getOptionalFile(String, LinkOption...)}</td>
 *  </tr>
 *  <tr>
 *      <td>{@link #getFileList(String, LinkOption...)}</td>
 *  </tr>
 *  <tr>
 *      <td>{@link #getOptionalFileList(String, LinkOption...)}</td>
 *  </tr>
 *  <tr>
 *      <td>{@link #getDirectory(String, LinkOption...)}</td><td rowspan=
"4">Offering methods which operate on properties referencing existing directories.<br>These methods will throw exceptions if the referenced directories do not exist on the filesystem.</td>
 *  </tr>
 *  <tr>
 *      <td>{@link #getOptionalDirectory(String, LinkOption...)}</td>
 *  </tr>
 *  <tr>
 *      <td>{@link #getDirectoryList(String, LinkOption...)}</td>
 *  </tr>
 *  <tr>
 *      <td>{@link #getOptionalDirectoryList(String, LinkOption...)}</td>
 *  </tr>
 *  <tr>
 *      <td>{@link #getConfig(String)}</td><td>Offering the capability to get nested configuration objects as a standalone OptionalConfig.</td>
 *  </tr>
 * </table>
 * </pre>
 * 
 * @author Zhenya Leonov
 */
public final class OptionalConfig {

    private final Config  config;
    private final boolean strictMode;

    private OptionalConfig(final Config config, final boolean strictMode) {
        this.config     = config;
        this.strictMode = strictMode;
    }

    /**
     * Creates a new {@link OptionalConfig} from the underlying {@link Config} object.
     * 
     * @param config the underlying {@link Config} object
     * @return a new {@link OptionalConfig} from the underlying {@link Config} object
     */
    public static OptionalConfig from(final Config config) {
        requireNonNull(config, "config == null");
        return new OptionalConfig(config, true);
    }

    /**
     * Returns the original {@link Config} object used to instantiate this {@link OptionalConfig}.
     * 
     * @return the original {@link Config} object used to instantiate this {@link OptionalConfig}
     */
    public Config getDelegate() {
        return config;
    }

    /**
     * Returns an {@code OptionalConfig} representing the nested configuration object at the specified {@code path}
     * expression.
     * <p>
     * For example given the following configuration: <pre>
     * {
     *     child {
     *         prop1 = abc
     *         prop2 = true
     *     }
     *     
     *     prop3 = 5
     *     
     *     sibling {
     *         prop4 = xyz
     *     }
     * }
     * </pre> the call to {@code OptionalConfig.getConfig("child")} will return the following nested configuration: <pre>
     * {
     *     prop1 = abc
     *     prop2 = true
     * }
     * </pre> such that {@code parent.getString("child.prop1").equals(parent.getConfig("child").getString("prop1"))}.
     * 
     * @param path the path expression
     * @return the nested {@code OptionalConfig} value at the requested {@code path}
     * @throws com.typesafe.config.ConfigException.Missing   if property is absent or set to {@code null}
     * @throws com.typesafe.config.ConfigException.WrongType if property value does not reference a nested configuration
     *                                                       type
     */
    public OptionalConfig getConfig(final String path) {
        requireNonNull(path, "path == null");
        final Config config = get(OBJECT, path, value -> toConfig(value, path));
        return config == null ? null : new OptionalConfig(config, strictMode);
    }

    /**
     * Returns an {@code OptionalConfig} representing the {@link Optional optional} nested configuration object at the
     * specified {@code path} expression.
     * <p>
     * For example given the following configuration: <pre>
     * {
     *     child {
     *         prop1 = abc
     *         prop2 = true
     *     }
     *     
     *     prop3 = 5
     *     
     *     sibling {
     *         prop4 = xyz
     *     }
     * }
     * </pre> the call to {@code OptionalConfig.getConfig("child")} will return the following nested configuration: <pre>
     * {
     *     prop1 = abc
     *     prop2 = true
     * }
     * </pre> such that {@code parent.getString("child.prop1").equals(parent.getConfig("child").getString("prop1"))}.
     * 
     * @param path the path expression
     * @return an {@code OptionalConfig} representing the {@link Optional optional} nested configuration object at the
     *         specified {@code path}
     * @throws com.typesafe.config.ConfigException.Missing   if property is absent or set to {@code null}
     * @throws com.typesafe.config.ConfigException.WrongType if property value does not reference a nested configuration
     *                                                       type
     */
    public Optional<OptionalConfig> getOptionalConfig(final String path) {
        requireNonNull(path, "path == null");
        final Optional<Config> config = getOptional(OBJECT, path, value -> toConfig(value, path));
        return config.map(cfg -> new OptionalConfig(cfg, strictMode));
    }

    /**
     * Returns a copy of this {@link OptionalConfig} with the specified strict-mode setting. When strict-mode is enabled
     * properties set to {@code null} are treated as {@link com.typesafe.config.ConfigException.Missing absent} for the
     * purpose of {@code getXXXX} methods.
     * <p>
     * By default strict-mode is enabled.
     * 
     * @param strictMode the specified strict-mode setting
     * @return a copy of this {@link OptionalConfig} with the specified strict-mode setting
     */
    public OptionalConfig setStrictMode(final boolean strictMode) {
        return this.strictMode == strictMode ? this : new OptionalConfig(config, strictMode);
    }

    /**
     * Returns whether or not strict-mode is enabled. When strict-mode is enabled properties set to {@code null} are treated
     * as {@link com.typesafe.config.ConfigException.Missing absent} purpose of {@code getXXXX} methods.
     * 
     * @return whether or not strict-mode is enabled
     */
    public boolean isStrictMode() {
        return strictMode;
    }

    /*** String ***/

    /**
     * Returns a {@code String} value for the property at the given {@code path} expression or {@code null} if the property
     * is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled.
     * 
     * @param path the path expression
     * @return a {@code String} value for the property at the given {@code path} expression or {@code null} if the property
     *         is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled
     * @throws com.typesafe.config.ConfigException.Missing if the property is missing
     * @throws com.typesafe.config.ConfigException.Null    if the property is set to {@code null} and
     *                                                     {@link #setStrictMode(boolean) strict-mode} is enabled
     */
    public String getString(final String path) {
        requireNonNull(path, "path == null");
        return get(STRING, path);
    }

    /**
     * Returns a {@code String} value for the {@link Optional optional} property at the given {@code path} expression or an
     * {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to {@code null}. Note that
     * {@link #isStrictMode() strict-mode} has no effect on optional properties.
     * 
     * @param path the path expression
     * @return a {@code String} value for the {@link Optional optional} property at the given {@code path} expression or an
     *         {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to {@code null}
     */
    public Optional<String> getOptionalString(final String path) {
        requireNonNull(path, "path == null");
        return getOptional(STRING, path);
    }

    /*** String List ***/

    /**
     * Returns a {@code List} of {@code String} values for the property at the given {@code path} expression or {@code null}
     * if the property is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled. Similarly, if
     * strict-mode is disabled, individual values within the list may be set to {@code null}.
     * 
     * @param path the path expression
     * @return a {@code List} of {@code String} values for the property at the given {@code path} expression or {@code null}
     *         if the property is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled
     * @throws com.typesafe.config.ConfigException.Missing if the property is missing
     * @throws com.typesafe.config.ConfigException.Null    if the property is set to {@code null} and
     *                                                     {@link #setStrictMode(boolean) strict-mode} is enabled
     */
    public List<String> getStringList(final String path) {
        requireNonNull(path, "path == null");
        return getList(STRING, path);
    }

    /**
     * Returns a {@code List} of {@code String} values for the {@link Optional optional} property at the given {@code path}
     * expression or an {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to
     * {@code null}. Note that {@link #isStrictMode() strict-mode} has no effect on optional properties, but if strict-mode
     * is disabled, individual values within the list may be set to {@code null}.
     * 
     * @param path the path expression
     * @return a {@code List} of {@code String} values for the {@link Optional optional} property at the given {@code path}
     *         expression or an {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to
     *         {@code null}
     */
    public Optional<List<String>> getOptionalStringList(final String path) {
        requireNonNull(path, "path == null");
        return Optional.ofNullable(getNullableList(STRING, path));
    }

    /*** Short ***/

    /**
     * Returns a {@code Short} value for the property at the given {@code path} expression or {@code null} if the property
     * is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled.
     * 
     * @param path the path expression
     * @return a {@code Short} value for the property at the given {@code path} expression or {@code null} if the property
     *         is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled
     * @throws com.typesafe.config.ConfigException.Missing   if the property is missing
     * @throws com.typesafe.config.ConfigException.Null      if the property is set to {@code null} and
     *                                                       {@link #setStrictMode(boolean) strict-mode} is enabled
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot be converted to a {@code Short}
     * @throws com.typesafe.config.ConfigException.WrongType if the property value is less than {@link Short#MIN_VALUE} or
     *                                                       more than {@link Short#MAX_VALUE}
     */
    public Short getShort(final String path) {
        requireNonNull(path, "path == null");
        return get(NUMBER, path, value -> toShort(value, path));
    }

    /**
     * Returns a {@code Short} value for the {@link Optional optional} property at the given {@code path} expression or an
     * {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to {@code null}. Note that
     * {@link #isStrictMode() strict-mode} has no effect on optional properties.
     * 
     * @param path the path expression
     * @return a {@code Short} value for the {@link Optional optional} property at the given {@code path} expression or an
     *         {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to {@code null}
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot be converted to a {@code Short}
     * @throws com.typesafe.config.ConfigException.WrongType if the property value is less than {@link Short#MIN_VALUE} or
     *                                                       more than {@link Short#MAX_VALUE}
     */
    public Optional<Short> getOptionalShort(final String path) {
        requireNonNull(path, "path == null");
        return getOptional(NUMBER, path, value -> toShort(value, path));
    }

    /*** Short List ***/

    /**
     * Returns a {@code List} of {@code Short} values for the property at the given {@code path} expression or {@code null}
     * if the property is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled. Similarly, if
     * strict-mode is disabled, individual values within the list may be set to {@code null}.
     * 
     * @param path the path expression
     * @return a {@code List} of {@code Short} values for the property at the given {@code path} expression or {@code null}
     *         if the property is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled
     * @throws com.typesafe.config.ConfigException.Missing   if the property is missing
     * @throws com.typesafe.config.ConfigException.Null      if the property is set to {@code null} and
     *                                                       {@link #setStrictMode(boolean) strict-mode} is enabled
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot be converted to a {@code List} of
     *                                                       {@code Short}s
     * @throws com.typesafe.config.ConfigException.WrongType if a value in the list value is less than
     *                                                       {@link Short#MIN_VALUE} or more than {@link Short#MAX_VALUE}
     */
    public List<Short> getShortList(final String path) {
        requireNonNull(path, "path == null");
        return getList(NUMBER, path, value -> toShort(value, path));
    }

    /**
     * Returns a {@code List} of {@code Short} values for the {@link Optional optional} property at the given {@code path}
     * expression or an {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to
     * {@code null}. Note that {@link #isStrictMode() strict-mode} has no effect on optional properties, but if strict-mode
     * is disabled, individual values within the list may be set to {@code null}.
     * 
     * @param path the path expression
     * @return a {@code List} of {@code Short} values for the {@link Optional optional} property at the given {@code path}
     *         expression or an {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to
     *         {@code null}
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot be converted to a {@code List} of
     *                                                       {@code Short}s
     * @throws com.typesafe.config.ConfigException.WrongType if a value in the list value is less than
     *                                                       {@link Short#MIN_VALUE} or more than {@link Short#MAX_VALUE}
     */
    public Optional<List<Short>> getOptionalShortList(final String path) {
        requireNonNull(path, "path == null");
        return Optional.ofNullable(getNullableList(NUMBER, path, value -> toShort(value, path)));
    }

    /*** Integer ***/

    /**
     * Returns an {@code Integer} value for the property at the given {@code path} expression or {@code null} if the
     * property is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled.
     * 
     * @param path the path expression
     * @return an {@code Integer} value for the property at the given {@code path} expression or {@code null} if the
     *         property is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled
     * @throws com.typesafe.config.ConfigException.Missing   if the property is missing
     * @throws com.typesafe.config.ConfigException.Null      if the property is set to {@code null} and
     *                                                       {@link #setStrictMode(boolean) strict-mode} is enabled
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot be converted to an {@code Integer}
     * @throws com.typesafe.config.ConfigException.WrongType if the property value is less than {@link Integer#MIN_VALUE} or
     *                                                       more than {@link Integer#MAX_VALUE}
     */
    public Integer getInteger(final String path) {
        requireNonNull(path, "path == null");
        return get(NUMBER, path, value -> toInteger(value, path));
    }

    /**
     * Returns an {@code Integer} value for the {@link Optional optional} property at the given {@code path} expression or
     * an {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to {@code null}. Note that
     * {@link #isStrictMode() strict-mode} has no effect on optional properties.
     * 
     * @param path the path expression
     * @return an {@code Integer} value for the {@link Optional optional} property at the given {@code path} expression or
     *         an {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to {@code null}
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot be converted to an {@code Integer}
     * @throws com.typesafe.config.ConfigException.WrongType if the property value is less than {@link Integer#MIN_VALUE} or
     *                                                       more than {@link Integer#MAX_VALUE}
     */
    public Optional<Integer> getOptionalInteger(final String path) {
        requireNonNull(path, "path == null");
        return getOptional(NUMBER, path, value -> toInteger(value, path));
    }

    /*** Integer List ***/

    /**
     * Returns a {@code List} of {@code Integer} values for the property at the given {@code path} expression or
     * {@code null} if the property is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled.
     * Similarly, if strict-mode is disabled, individual values within the list may be set to {@code null}.
     * 
     * @param path the path expression
     * @return a {@code List} of {@code Integer} values for the property at the given {@code path} expression or
     *         {@code null} if the property is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is
     *         disabled
     * @throws com.typesafe.config.ConfigException.Missing   if the property is missing
     * @throws com.typesafe.config.ConfigException.Null      if the property is set to {@code null} and
     *                                                       {@link #setStrictMode(boolean) strict-mode} is enabled
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot be converted to a {@code List} of
     *                                                       {@code Integer}s
     * @throws com.typesafe.config.ConfigException.WrongType if a value in the list value is less than
     *                                                       {@link Integer#MIN_VALUE} or more than
     *                                                       {@link Integer#MAX_VALUE}
     */
    public List<Integer> getIntegerList(final String path) {
        requireNonNull(path, "path == null");
        return getList(NUMBER, path, value -> toInteger(value, path));
    }

    /**
     * Returns a {@code List} of {@code Integer} values for the {@link Optional optional} property at the given {@code path}
     * expression or an {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to
     * {@code null}. Note that {@link #isStrictMode() strict-mode} has no effect on optional properties, but if strict-mode
     * is disabled, individual values within the list may be set to {@code null}.
     * 
     * @param path the path expression
     * @return a {@code List} of {@code Integer} values for the {@link Optional optional} property at the given {@code path}
     *         expression or an {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to
     *         {@code null}
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot be converted to a {@code List} of
     *                                                       {@code Integer}s
     * @throws com.typesafe.config.ConfigException.WrongType if a value in the list value is less than
     *                                                       {@link Integer#MIN_VALUE} or more than
     *                                                       {@link Integer#MAX_VALUE}
     */
    public Optional<List<Integer>> getOptionalIntegerList(final String path) {
        requireNonNull(path, "path == null");
        return Optional.ofNullable(getNullableList(NUMBER, path, value -> toInteger(value, path)));
    }

    /*** Long ***/

    /**
     * Returns a {@code Long} value for the property at the given {@code path} expression or {@code null} if the property is
     * set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled. If the value overflows or underflows
     * {@link Long#MAX_VALUE} or {@link Long#MIN_VALUE} will be returned respectively.
     * 
     * @param path the path expression
     * @return a {@code Long} value for the property at the given {@code path} expression or {@code null} if the property is
     *         set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled
     * @throws com.typesafe.config.ConfigException.Missing   if the property is missing
     * @throws com.typesafe.config.ConfigException.Null      if the property is set to {@code null} and
     *                                                       {@link #setStrictMode(boolean) strict-mode} is enabled
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot be converted to a {@code Long}
     */
    public Long getLong(final String path) {
        requireNonNull(path, "path == null");
        return get(NUMBER, path, value -> toLong(value, path));
    }

    /**
     * Returns a {@code Long} value for the {@link Optional optional} property at the given {@code path} expression or an
     * {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to {@code null}. If the value
     * overflows or underflows {@link Long#MAX_VALUE} or {@link Long#MIN_VALUE} will be returned respectively. Note that
     * {@link #isStrictMode() strict-mode} has no effect on optional properties.
     * 
     * @param path the path expression
     * @return a {@code Long} value for the {@link Optional optional} property at the given {@code path} expression or an
     *         {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to {@code null}
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot be converted to a {@code Long}
     */
    public Optional<Long> getOptionalLong(final String path) {
        requireNonNull(path, "path == null");
        return getOptional(NUMBER, path, value -> toLong(value, path));
    }

    /*** Long List ***/

    /**
     * Returns a {@code List} of {@code Long} values for the property at the given {@code path} expression or {@code null}
     * if the property is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled. Similarly, if
     * strict-mode is disabled, individual values within the list may be set to {@code null}. An overflow or underflow for
     * any value in the list will result in {@link Long#MAX_VALUE} or {@link Long#MIN_VALUE} respectively.
     * 
     * @param path the path expression
     * @return a {@code List} of {@code Long} values for the property at the given {@code path} expression or {@code null}
     *         if the property is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled
     * @throws com.typesafe.config.ConfigException.Missing   if the property is missing
     * @throws com.typesafe.config.ConfigException.Null      if the property is set to {@code null} and
     *                                                       {@link #setStrictMode(boolean) strict-mode} is enabled
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot be converted to a {@code List} of
     *                                                       {@code Long}s
     */
    public List<Long> getLongList(final String path) {
        requireNonNull(path, "path == null");
        return getList(NUMBER, path, value -> toLong(value, path));
    }

    /**
     * Returns a {@code List} of {@code Long} values for the {@link Optional optional} property at the given {@code path}
     * expression or an {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to
     * {@code null}. An overflow or underflow for any value in the list will result in {@link Long#MAX_VALUE} or
     * {@link Long#MIN_VALUE} respectively. Note that {@link #isStrictMode() strict-mode} has no effect on optional
     * properties, but if strict-mode is disabled, individual values within the list may be set to {@code null}.
     * 
     * @param path the path expression
     * @return a {@code List} of {@code Long} values for the {@link Optional optional} property at the given {@code path}
     *         expression or an {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to
     *         {@code null}
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot be converted to a {@code List} of
     *                                                       {@code Long}s
     */
    public Optional<List<Long>> getOptionalLongList(final String path) {
        requireNonNull(path, "path == null");
        return Optional.ofNullable(getNullableList(NUMBER, path, value -> toLong(value, path)));
    }

    /*** Double ***/

    /**
     * Returns a {@code Double} value for the property at the given {@code path} expression or {@code null} if the property
     * is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled.
     * 
     * @param path the path expression
     * @return a {@code Double} value for the property at the given {@code path} expression or {@code null} if the property
     *         is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled
     * @throws com.typesafe.config.ConfigException.Missing   if the property is missing
     * @throws com.typesafe.config.ConfigException.Null      if the property is set to {@code null} and
     *                                                       {@link #setStrictMode(boolean) strict-mode} is enabled
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot be converted to a {@code Double}
     */
    public Double getDouble(final String path) {
        requireNonNull(path, "path == null");
        return get(NUMBER, path, value -> toDouble(value, path));
    }

    /**
     * Returns a {@code Double} value for the {@link Optional optional} property at the given {@code path} expression or an
     * {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to {@code null}. Note that
     * {@link #isStrictMode() strict-mode} has no effect on optional properties.
     * 
     * @param path the path expression
     * @return a {@code Double} value for the {@link Optional optional} property at the given {@code path} expression or an
     *         {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to {@code null}
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot be converted to a {@code Double}
     */
    public Optional<Double> getOptionalDouble(final String path) {
        requireNonNull(path, "path == null");
        return getOptional(NUMBER, path, value -> toDouble(value, path));
    }

    /*** Double List ***/

    /**
     * Returns a {@code List} of {@code Double} values for the property at the given {@code path} expression or {@code null}
     * if the property is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled. Similarly, if
     * strict-mode is disabled, individual values within the list may be set to {@code null}.
     * 
     * @param path the path expression
     * @return a {@code List} of {@code Double} values for the property at the given {@code path} expression or {@code null}
     *         if the property is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled
     * @throws com.typesafe.config.ConfigException.Missing   if the property is missing
     * @throws com.typesafe.config.ConfigException.Null      if the property is set to {@code null} and
     *                                                       {@link #setStrictMode(boolean) strict-mode} is enabled
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot be converted to a {@code List} of
     *                                                       {@code Double}s
     */
    public List<Double> getDoubleList(final String path) {
        requireNonNull(path, "path == null");
        return getList(NUMBER, path, value -> toDouble(value, path));
    }

    /**
     * Returns a {@code List} of {@code Double} values for the {@link Optional optional} property at the given {@code path}
     * expression or an {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to
     * {@code null}. Note that {@link #isStrictMode() strict-mode} has no effect on optional properties, but if strict-mode
     * is disabled, individual values within the list may be set to {@code null}.
     * 
     * @param path the path expression
     * @return a {@code List} of {@code Double} values for the {@link Optional optional} property at the given {@code path}
     *         expression or an {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to
     *         {@code null}
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot be converted to a {@code List} of
     *                                                       {@code Double}s
     */
    public Optional<List<Double>> getOptionalDoubleList(final String path) {
        requireNonNull(path, "path == null");
        return Optional.ofNullable(getNullableList(NUMBER, path, value -> toDouble(value, path)));
    }

    /*** Boolean ***/

    /**
     * Returns a {@code Boolean} value for the property at the given {@code path} expression or {@code null} if the property
     * is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled.
     * 
     * @param path the path expression
     * @return a {@code Boolean} value for the property at the given {@code path} expression or {@code null} if the property
     *         is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled
     * @throws com.typesafe.config.ConfigException.Missing   if the property is missing
     * @throws com.typesafe.config.ConfigException.Null      if the property is set to {@code null} and
     *                                                       {@link #setStrictMode(boolean) strict-mode} is enabled
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot be converted to a {@code Boolean}
     */
    public Boolean getBoolean(final String path) {
        requireNonNull(path, "path == null");
        return get(BOOLEAN, path);
    }

    /**
     * Returns a {@code Boolean} value for the {@link Optional optional} property at the given {@code path} expression or an
     * {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to {@code null}. Note that
     * {@link #isStrictMode() strict-mode} has no effect on optional properties.
     * 
     * @param path the path expression
     * @return a {@code Boolean} value for the {@link Optional optional} property at the given {@code path} expression or an
     *         {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to {@code null}
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot be converted to a {@code Boolean}
     */
    public Optional<Boolean> getOptionalBoolean(final String path) {
        requireNonNull(path, "path == null");
        return getOptional(BOOLEAN, path);
    }

    /*** Boolean List ***/

    /**
     * Returns a {@code List} of {@code Boolean} values for the property at the given {@code path} expression or
     * {@code null} if the property is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled.
     * Similarly, if strict-mode is disabled, individual values within the list may be set to {@code null}.
     * 
     * @param path the path expression
     * @return a {@code List} of {@code Boolean} values for the property at the given {@code path} expression or
     *         {@code null} if the property is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is
     *         disabled
     * @throws com.typesafe.config.ConfigException.Missing   if the property is missing
     * @throws com.typesafe.config.ConfigException.Null      if the property is set to {@code null} and
     *                                                       {@link #setStrictMode(boolean) strict-mode} is enabled
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot be converted to a {@code List} of
     *                                                       {@code Boolean}s
     */
    public List<Boolean> getBooleanList(final String path) {
        requireNonNull(path, "path == null");
        return getList(BOOLEAN, path);
    }

    /**
     * Returns a {@code List} of {@code Boolean} values for the {@link Optional optional} property at the given {@code path}
     * expression or an {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to
     * {@code null}. Note that {@link #isStrictMode() strict-mode} has no effect on optional properties, but if strict-mode
     * is disabled, individual values within the list may be set to {@code null}.
     * 
     * @param path the path expression
     * @return a {@code List} of {@code Boolean} values for the {@link Optional optional} property at the given {@code path}
     *         expression or an {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to
     *         {@code null}
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot be converted to a {@code List} of
     *                                                       {@code Boolean}s
     */
    public Optional<List<Boolean>> getOptionalBooleanList(final String path) {
        requireNonNull(path, "path == null");
        return Optional.ofNullable(getNullableList(BOOLEAN, path));
    }

    /*** Paths ***/

    /**
     * Returns a {@link java.nio.file.Path} for the property at the given {@code path} expression or {@code null} if the
     * property is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled.
     * 
     * @param path the path expression
     * @return a {@link java.nio.file.Path} for the property at the given {@code path} expression or {@code null} if the
     *         property is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled
     * @throws com.typesafe.config.ConfigException.Missing   if the property is missing
     * @throws com.typesafe.config.ConfigException.Null      if the property is set to {@code null} and
     *                                                       {@link #setStrictMode(boolean) strict-mode} is enabled
     * @throws com.typesafe.config.ConfigException.BadValue  if the property value cannot be converted into a {@link Path}
     *                                                       because it contains invalid characters or for other file system
     *                                                       specific reasons
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot otherwise be converted to a
     *                                                       {@code Path}
     */
    public Path getPath(final String path) {
        requireNonNull(path, "path == null");
        return get(STRING, path, value -> toPath(value, path));
    }

    /**
     * Returns a {@link java.nio.file.Path} for the {@link Optional optional} property at the given {@code path} expression
     * or an {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to {@code null}. Note
     * that {@link #isStrictMode() strict-mode} has no effect on optional properties.
     * 
     * @param path the path expression
     * @return a {@link java.nio.file.Path} for the {@link Optional optional} property at the given {@code path} expression
     *         or an {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to {@code null}
     * @throws com.typesafe.config.ConfigException.BadValue  if the property value cannot be converted into a {@link Path}
     *                                                       because it contains invalid characters or for other file system
     *                                                       specific reasons
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot otherwise be converted to a
     *                                                       {@code Path}
     */
    public Optional<Path> getOptionalPath(final String path) {
        requireNonNull(path, "path == null");
        return getOptional(STRING, path, value -> toPath(value, path));
    }

    /**
     * Returns an {@link Files#isRegularFile(Path, LinkOption...) existing} file {@link Path path} for the property at the
     * given {@code path} expression or {@code null} if the property is set to {@code null} and
     * {@link #setStrictMode(boolean) strict-mode} is disabled.
     * 
     * @param path    the path expression
     * @param options the options indicating how symbolic links are handled
     * @return an {@link Files#isRegularFile(Path, LinkOption...) existing} file {@link Path path} for the property at the
     *         given {@code path} expression or {@code null} if the property is set to {@code null} and
     *         {@link #setStrictMode(boolean) strict-mode} is disabled
     * @throws com.typesafe.config.ConfigException.Missing   if the property is missing
     * @throws com.typesafe.config.ConfigException.Null      if the property is set to {@code null} and
     *                                                       {@link #setStrictMode(boolean) strict-mode} is enabled
     * @throws com.typesafe.config.ConfigException.BadValue  if the property value does not reference an
     *                                                       {@link Files#isRegularFile(Path, LinkOption...) existing} file,
     *                                                       or if it cannot be converted into a {@link Path} because it
     *                                                       contains invalid characters or for other file system specific
     *                                                       reasons
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot otherwise be converted to a
     *                                                       {@code Path}
     */
    public Path getFile(final String path, final LinkOption... options) {
        requireNonNull(path, "path == null");
        requireNonNull(options, "options == null");
        return get(STRING, path, value -> toRegularFile(value, path, options));
    }

    /**
     * Returns an {@link Files#isRegularFile(Path, LinkOption...) existing} file {@link Path path} for the {@link Optional
     * optional} property at the given {@code path} expression or an {@link Optional#empty empty} {@code Optional} instance
     * if the property is absent or set to {@code null}. Note that {@link #isStrictMode() strict-mode} has no effect on
     * optional properties.
     * 
     * @param path    the path expression
     * @param options the options indicating how symbolic links are handled
     * @return an {@link Files#isRegularFile(Path, LinkOption...) existing} file {@link Path path} for the {@link Optional
     *         optional} property at the given {@code path} expression or an {@link Optional#empty empty} {@code Optional}
     *         instance if the property is absent or set to {@code null}
     * @throws com.typesafe.config.ConfigException.BadValue  if the property value does not reference an
     *                                                       {@link Files#isRegularFile(Path, LinkOption...) existing} file,
     *                                                       or if it cannot be converted into a {@link Path} because it
     *                                                       contains invalid characters or for other file system specific
     *                                                       reasons
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot otherwise be converted to a
     *                                                       {@code Path}
     */
    public Optional<Path> getOptionalFile(final String path, final LinkOption... options) {
        requireNonNull(path, "path == null");
        requireNonNull(options, "options == null");
        return getOptional(STRING, path, value -> toRegularFile(value, path, options));
    }

    /**
     * Returns an {@link Files#isDirectory(Path, LinkOption...) existing} directory {@link Path path} for the property at
     * the given {@code path} expression or {@code null} if the property is set to {@code null} and
     * {@link #setStrictMode(boolean) strict-mode} is disabled.
     * 
     * @param path    the path expression
     * @param options the options indicating how symbolic links are handled
     * @return an {@link Files#isDirectory(Path, LinkOption...) existing} directory {@link Path path} for the property at
     *         the given {@code path} expression or {@code null} if the property is set to {@code null} and
     *         {@link #setStrictMode(boolean) strict-mode} is disabled
     * @throws com.typesafe.config.ConfigException.Missing   if the property is missing
     * @throws com.typesafe.config.ConfigException.Null      if the property is set to {@code null} and
     *                                                       {@link #setStrictMode(boolean) strict-mode} is enabled
     * @throws com.typesafe.config.ConfigException.BadValue  if the property value does not reference an
     *                                                       {@link Files#isDirectory(Path, LinkOption...) existing}
     *                                                       directory, or if it cannot be converted into a {@link Path}
     *                                                       because it contains invalid characters or for other file system
     *                                                       specific reasons
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot otherwise be converted to a
     *                                                       {@code Path}
     */
    public Path getDirectory(final String path, final LinkOption... options) {
        requireNonNull(path, "path == null");
        requireNonNull(options, "options == null");
        return get(STRING, path, value -> toDirectory(value, path, options));
    }

    /**
     * Returns an {@link Files#isDirectory(Path, LinkOption...) existing} directory {@link Path path} for the
     * {@link Optional optional} property at the given {@code path} expression or an {@link Optional#empty empty}
     * {@code Optional} instance if the property is absent or set to {@code null}. Note that {@link #isStrictMode()
     * strict-mode} has no effect on optional properties.
     * 
     * @param path    the path expression
     * @param options the options indicating how symbolic links are handled
     * @return an {@link Files#isDirectory(Path, LinkOption...) existing} directory {@link Path path} for the
     *         {@link Optional optional} property at the given {@code path} expression or an {@link Optional#empty empty}
     *         {@code Optional} instance if the property is absent or set to {@code null}
     * @throws com.typesafe.config.ConfigException.BadValue  if the property value does not reference an
     *                                                       {@link Files#isDirectory(Path, LinkOption...) existing}
     *                                                       directory, or if it cannot be converted into a {@link Path}
     *                                                       because it contains invalid characters or for other file system
     *                                                       specific reasons
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot otherwise be converted to a
     *                                                       {@code Path}
     */
    public Optional<Path> getOptionalDirectory(final String path, final LinkOption... options) {
        requireNonNull(path, "path == null");
        requireNonNull(options, "options == null");
        return getOptional(STRING, path, value -> toDirectory(value, path, options));
    }

    /*** Paths List ***/

    /**
     * Returns a {@code List} of {@link java.nio.file.Path} values for the property at the given {@code path} expression or
     * {@code null} if the property is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled.
     * Similarly, if strict-mode is disabled, individual values within the list may be set to {@code null}.
     * 
     * @param path the path expression
     * @return a {@code Set} of {@code Enum} values for the property at the given {@code path} expression or {@code null} if
     *         the property is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled
     * @throws com.typesafe.config.ConfigException.Missing   if the property is missing
     * @throws com.typesafe.config.ConfigException.Null      if the property is set to {@code null} and
     *                                                       {@link #setStrictMode(boolean) strict-mode} is enabled
     * @throws com.typesafe.config.ConfigException.BadValue  if one or more of the list elements cannot be converted into a
     *                                                       {@link Path} because it contains invalid characters, or for
     *                                                       other file system specific reasons
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot otherwise be converted to a
     *                                                       {@code List} of {@code Path}s
     */
    public List<Path> getPathList(final String path) {
        requireNonNull(path, "path == null");
        return getList(STRING, path, value -> toPath(value, path));
    }

    /**
     * Returns a {@code List} of {@link java.nio.file.Path} values for the {@link Optional optional} property at the given
     * {@code path} expression or an {@link Optional#empty empty} {@code Optional} instance if the property is absent or set
     * to {@code null}. Note that {@link #isStrictMode() strict-mode} has no effect on optional properties, but if
     * strict-mode is disabled, individual values within the list may be set to {@code null}.
     * 
     * @param path the path expression
     * @return a {@code List} of {@link java.nio.file.Path} values for the {@link Optional optional} property at the given
     *         {@code path} expression or an {@link Optional#empty empty} {@code Optional} instance if the property is
     *         absent or set to {@code null}
     * @throws com.typesafe.config.ConfigException.BadValue  if one or more of the list elements cannot be converted into a
     *                                                       {@link Path} because it contains invalid characters, or for
     *                                                       other file system specific reasons
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot otherwise be converted to a
     *                                                       {@code List} of {@code Path}s
     */
    public Optional<List<Boolean>> getOptionalPathList(final String path) {
        requireNonNull(path, "path == null");
        return Optional.ofNullable(getNullableList(BOOLEAN, path));
    }

    /**
     * Returns a {@code List} of {@link Files#isRegularFile(Path, LinkOption...) existing} file {@link Path path} values for
     * the property at the given {@code path} expression or {@code null} if the property is set to {@code null} and
     * {@link #setStrictMode(boolean) strict-mode} is disabled. Similarly, if strict-mode is disabled, individual values
     * within the list may be set to {@code null}.
     * 
     * @param path    the path expression
     * @param options the options indicating how symbolic links are handled
     * @return a {@code List} of {@link Files#isRegularFile(Path, LinkOption...) existing} file {@link Path path} values for
     *         the property at the given {@code path} expression or {@code null} if the property is set to {@code null} and
     *         {@link #setStrictMode(boolean) strict-mode} is disabled
     * @throws com.typesafe.config.ConfigException.Missing   if the property is missing
     * @throws com.typesafe.config.ConfigException.Null      if the property is set to {@code null} and
     *                                                       {@link #setStrictMode(boolean) strict-mode} is enabled
     * @throws com.typesafe.config.ConfigException.BadValue  if one or more of the list elements does not reference an
     *                                                       {@link Files#isRegularFile(Path, LinkOption...) existing} file,
     *                                                       or cannot be converted into a {@link Path} because it contains
     *                                                       invalid characters or for other file system specific reasons
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot otherwise be converted to a
     *                                                       {@code List} of {@code Path}s
     */
    public List<Path> getFileList(final String path, final LinkOption... options) {
        requireNonNull(path, "path == null");
        requireNonNull(options, "options == null");
        return getList(STRING, path, value -> toRegularFile(value, path, options));
    }

    /**
     * Returns a {@code List} of {@link Files#isRegularFile(Path, LinkOption...) existing} file {@link Path path} values for
     * the {@link Optional optional} property at the given {@code path} expression or an {@link Optional#empty empty}
     * {@code Optional} instance if the property is absent or set to {@code null}. Note that {@link #isStrictMode()
     * strict-mode} has no effect on optional properties, but if strict-mode is disabled, individual values within the list
     * may be set to {@code null}.
     * 
     * @param path    the path expression
     * @param options the options indicating how symbolic links are handled
     * @return a {@code List} of {@link Files#isRegularFile(Path, LinkOption...) existing} file {@link Path path} values for
     *         the {@link Optional optional} property at the given {@code path} expression or an {@link Optional#empty
     *         empty} {@code Optional} instance if the property is absent or set to {@code null}
     * @throws com.typesafe.config.ConfigException.BadValue  if one or more of the list elements does not reference an
     *                                                       {@link Files#isRegularFile(Path, LinkOption...) existing} file,
     *                                                       or it contains invalid characters or for other file system
     *                                                       specific reasons
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot otherwise be converted to a
     *                                                       {@code List} of {@code Path}s
     */
    public Optional<List<Path>> getOptionalFileList(final String path, final LinkOption... options) {
        requireNonNull(path, "path == null");
        requireNonNull(options, "options == null");
        return Optional.ofNullable(getNullableList(STRING, path, value -> toRegularFile(value, path, options)));
    }

    /**
     * Returns a {@code List} of {@link Files#isDirectory(Path, LinkOption...) existing} directory {@link Path path} values
     * for the property at the given {@code path} expression or {@code null} if the property is set to {@code null} and
     * {@link #setStrictMode(boolean) strict-mode} is disabled. Similarly, if strict-mode is disabled, individual values
     * within the list may be set to {@code null}.
     * 
     * @param path    the path expression
     * @param options the options indicating how symbolic links are handled
     * @return a {@code List} of {@link Files#isDirectory(Path, LinkOption...) existing} directory {@link Path path} values
     *         for the property at the given {@code path} expression or {@code null} if the property is set to {@code null}
     *         and {@link #setStrictMode(boolean) strict-mode} is disabled
     * @throws com.typesafe.config.ConfigException.Missing   if the property is missing
     * @throws com.typesafe.config.ConfigException.Null      if the property is set to {@code null} and
     *                                                       {@link #setStrictMode(boolean) strict-mode} is enabled
     * @throws com.typesafe.config.ConfigException.BadValue  if one or more of the list elements does not reference an
     *                                                       {@link Files#isDirectory(Path, LinkOption...) existing}
     *                                                       directory, or cannot be converted into a {@link Path} because
     *                                                       it contains invalid characters or for other file system
     *                                                       specific reasons
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot otherwise be converted to a
     *                                                       {@code List} of {@code Path}s
     */
    public List<Path> getDirectoryList(final String path, final LinkOption... options) {
        requireNonNull(path, "path == null");
        requireNonNull(options, "options == null");
        return getList(STRING, path, value -> toRegularFile(value, path, options));
    }

    /**
     * Returns a {@code List} of {@link Files#isDirectory(Path, LinkOption...) existing} directory {@link Path path} values
     * for the {@link Optional optional} property at the given {@code path} expression or an {@link Optional#empty empty}
     * {@code Optional} instance if the property is absent or set to {@code null}. Note that {@link #isStrictMode()
     * strict-mode} has no effect on optional properties, but if strict-mode is disabled, individual values within the list
     * may be set to {@code null}.
     * 
     * @param path    the path expression
     * @param options the options indicating how symbolic links are handled
     * @return a {@code List} of {@link Files#isDirectory(Path, LinkOption...) existing} directory {@link Path path} values
     *         for the {@link Optional optional} property at the given {@code path} expression or an {@link Optional#empty
     *         empty} {@code Optional} instance if the property is absent or set to {@code null}
     * @throws com.typesafe.config.ConfigException.BadValue  if one or more of the list elements does not reference an
     *                                                       {@link Files#isDirectory(Path, LinkOption...) existing}
     *                                                       directory, or cannot be converted into a {@link Path} because
     *                                                       it contains invalid characters or for other file system
     *                                                       specific reasons
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot otherwise be converted to a
     *                                                       {@code List} of {@code Path}s
     */
    public Optional<List<Path>> getOptionalDirectoryList(final String path, final LinkOption... options) {
        requireNonNull(path, "path == null");
        requireNonNull(options, "options == null");
        return Optional.ofNullable(getNullableList(STRING, path, value -> toRegularFile(value, path, options)));
    }

    /*** Enum ***/

    /**
     * Returns an {@code Enum} value for the property at the given {@code path} expression or {@code null} if the property
     * is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled.
     * 
     * @param <T>  the enum type
     * @param type the {@code Class} of the enum type
     * @param path the path expression
     * @return an {@code Enum} value for the property at the given {@code path} expression or {@code null} if the property
     *         is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled
     * @throws com.typesafe.config.ConfigException.Missing   if the property is missing
     * @throws com.typesafe.config.ConfigException.Null      if the property is set to {@code null} and
     *                                                       {@link #setStrictMode(boolean) strict-mode} is enabled
     * @throws com.typesafe.config.ConfigException.BadValue  if the {@code Enum} type has no constant matching the property
     *                                                       value
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot otherwise be converted to an
     *                                                       {@code Enum}
     */
    public <T extends Enum<T>> T getEnum(final Class<T> type, final String path) {
        requireNonNull(type, "type == null");
        requireNonNull(path, "path == null");
        return get(STRING, path, value -> toEnum(type, value, path));
    }

    /**
     * Returns an {@code Enum} value for the {@link Optional optional} property at the given {@code path} expression or an
     * {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to {@code null}. Note that
     * {@link #isStrictMode() strict-mode} has no effect on optional properties.
     * 
     * @param <T>  the enum type
     * @param type the {@code Class} of the enum type
     * @param path the path expression
     * @return an {@code Enum} value for the {@link Optional optional} property at the given {@code path} expression or an
     *         {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to {@code null}
     * @throws com.typesafe.config.ConfigException.BadValue  if the {@code Enum} type has no constant matching the property
     *                                                       value
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot otherwise be converted to an
     *                                                       {@code Enum}
     */
    public <T extends Enum<T>> Optional<T> getOptionalEnum(final Class<T> type, final String path) {
        requireNonNull(type, "type == null");
        requireNonNull(path, "path == null");
        return getOptional(STRING, path, value -> toEnum(type, value, path));
    }

    /*** Enum Set ***/

    /**
     * Returns a {@code Set} of {@code Enum} values for the property at the given {@code path} expression or {@code null} if
     * the property is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled. Similarly, if
     * strict-mode is disabled, individual values within the list may be set to {@code null}.
     * 
     * @param <T>  the enum type
     * @param type the {@code Class} of the enum type
     * @param path the path expression
     * @return a {@code Set} of {@code Enum} values for the property at the given {@code path} expression or {@code null} if
     *         the property is set to {@code null} and {@link #setStrictMode(boolean) strict-mode} is disabled
     * @throws com.typesafe.config.ConfigException.Missing   if the property is missing
     * @throws com.typesafe.config.ConfigException.Null      if the property is set to {@code null} and
     *                                                       {@link #setStrictMode(boolean) strict-mode} is enabled
     * @throws com.typesafe.config.ConfigException.BadValue  if the {@code Enum} type has no constant matching one or more
     *                                                       of the list elements
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot otherwise be converted to a
     *                                                       {@code Set} of {@code Enum}s
     */
    public <T extends Enum<T>> Set<T> getEnumSet(final Class<T> type, final String path) {
        requireNonNull(type, "type == null");
        requireNonNull(path, "path == null");

        final List<T> list = getList(STRING, path, value -> toEnum(type, value, path));

        return list == null ? null : list.stream().collect(Collectors.toSet());
    }

    /**
     * Returns a {@code Set} of {@code Enum} values for the {@link Optional optional} property at the given {@code path}
     * expression or an {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to
     * {@code null}. Note that {@link #isStrictMode() strict-mode} has no effect on optional properties, but if strict-mode
     * is disabled, individual values within the set may be set to {@code null}.
     * 
     * @param <T>  the enum type
     * @param type the {@code Class} of the enum type
     * @param path the path expression
     * @return a {@code Set} of {@code Enum} values for the {@link Optional optional} property at the given {@code path}
     *         expression or an {@link Optional#empty empty} {@code Optional} instance if the property is absent or set to
     *         {@code null}
     * @throws com.typesafe.config.ConfigException.BadValue  if the {@code Enum} type has no constant matching one or more
     *                                                       of the list elements
     * @throws com.typesafe.config.ConfigException.WrongType if the property value cannot otherwise be converted to a
     *                                                       {@code Set} of {@code Enum}s
     */
    public <T extends Enum<T>> Optional<Set<T>> getOptionalEnumSet(final Class<T> type, final String path) {
        requireNonNull(type, "type == null");
        requireNonNull(path, "path == null");

        final List<T> list = getNullableList(STRING, path, value -> toEnum(type, value, path));

        return list == null ? Optional.empty() : Optional.of(list.stream().collect(Collectors.toSet()));
    }

    /*** Utility methods ***/

    @SuppressWarnings("unchecked")
    private <T> T get(final ConfigValueType valueType, final String path) {
        return (T) get(valueType, path, ConfigValue::unwrapped);
    }

    private <T> T get(final ConfigValueType valueType, final String path, final Function<ConfigValue, T> valueTransformer) {
        return getValue(strictMode, config, path, valueType, valueTransformer);
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<T> getOptional(final ConfigValueType valueType, final String path) {
        return (Optional<T>) getOptional(valueType, path, ConfigValue::unwrapped);
    }

    private <T> Optional<T> getOptional(final ConfigValueType valueType, final String path, final Function<ConfigValue, T> valueTransformer) {
        return Optional.ofNullable(isMissingOrNull(path) ? null : getValue(false, config, path, valueType, valueTransformer));
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getList(final ConfigValueType valueType, final String path) {
        return (List<T>) getList(false, valueType, path, ConfigValue::unwrapped);
    }

    private <T> List<T> getList(final ConfigValueType valueType, final String path, final Function<ConfigValue, T> valueTransformer) {
        return (List<T>) getList(false, valueType, path, valueTransformer);
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getNullableList(final ConfigValueType valueType, final String path) {
        return (List<T>) getList(true, valueType, path, ConfigValue::unwrapped);
    }

    private <T> List<T> getNullableList(final ConfigValueType valueType, final String path, final Function<ConfigValue, T> valueTransformer) {
        return (List<T>) getList(true, valueType, path, valueTransformer);
    }

    private <T> List<T> getList(final boolean propertyIsOptional, final ConfigValueType valueType, final String path, final Function<ConfigValue, T> valueTransformer) {
        final Stream<ConfigValue> values = propertyIsOptional && isMissingOrNull(path) ? null : getConfigValues(path, valueType);
        return values == null ? null : values.<T>map(valueTransformer).collect(toList());
    }

    private boolean ifNull(final String path, final String expected) {
        if (config.getIsNull(path))
            if (strictMode)
                throw new ConfigException.Null(config.origin(), path, expected);
            else
                return true;
        else
            return false;
    }

    private boolean isMissingOrNull(final String path) {
        return !config.hasPathOrNull(path) || config.getIsNull(path);
    }

    private Stream<ConfigValue> getConfigValues(final String path, final ConfigValueType to) {
        return ifNull(path, LIST.name()) ? null : config.getList(path).stream().map(value -> transformElement(strictMode, path, value, to));
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" + getDelegate() + "]";
    }

}