package software.leonov.config;

import static com.typesafe.config.ConfigValueType.BOOLEAN;
import static com.typesafe.config.ConfigValueType.LIST;
import static com.typesafe.config.ConfigValueType.NUMBER;
import static com.typesafe.config.ConfigValueType.STRING;
import static com.typesafe.config.impl.ConfigValues.getValue;
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
 * This class is a companion to the {@link Config} class in the configuration library
 * <a href="https://github.com/lightbend/config" target="_blank">Typesafe Config</a>, it allows users a more convenient
 * way to handle <i>optional</i> properties and provides a number of additional enhancements.
 * <p>
 * An instance of {@link OptionalConfig} is immutable and thread-safe. It can be constructed by passing a {@link Config}
 * object to the {@link #from(Config)} method. The {@link #setStrictMode(boolean)} determines how this class treats
 * properties with {@code null} values.
 * <p>
 * <strong>Discusson:</strong> Typesafe Config is one of the more popular configuration libraries for JVM languages. The
 * authors of Typesafe Config take an opinionated view on the handling of
 * <a href="https://github.com/lightbend/config#how-to-handle-defaults" _target="blank">default properties</a>:
 * 
 * <pre><i>
 *  Many other configuration APIs allow you to provide a default to the getter methods, like this:
 *      
 *      boolean getBoolean(String path, boolean fallback)
 *  
 *  Here, if the path has no setting, the fallback would be returned. An API could also return null for unset values, so you would check for null:
 * 
 *      // returns null on unset, check for null and fall back
 *      Boolean getBoolean(String path)
 * 
 *  The methods on the Config interface do NOT do this, for two major reasons:
 * 
 *      1. If you use a config setting in two places, the default fallback value gets cut-and-pasted and typically out of sync.
 *         This can result in Very Evil Bugs.
 *      2. If the getter returns null (or None, in Scala) then every time you get a setting you have to write handling code
 *         for null/None and that code will almost always just throw an exception. Perhaps more commonly, people
 *         forget to check for null at all, so missing settings result in NullPointerException.
 * </i></pre>
 * 
 * While this is generally applicable to <i>default</i> values, it does not address scenarios where properties have no
 * defaults and are inherently <i>optional</i>. For instance, consider a directory copy utility capable of filtering on
 * file extensions. The utility would be expected to transfer all files indiscriminately if no file extensions are
 * specified. Without native capability to handle optional properties, developers would have to resort to writing
 * cumbersome boilerplate code or use special placeholder value to indicate <i>no filter</i>.
 * <p>
 * See issues <a href="https://github.com/lightbend/config/issues/186">186</a>,
 * <a href="https://github.com/lightbend/config/issues/282" _target="blank">282</a>,
 * <a href="https://github.com/lightbend/config/issues/282" _target="blank">286</a>,
 * <a href="https://github.com/lightbend/config/issues/110" _target="blank">110</a>,
 * <a href="https://github.com/lightbend/config/issues/440" _target="blank">440</a> for historical discussion.
 * <p>
 * Java's {@link Optional} class serves as an elegant solution for handling optional properties in a fluent manner while
 * mitigating the concerns highlighted above.
 * <p>
 * API Example:
 * 
 * <pre> 
 *       final int nthreads = conf.getOptionalInteger("nthreads")
 *                                .orElse(Runtime.getRuntime().availableProcessors());
 *       
 *       System.out.println("number of threads: " + nthreads);
 * </pre>
 * <p>
 * The table below compares and contrasts this class to the {@link Config} class from Typesafe Config:
 * <p>
 * <pre>
 * <table border="1" cellpadding="5" cellspacing="1">
 *  <tr>
 *      <th>Method</th><th>Property</th><th>{@link Config} result</th><th>Strict mode</th><th>OptionalConfig result</th>
 *  </tr>
 *  <tr>
 *      <td rowspan=
"6">getXXXX</td><td>Valid value</td><td>Returns the property value</td><td>On/Off</td><td>Returns the property value</td>
 *  </tr>
 *  <tr>
 *      <td rowspan="2">Null value</td><td rowspan="5">Exception</td><td>Off</td><td>Returns null</td>
 *  </tr>
 *  <tr>
 *      <td>On</td><td rowspan="4">Exception</td>
 *  </tr>
 *  <tr>
 *      <td>Invalid value</td><td rowspan="4">On/Off</td>
 *  </tr>
 *  <tr>
 *      <td>Missing property</td>
 *  </tr>
 *  <tr>
 *      <td>Missing value</td>
 *  </tr>
 *  <tr>
 *      <td rowspan=
"8">getXXXXList</td><td>Valid list (no null values)</td><td>Returns the list of values</td><td>Returns the list of values</td>
 *  </tr>
 *  <tr>
 *      <td>Valid list (contains null values)</td><td rowspan="7">Exception</td><td rowspan=
"2">Off</td><td>Returns the list of values (including nulls)</td>
 *  </tr>
 *  <tr>
 *      <td>Null list</td><td>Returns null</td>
 *  </tr>
 *  <tr>
 *      <td>Valid list (contains null values)</td><td rowspan="2">On</td><td rowspan="5">Exception</td>
 *  </tr>
 *  <tr>
 *      <td>Null list</td>
 *  </tr>
 *  <tr>
 *      <td>Invalid list</td><td rowspan="11">On/Off</td>
 *  </tr>
 *  <tr>
 *      <td>Missing property</td>
 *  </tr>
 *  <tr>
 *      <td>Missing value</td>
 *  </tr>
 *  <tr>
 *      <td rowspan="5">getOptionalXXXX</td><td>Valid value</td><td rowspan=
"12">N/A</td><td>Returns an {@code Optional} containing the value</td>
 *  </tr>
 *  <tr>
 *      <td>Null value</td><td rowspan="2">Returns {@link Optional#empty()}</td>
 *  </tr>
 *  <tr>
 *      <td>Missing property</td>
 *  </tr>
 *  <tr>
 *      <td>Invalid value</td><td rowspan="2">Exception</td>
 *  </tr>
 *  <tr>
 *      <td>Missing value</td>
 *  </tr>  
 *  <tr>
 *      <td rowspan=
"7">getOptionalXXXXList</td><td>Valid list (no null values)</td><td>Returns an Optional containing the list of values</td>
 *  </tr>
 *  <tr>
 *      <td>Null list</td><td rowspan="2">Returns {@link Optional#empty()}</td>
 *  </tr>
 *  <tr>
 *      <td>Missing property</td>
 *  </tr>
 *  <tr>
 *      <td rowspan=
"2">Valid list (contains null values)</td><td>Off</td><td>Returns an Optional containing the list of values (including nulls)</td>
 *  </tr>
 *  <tr>
 *      <td>On</td><td rowspan="3">Exception</td>
 *  </tr>
 *  <tr>
 *      <td>Invalid list</td><td rowspan="2">On/Off</td>
 *  </tr>
 *  <tr>
 *      <td>Missing value</td>
 *  </tr>
 * </table>
 * </pre>
 * <p>
 * <strong>Additional functionality:</strong>
 * <p>
 * This class offers a handful of additional methods which have no equivalent functionality in the {@link Config} class:
 * 
 * <pre>
 * <table border="1" cellpadding="5" cellspacing="1">
 *  <tr>
 *      <th>Method</th><th>Functionality</th>
 *  </tr>
 *  <tr>
 *      <td>{@link #getShort(String)}</td><td rowspan=
"4">Offering methods which operate on properties referencing Short values (for completeness).</td>
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
"4">Offering methods which operate on properties referencing {@link Path} values.</td>
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
 *      <td>{@link #getRegularFile(String, LinkOption...)}</td><td rowspan=
"4">Offering methods which operate on properties referencing {@link Files#isRegularFile(Path, LinkOption...) existing} file paths.</td>
 *  </tr>
 *  <tr>
 *      <td>{@link #getOptionalRegularFile(String, LinkOption...)}</td>
 *  </tr>
 *  <tr>
 *      <td>{@link #getRegularFileList(String, LinkOption...)}</td>
 *  </tr>
 *  <tr>
 *      <td>{@link #getOptionalRegularFileList(String, LinkOption...)}</td>
 *  </tr>
 *  <tr>
 *      <td>{@link #getDirectory(String, LinkOption...)}</td><td rowspan=
"4">Offering methods which operate on properties referencing {@link Files#isDirectory(Path, LinkOption...) existing} directories.</td>
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
    public Config getConfig() {
        return config;
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
    public Path getRegularFile(final String path, final LinkOption... options) {
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
    public Optional<Path> getOptionalRegularFile(final String path, final LinkOption... options) {
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
    public List<Path> getRegularFileList(final String path, final LinkOption... options) {
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
    public Optional<List<Path>> getOptionalRegularFileList(final String path, final LinkOption... options) {
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
        return Optional.ofNullable(missingOrIsNull(path) ? null : getValue(false, config, path, valueType, valueTransformer));
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

    private <T> List<T> getList(final boolean optional, final ConfigValueType valueType, final String path, final Function<ConfigValue, T> valueTransformer) {
        final Stream<ConfigValue> values = optional && missingOrIsNull(path) ? null : getConfigValues(path, valueType);
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

    private boolean missingOrIsNull(final String path) {
        return !config.hasPathOrNull(path) || config.getIsNull(path);
    }

    private Stream<ConfigValue> getConfigValues(final String path, final ConfigValueType to) {
        return ifNull(path, LIST.name()) ? null : config.getList(path).stream().map(value -> transformElement(strictMode, path, value, to));
    }

}