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
package com.typesafe.config.impl;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;

/**
 * Utility methods designed for the manipulation and handling of {@code ConfigValue}s.
 * <p>
 * <b>Note: Despite its public visibility, this class is not intended for use by end-users and should be considered part
 * of internal implementation details.</b> Its primary purpose is to access package-private classes and methods within
 * the {@code com.typesafe.config.impl} package. All methods contained within this class are strictly designated for
 * internal use only.
 * 
 * @author Zhenya Leonov
 */
public final class ConfigValues {

    private ConfigValues() {
    }

    public static <T> T getValue(final boolean strictMode, final Config config, final String path, final Function<ConfigValue, T> transformer) {
        return (T) getValue(strictMode, config, path, null, transformer);
    }

    public static <T> T getValue(final boolean strictMode, final Config config, final String path, final ConfigValueType expected, final Function<ConfigValue, T> transformer) {
        return (T) transformer.apply(transform(strictMode, path, getValue(((SimpleConfig) config).toFallbackValue(), Path.newPath(path)), expected));
    }

    public static ConfigValue transformElement(final boolean strictMode, final String path, final ConfigValue from, final ConfigValueType to) {
        final ConfigValue value = from.valueType() == to ? from : DefaultTransformer.transform((AbstractConfigValue) from, to);

        if (strictMode && value.valueType() == ConfigValueType.NULL || value.valueType() != ConfigValueType.NULL && value.valueType() != to)
            throw new ConfigException.WrongType(value.origin(), path, "list of " + to.name(), "list of " + value.valueType().name());

        return value;
    }

    public static Integer toInteger(final ConfigValue value, final String path) {
        return value.valueType() == ConfigValueType.NULL ? null : ((ConfigNumber) value).intValueRangeChecked(path);
    }

    public static Short toShort(final ConfigValue value, final String path) {
        if (value.valueType() == ConfigValueType.NULL)
            return null;

        final long longValue = ((ConfigNumber) value).longValue();

        if (longValue < Short.MIN_VALUE || longValue > Short.MAX_VALUE) {
            throw new ConfigException.WrongType(value.origin(), path, "16-bit short", "out-of-range value " + longValue);
        }
        return (short) longValue;
    }

    public static Long toLong(final ConfigValue value, final String path) {
        return value.valueType() == ConfigValueType.NULL ? null : ((ConfigNumber) value).longValue();
    }

    public static Double toDouble(final ConfigValue value, final String path) {
        return value.valueType() == ConfigValueType.NULL ? null : ((ConfigNumber) value).doubleValue();
    }

    public static <T extends Enum<T>> T toEnum(final Class<T> type, final ConfigValue value, final String path) {
        if (value.valueType() == ConfigValueType.NULL)
            return null;

        final String name = (String) value.unwrapped();

        try {
            return Enum.valueOf(type, name);
        } catch (final IllegalArgumentException e) {
            final Enum<?>[] constants = type.getEnumConstants();
            final String    names     = constants == null ? Collections.emptyList().toString() : Arrays.toString(constants);
            throw new ConfigException.BadValue(value.origin(), path, String.format("The enum class %s has no constant of the name '%s' (should be one of %s.)", type.getSimpleName(), name, names));
        }
    }

    public static java.nio.file.Path toPath(final ConfigValue value, final String path) {
        try {
            return value.valueType() == ConfigValueType.NULL ? null : Paths.get((String) value.unwrapped());
        } catch (final InvalidPathException e) {
            throw new ConfigException.BadValue(value.origin(), path, e.getMessage());
        }
    }

    public static java.nio.file.Path toRegularFile(final ConfigValue value, final String path, final LinkOption... options) {
        final java.nio.file.Path file = toPath(value, path);

        if (Files.isRegularFile(file, options))
            return file;

        throw new ConfigException.BadValue(value.origin(), path, file + " is not a regular file or does not exist");
    }

    public static java.nio.file.Path toDirectory(final ConfigValue value, final String path, final LinkOption... options) {
        final java.nio.file.Path directory = toPath(value, path);

        if (Files.isDirectory(directory, options))
            return directory;

        throw new ConfigException.BadValue(value.origin(), path, directory + " is not a directory or does not exist");
    }

    private static AbstractConfigValue getValue(final AbstractConfigObject object, final Path path) {
        return getValue(object, path, path);
    }

    private static AbstractConfigValue getValue(final AbstractConfigObject object, final Path path, final Path original) {
        try {
            final String key  = path.first();
            final Path   next = path.remainder();
            if (next == null)
                return getValue(object, key, original);
            else {
                final AbstractConfigObject o = (AbstractConfigObject) getValue(object, key, original.subPath(0, original.length() - next.length()));
                return getValue(o, next, original);
            }
        } catch (ConfigException.NotResolved e) {
            throw ConfigImpl.improveNotResolved(path, e);
        }
    }

    private static AbstractConfigValue getValue(final AbstractConfigObject object, final String key, final Path original) {
        final AbstractConfigValue value = object.peekAssumingResolved(key, original);

        if (value == null)
            throw new ConfigException.Missing(object.origin(), original.render());
        else
            return value;
    }

    private static ConfigValue transform(final boolean strictMode, final String path, final ConfigValue from, final ConfigValueType to) {
        final ConfigValue value = from.valueType() == to ? from : DefaultTransformer.transform((AbstractConfigValue) from, to);

        if (strictMode && value.valueType() == ConfigValueType.NULL)
            throw new ConfigException.Null(value.origin(), path, to.name());

        if (value.valueType() != ConfigValueType.NULL && value.valueType() != to)
            throw new ConfigException.WrongType(value.origin(), path, to.name(), value.valueType().name());

        return value;
    }

}