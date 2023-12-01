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
package software.leonov.config.test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;

import software.leonov.config.OptionalConfig;

class OptionalConfigTest {

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        System.out.println("Executing " + OptionalConfigTest.class.getSimpleName());
    }

    @AfterAll
    static void tearDownAfterClass() throws Exception {
    }

    @BeforeEach
    void setUp(final TestInfo info) throws Exception {
        final String display = info.getDisplayName();
        System.out.println("\n" + (display.startsWith("[") ? info.getTestMethod().get().getName() + "(): " + display : display));
    }

    @AfterEach
    void tearDown() throws Exception {
    }

    static OptionalConfig createConfiguration(final String properties) {
        return OptionalConfig.from(ConfigFactory.parseString(properties));
    }

    static OptionalConfig emptyConfiguration() {
        return OptionalConfig.from(ConfigFactory.empty());
    }

    /*** Boolean ***/

    @ParameterizedTest
    @ValueSource(strings = { "true", "yes" })
    void test_getBoolean_true(final String value) {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = " + value + "} }");

        assertThat(conf.getBoolean("outer.inner.prop")).isTrue();
        assertThat(conf.getConfig().getBoolean("outer.inner.prop")).isTrue();
        assertThat(conf.getOptionalBoolean("outer.inner.prop")).hasValue(true);
    }

    @ParameterizedTest
    @ValueSource(strings = { "false", "no" })
    void test_getBoolean_false(final String value) {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = " + value + "} }");

        assertThat(conf.getBoolean("outer.inner.prop")).isFalse();
        assertThat(conf.getConfig().getBoolean("outer.inner.prop")).isFalse();
        assertThat(conf.getOptionalBoolean("outer.inner.prop")).hasValue(false);
    }

    @Test
    void test_getBoolean_ConfigException_Missing() {
        final OptionalConfig conf = emptyConfiguration();

        final Exception e1 = assertThrows(ConfigException.Missing.class, () -> conf.getBoolean("path"));
        final Exception e2 = assertThrows(ConfigException.Missing.class, () -> conf.getConfig().getBoolean("path"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(conf.getOptionalBoolean("path")).isEmpty();
    }

    @Test
    void test_getBoolean_null_strictMode_false() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = null } }").setStrictMode(false);

        assertThat(conf.getBoolean("outer.inner.prop")).isNull();
        assertThat(conf.getOptionalBoolean("outer.inner.prop")).isEmpty();
    }

    @Test
    void test_getBoolean_null_strictMode_true() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = null } }").setStrictMode(true);

        final Exception e1 = assertThrows(ConfigException.Null.class, () -> conf.getBoolean("outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.Null.class, () -> conf.getConfig().getBoolean("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(conf.getOptionalBoolean("outer.inner.prop")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "outer { inner { prop {a = 1} } }", "outer { inner { prop = [a, b, c] } }", "outer { inner { prop = 1.5 } }", "outer { inner { prop = abc } }" })
    void test_getBoolean_WrongType(final String configuration) {
        final OptionalConfig conf = createConfiguration(configuration);

        final Exception e1 = assertThrows(ConfigException.WrongType.class, () -> conf.getBoolean("outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getBoolean("outer.inner.prop"));
        final Exception e3 = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalBoolean("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    /*** Boolean List ***/

    @Test
    void test_getBooleanList() {
        final OptionalConfig conf     = createConfiguration("outer { inner { prop = [true, false, true] } }");
        final List<Boolean>  expected = Lists.newArrayList(true, false, true);

        assertThat(conf.getBooleanList("outer.inner.prop")).isEqualTo(expected);
        assertThat(conf.getOptionalBooleanList("outer.inner.prop")).hasValue(expected);
        assertThat(conf.getConfig().getBooleanList("outer.inner.prop")).isEqualTo(expected);
    }

    @Test
    void test_getBooleanList_null_element_strictMode_false() {
        final OptionalConfig conf     = createConfiguration("outer { inner { prop = [true, false, null, true] } }").setStrictMode(false);
        final List<Boolean>  expected = Lists.newArrayList(true, false, null, true);

        assertThat(conf.getBooleanList("outer.inner.prop")).isEqualTo(expected);
        assertThat(conf.getOptionalBooleanList("outer.inner.prop")).hasValue(expected);
    }

    @Test
    void test_getBooleanList_null_element_strictMode_true() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = [true, false, null, true] } }").setStrictMode(true);
        final Exception      e1   = assertThrows(ConfigException.WrongType.class, () -> conf.getBooleanList("outer.inner.prop"));
        final Exception      e2   = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getBooleanList("outer.inner.prop"));
        final Exception      e3   = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalBooleanList("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    @Test
    void test_getBooleanList_null_strictMode_false() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = null } }").setStrictMode(false);

        assertThat(conf.getBooleanList("outer.inner.prop")).isNull();
        assertThat(conf.getOptionalBooleanList("outer.inner.prop")).isEmpty();
    }

    @Test
    void test_getBooleanList_null_strictMode_true() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = null } }").setStrictMode(true);

        final Exception e1 = assertThrows(ConfigException.Null.class, () -> conf.getBooleanList("outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.Null.class, () -> conf.getConfig().getBooleanList("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(conf.getOptionalBooleanList("outer.inner.prop")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "outer { inner { prop = 5.3 } }", "outer { inner { prop = a } }", "outer { inner { prop { abc = 1 } } }" })
    void test_getBooleanList_WrongType(final String configuration) {
        final OptionalConfig conf = createConfiguration(configuration);

        final Exception e1 = assertThrows(ConfigException.WrongType.class, () -> conf.getBooleanList("outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getBooleanList("outer.inner.prop"));
        final Exception e3 = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalBooleanList("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = { "outer { inner { prop = [true, false, 1.5] } }", "outer { inner { prop = [true, false, [true, false]] } }", "outer { inner { prop = [true, false, {a = false}] } }" })
    void test_getBooleanList_WrongType_element(final String configuration) {
        final OptionalConfig conf = createConfiguration(configuration);

        final Exception e1 = assertThrows(ConfigException.WrongType.class, () -> conf.getBooleanList("outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getBooleanList("outer.inner.prop"));
        final Exception e3 = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalBooleanList("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    /*** Double ***/

    @Test
    void test_getDouble() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = 15498 } }");

        assertThat(conf.getDouble("outer.inner.prop")).isEqualTo(15498);
        assertThat(conf.getConfig().getDouble("outer.inner.prop")).isEqualTo(15498);
        assertThat(conf.getOptionalDouble("outer.inner.prop")).hasValue(15498);
    }

    @Test
    void test_getDouble_ConfigException_Missing() {
        final OptionalConfig conf = emptyConfiguration();

        final Exception e1 = assertThrows(ConfigException.Missing.class, () -> conf.getDouble("path"));
        final Exception e2 = assertThrows(ConfigException.Missing.class, () -> conf.getConfig().getDouble("path"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(conf.getOptionalDouble("path")).isEmpty();
    }

    @Test
    void test_getDouble_null_strictMode_false() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = null } }").setStrictMode(false);

        assertThat(conf.getDouble("outer.inner.prop")).isNull();
        assertThat(conf.getOptionalDouble("outer.inner.prop")).isEmpty();
    }

    @Test
    void test_getDouble_null_strictMode_true() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = null } }").setStrictMode(true);

        final Exception e1 = assertThrows(ConfigException.Null.class, () -> conf.getDouble("outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.Null.class, () -> conf.getConfig().getDouble("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(conf.getOptionalDouble("outer.inner.prop")).isEmpty();
    }

    @Test
    void test_getDouble_toBig() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = -1.8976931348623157E308 } }");

        assertThat(conf.getDouble("outer.inner.prop")).isEqualTo(Double.NEGATIVE_INFINITY);
        assertThat(conf.getConfig().getDouble("outer.inner.prop")).isEqualTo(Double.NEGATIVE_INFINITY);
        assertThat(conf.getOptionalDouble("outer.inner.prop")).hasValue(Double.NEGATIVE_INFINITY);
    }

    @ParameterizedTest
    @ValueSource(strings = { "outer { inner { prop {a = 1} } }", "outer { inner { prop = [a, b, c] } }", "outer { inner { prop = abc } }", "outer { inner { prop = true } }" })
    void test_getDouble_WrongType(final String configuration) {
        final OptionalConfig conf = createConfiguration(configuration);

        final Exception e1 = assertThrows(ConfigException.WrongType.class, () -> conf.getDouble("outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getDouble("outer.inner.prop"));
        final Exception e3 = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalDouble("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    /*** Double List ***/

    @Test
    void test_getDoubleList() {
        final OptionalConfig conf     = createConfiguration("outer { inner { prop = [7.5, 32.3, 14.1] } }");
        final List<Double>   expected = Lists.newArrayList(7.5, 32.3, 14.1);

        assertThat(conf.getDoubleList("outer.inner.prop")).isEqualTo(expected);
        assertThat(conf.getOptionalDoubleList("outer.inner.prop")).hasValue(expected);
        assertThat(conf.getConfig().getDoubleList("outer.inner.prop")).isEqualTo(expected);
    }

    @Test
    void test_getDoubleList_element_toBig() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = [1.8976931348623157E308, 5.3] } }");

        final List<Double> expected = Lists.newArrayList(Double.POSITIVE_INFINITY, 5.3);

        assertThat(conf.getDoubleList("outer.inner.prop")).isEqualTo(expected);
        assertThat(conf.getConfig().getDoubleList("outer.inner.prop")).isEqualTo(expected);
        assertThat(conf.getOptionalDoubleList("outer.inner.prop")).hasValue(expected);
    }

    @Test
    void test_getDoubleList_null_element_strictMode_false() {
        final OptionalConfig conf     = createConfiguration("outer { inner { prop = [1.1, 2.2, null, 4.4, null] } }").setStrictMode(false);
        final List<Double>   expected = Lists.newArrayList(1.1, 2.2, null, 4.4, null);

        assertThat(conf.getDoubleList("outer.inner.prop")).isEqualTo(expected);
        assertThat(conf.getOptionalDoubleList("outer.inner.prop")).hasValue(expected);
    }

    @Test
    void test_getDoubleList_null_element_strictMode_true() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = [1.1, 2.2, null, 4.4, null] } }").setStrictMode(true);
        final Exception      e1   = assertThrows(ConfigException.WrongType.class, () -> conf.getDoubleList("outer.inner.prop"));
        final Exception      e2   = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getDoubleList("outer.inner.prop"));
        final Exception      e3   = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalDoubleList("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    @Test
    void test_getDoubleList_null_strictMode_false() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = null } }").setStrictMode(false);

        assertThat(conf.getDoubleList("outer.inner.prop")).isNull();
        assertThat(conf.getOptionalDoubleList("outer.inner.prop")).isEmpty();
    }

    @Test
    void test_getDoubleList_null_strictMode_true() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = null } }").setStrictMode(true);

        final Exception e1 = assertThrows(ConfigException.Null.class, () -> conf.getDoubleList("outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.Null.class, () -> conf.getConfig().getDoubleList("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(conf.getOptionalDoubleList("outer.inner.prop")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "outer { inner { prop = true } }", "outer { inner { prop = a } }", "outer { inner { prop { abc = 1 } } }" })
    void test_getDoubleList_WrongType(final String configuration) {
        final OptionalConfig conf = createConfiguration(configuration);

        final Exception e1 = assertThrows(ConfigException.WrongType.class, () -> conf.getDoubleList("outer.inner.prop"));
        final Exception e3 = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getDoubleList("outer.inner.prop"));

        final Exception e2 = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalDoubleList("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = { "outer { inner { prop = [true, false, abc] } }", "outer { inner { prop = [1.5, 2.3, [3.4, 4.89]] } }", "outer { inner { prop = [1.2, 2.3, {a = 3.4}] } }" })
    void test_getDoubleList_WrongType_element(final String configuration) {
        final OptionalConfig conf = createConfiguration(configuration);

        final Exception e1 = assertThrows(ConfigException.WrongType.class, () -> conf.getDoubleList("outer.inner.prop"));
        final Exception e3 = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getDoubleList("outer.inner.prop"));

        final Exception e2 = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalDoubleList("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    /*** Short ***/

    @Test
    void test_getShort() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = 15498 } }");

        assertThat(conf.getShort("outer.inner.prop")).isEqualTo(15498);
        assertThat(conf.getConfig().getInt("outer.inner.prop")).isEqualTo(15498);

        assertThat(conf.getOptionalShort("outer.inner.prop")).hasValue(15498);
    }

    @Test
    void test_getShort_ConfigException_Missing() {
        final OptionalConfig conf = emptyConfiguration();

        final Exception e1 = assertThrows(ConfigException.Missing.class, () -> conf.getShort("path"));
        final Exception e2 = assertThrows(ConfigException.Missing.class, () -> conf.getConfig().getInt("path"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(conf.getOptionalShort("path")).isEmpty();
    }

    @Test
    void test_getShort_null_strictMode_false() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = null } }").setStrictMode(false);

        assertThat(conf.getShort("outer.inner.prop")).isNull();
        assertThat(conf.getOptionalShort("outer.inner.prop")).isEmpty();
    }

    @Test
    void test_getShort_null_strictMode_true() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = null } }").setStrictMode(true);

        final Exception e1 = assertThrows(ConfigException.Null.class, () -> conf.getShort("outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.Null.class, () -> conf.getConfig().getInt("outer.inner.prop"));
        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());

        assertThat(conf.getOptionalShort("outer.inner.prop")).isEmpty();
    }

    @Test
    void test_getShort_toBig() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = 3147483647 } }");

        final Exception e1 = assertThrows(ConfigException.WrongType.class, () -> conf.getShort("outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalShort("outer.inner.prop"));

        assertThat(e1.getMessage()).endsWith("prop has type out-of-range value 3147483647 rather than 16-bit short");
        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = { "outer { inner { prop {a = 1} } }", "outer { inner { prop = [a, b, c] } }", "outer { inner { prop = abc } }", "outer { inner { prop = true } }" })
    void test_getShort_WrongType(final String configuration) {
        final OptionalConfig conf = createConfiguration(configuration);

        final Exception e1 = assertThrows(ConfigException.WrongType.class, () -> conf.getShort("outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getInt("outer.inner.prop"));

        final Exception e3 = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalShort("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    /*** Short List ***/

    @Test
    void test_getShortList() {
        final OptionalConfig conf     = createConfiguration("outer { inner { prop = [7, 32, 14] } }");
        final List<Short>    expected = Lists.newArrayList((short) 7, (short) 32, (short) 14);

        assertThat(conf.getShortList("outer.inner.prop")).isEqualTo(expected);
        assertThat(conf.getOptionalShortList("outer.inner.prop")).hasValue(expected);

        assertThat(Lists.transform(conf.getConfig().getIntList("outer.inner.prop"), i -> i.shortValue())).isEqualTo(expected);
    }

    @Test
    void test_getShortList_element_toBig() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = [3147483647] } }");

        final Exception e1 = assertThrows(ConfigException.WrongType.class, () -> conf.getShortList("outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalShortList("outer.inner.prop"));

        assertThat(e1.getMessage()).endsWith("prop has type out-of-range value 3147483647 rather than 16-bit short");
        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
    }

    @Test
    void test_getShortList_null_element_strictMode_false() {
        final OptionalConfig conf     = createConfiguration("outer { inner { prop = [1, 2, null, 4, null] } }").setStrictMode(false);
        final List<Short>    expected = Lists.newArrayList((short) 1, (short) 2, null, (short) 4, null);
        assertThat(conf.getShortList("outer.inner.prop")).isEqualTo(expected);
        assertThat(conf.getOptionalShortList("outer.inner.prop")).hasValue(expected);
    }

    @Test
    void test_getShortList_null_element_strictMode_true() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = [1, 2, null, 4, null] } }").setStrictMode(true);
        final Exception      e1   = assertThrows(ConfigException.WrongType.class, () -> conf.getShortList("outer.inner.prop"));
        final Exception      e2   = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getIntList("outer.inner.prop"));
        final Exception      e3   = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalShortList("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    @Test
    void test_getShortList_null_strictMode_false() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = null } }").setStrictMode(false);

        assertThat(conf.getShortList("outer.inner.prop")).isNull();
        assertThat(conf.getOptionalShortList("outer.inner.prop")).isEmpty();
    }

    @Test
    void test_getShortList_null_strictMode_true() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = null } }").setStrictMode(true);

        final Exception e1 = assertThrows(ConfigException.Null.class, () -> conf.getShortList("outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.Null.class, () -> conf.getConfig().getIntList("outer.inner.prop"));
        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());

        assertThat(conf.getOptionalShortList("outer.inner.prop")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "outer { inner { prop = 5.3 } }", "outer { inner { prop = a } }", "outer { inner { prop { abc = 1 } } }" })
    void test_getShortList_WrongType(final String configuration) {
        final OptionalConfig conf = createConfiguration(configuration);

        final Exception e1 = assertThrows(ConfigException.WrongType.class, () -> conf.getShortList("outer.inner.prop"));
        final Exception e3 = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getIntList("outer.inner.prop"));

        final Exception e2 = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalShortList("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = { "outer { inner { prop = [true, false, abc] } }", "outer { inner { prop = [1, 2, [3, 4]] } }", "outer { inner { prop = [1, 2, {a = 3}] } }" })
    void test_getShortList_WrongType_element(final String configuration) {
        final OptionalConfig conf = createConfiguration(configuration);

        final Exception e1 = assertThrows(ConfigException.WrongType.class, () -> conf.getShortList("outer.inner.prop"));
        final Exception e3 = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getIntList("outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalShortList("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    /*** Integer ***/

    @Test
    void test_getInteger() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = 15498 } }");

        assertThat(conf.getInteger("outer.inner.prop")).isEqualTo(15498);
        assertThat(conf.getConfig().getInt("outer.inner.prop")).isEqualTo(15498);

        assertThat(conf.getOptionalInteger("outer.inner.prop")).hasValue(15498);
    }

    @Test
    void test_getInteger_ConfigException_Missing() {
        final OptionalConfig conf = emptyConfiguration();

        final Exception e1 = assertThrows(ConfigException.Missing.class, () -> conf.getInteger("path"));
        final Exception e2 = assertThrows(ConfigException.Missing.class, () -> conf.getConfig().getInt("path"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(conf.getOptionalInteger("path")).isEmpty();
    }

    @Test
    void test_getInteger_null_strictMode_false() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = null } }").setStrictMode(false);

        assertThat(conf.getInteger("outer.inner.prop")).isNull();
        assertThat(conf.getOptionalInteger("outer.inner.prop")).isEmpty();
    }

    @Test
    void test_getInteger_null_strictMode_true() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = null } }").setStrictMode(true);

        final Exception e1 = assertThrows(ConfigException.Null.class, () -> conf.getInteger("outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.Null.class, () -> conf.getConfig().getInt("outer.inner.prop"));
        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());

        assertThat(conf.getOptionalInteger("outer.inner.prop")).isEmpty();
    }

    @Test
    void test_getInteger_toBig() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = 3147483647 } }");

        final Exception e1 = assertThrows(ConfigException.WrongType.class, () -> conf.getInteger("outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getInt("outer.inner.prop"));

        final Exception e3 = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalInteger("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = { "outer { inner { prop {a = 1} } }", "outer { inner { prop = [a, b, c] } }", "outer { inner { prop = abc } }", "outer { inner { prop = true } }" })
    void test_getInteger_WrongType(final String configuration) {
        final OptionalConfig conf = createConfiguration(configuration);

        final Exception e1 = assertThrows(ConfigException.WrongType.class, () -> conf.getInteger("outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getInt("outer.inner.prop"));

        final Exception e3 = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalInteger("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    /*** Integer List ***/

    @Test
    void test_getIntegerList() {
        final OptionalConfig conf     = createConfiguration("outer { inner { prop = [7, 32, 14] } }");
        final List<Integer>  expected = Lists.newArrayList(7, 32, 14);

        assertThat(conf.getIntegerList("outer.inner.prop")).isEqualTo(expected);
        assertThat(conf.getOptionalIntegerList("outer.inner.prop")).hasValue(expected);

        assertThat(conf.getConfig().getIntList("outer.inner.prop")).isEqualTo(expected);
    }

    @Test
    void test_getIntegerList_element_toBig() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = [3147483647] } }");

        final Exception e1 = assertThrows(ConfigException.WrongType.class, () -> conf.getIntegerList("outer.inner.prop"));
        final Exception e3 = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getIntList("outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalIntegerList("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    @Test
    void test_getIntegerList_null_element_strictMode_false() {
        final OptionalConfig conf     = createConfiguration("outer { inner { prop = [1, 2, null, 4, null] } }").setStrictMode(false);
        final List<Integer>  expected = Lists.newArrayList(1, 2, null, 4, null);
        assertThat(conf.getIntegerList("outer.inner.prop")).isEqualTo(expected);
        assertThat(conf.getOptionalIntegerList("outer.inner.prop")).hasValue(expected);
    }

    @Test
    void test_getIntegerList_null_element_strictMode_true() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = [1, 2, null, 4, null] } }").setStrictMode(true);
        final Exception      e1   = assertThrows(ConfigException.WrongType.class, () -> conf.getIntegerList("outer.inner.prop"));
        final Exception      e2   = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getIntList("outer.inner.prop"));
        final Exception      e3   = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalIntegerList("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    @Test
    void test_getIntegerList_null_strictMode_false() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = null } }").setStrictMode(false);

        assertThat(conf.getIntegerList("outer.inner.prop")).isNull();
        assertThat(conf.getOptionalIntegerList("outer.inner.prop")).isEmpty();
    }

    @Test
    void test_getIntegerList_null_strictMode_true() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = null } }").setStrictMode(true);

        final Exception e1 = assertThrows(ConfigException.Null.class, () -> conf.getIntegerList("outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.Null.class, () -> conf.getConfig().getIntList("outer.inner.prop"));
        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());

        assertThat(conf.getOptionalIntegerList("outer.inner.prop")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "outer { inner { prop = 5.3 } }", "outer { inner { prop = a } }", "outer { inner { prop { abc = 1 } } }" })
    void test_getIntegerList_WrongType(final String configuration) {
        final OptionalConfig conf = createConfiguration(configuration);

        final Exception e1 = assertThrows(ConfigException.WrongType.class, () -> conf.getIntegerList("outer.inner.prop"));
        final Exception e3 = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getIntList("outer.inner.prop"));

        final Exception e2 = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalIntegerList("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = { "outer { inner { prop = [true, false, abc] } }", "outer { inner { prop = [1, 2, [3, 4]] } }", "outer { inner { prop = [1, 2, {a = 3}] } }" })
    void test_getIntegerList_WrongType_element(final String configuration) {
        final OptionalConfig conf = createConfiguration(configuration);

        final Exception e1 = assertThrows(ConfigException.WrongType.class, () -> conf.getIntegerList("outer.inner.prop"));
        final Exception e3 = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getIntList("outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalIntegerList("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    /*** Long ***/

    @Test
    void test_getLong() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = 15498 } }");

        assertThat(conf.getLong("outer.inner.prop")).isEqualTo(15498);
        assertThat(conf.getConfig().getLong("outer.inner.prop")).isEqualTo(15498);

        assertThat(conf.getOptionalLong("outer.inner.prop")).hasValue(15498);
    }

    @Test
    void test_getLong_ConfigException_Missing() {
        final OptionalConfig conf = emptyConfiguration();

        final Exception e1 = assertThrows(ConfigException.Missing.class, () -> conf.getLong("path"));
        final Exception e2 = assertThrows(ConfigException.Missing.class, () -> conf.getConfig().getLong("path"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(conf.getOptionalLong("path")).isEmpty();
    }

    @Test
    void test_getLong_null_strictMode_false() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = null } }").setStrictMode(false);

        assertThat(conf.getLong("outer.inner.prop")).isNull();
        assertThat(conf.getOptionalLong("outer.inner.prop")).isEmpty();
    }

    @Test
    void test_getLong_null_strictMode_true() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = null } }").setStrictMode(true);

        final Exception e1 = assertThrows(ConfigException.Null.class, () -> conf.getLong("outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.Null.class, () -> conf.getConfig().getLong("outer.inner.prop"));
        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());

        assertThat(conf.getOptionalLong("outer.inner.prop")).isEmpty();
    }

    @Test
    void test_getLong_toBig() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = 19223372036854775807 } }");

        assertThat(conf.getLong("outer.inner.prop")).isEqualTo(Long.MAX_VALUE);
        assertThat(conf.getConfig().getLong("outer.inner.prop")).isEqualTo(Long.MAX_VALUE);

        assertThat(conf.getOptionalLong("outer.inner.prop")).hasValue(Long.MAX_VALUE);
    }

    @ParameterizedTest
    @ValueSource(strings = { "outer { inner { prop {a = 1} } }", "outer { inner { prop = [a, b, c] } }", "outer { inner { prop = abc } }", "outer { inner { prop = true } }" })
    void test_getLong_WrongType(final String configuration) {
        final OptionalConfig conf = createConfiguration(configuration);

        final Exception e1 = assertThrows(ConfigException.WrongType.class, () -> conf.getLong("outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getLong("outer.inner.prop"));

        final Exception e3 = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalLong("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    /*** Long List ***/

    @Test
    void test_getLongList() {
        final OptionalConfig conf     = createConfiguration("outer { inner { prop = [7, 32, 14] } }");
        final List<Long>     expected = Lists.newArrayList(7L, 32L, 14L);

        assertThat(conf.getLongList("outer.inner.prop")).isEqualTo(expected);
        assertThat(conf.getOptionalLongList("outer.inner.prop")).hasValue(expected);

        assertThat(conf.getConfig().getLongList("outer.inner.prop")).isEqualTo(expected);
    }

    @Test
    void test_getLongList_element_toBig() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = [19223372036854775807, 5] } }");

        final List<Long> expected = Lists.newArrayList(Long.MAX_VALUE, 5L);

        assertThat(conf.getLongList("outer.inner.prop")).isEqualTo(expected);
        assertThat(conf.getConfig().getLongList("outer.inner.prop")).isEqualTo(expected);
        assertThat(conf.getOptionalLongList("outer.inner.prop")).hasValue(expected);
    }

    @Test
    void test_getLongList_null_element_strictMode_false() {
        final OptionalConfig conf     = createConfiguration("outer { inner { prop = [1, 2, null, 4, null] } }").setStrictMode(false);
        final List<Long>     expected = Lists.newArrayList(1L, 2L, null, 4L, null);
        assertThat(conf.getLongList("outer.inner.prop")).isEqualTo(expected);
        assertThat(conf.getOptionalLongList("outer.inner.prop")).hasValue(expected);
    }

    @Test
    void test_getLongList_null_element_strictMode_true() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = [1, 2, null, 4, null] } }").setStrictMode(true);
        final Exception      e1   = assertThrows(ConfigException.WrongType.class, () -> conf.getLongList("outer.inner.prop"));
        final Exception      e2   = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getLongList("outer.inner.prop"));
        final Exception      e3   = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalLongList("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    @Test
    void test_getLongList_null_strictMode_false() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = null } }").setStrictMode(false);

        assertThat(conf.getLongList("outer.inner.prop")).isNull();
        assertThat(conf.getOptionalLongList("outer.inner.prop")).isEmpty();
    }

    @Test
    void test_getLongList_null_strictMode_true() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = null } }").setStrictMode(true);

        final Exception e1 = assertThrows(ConfigException.Null.class, () -> conf.getLongList("outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.Null.class, () -> conf.getConfig().getLongList("outer.inner.prop"));
        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());

        assertThat(conf.getOptionalLongList("outer.inner.prop")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "outer { inner { prop = 5.3 } }", "outer { inner { prop = a } }", "outer { inner { prop { abc = 1 } } }" })
    void test_getLongList_WrongType(final String configuration) {
        final OptionalConfig conf = createConfiguration(configuration);

        final Exception e1 = assertThrows(ConfigException.WrongType.class, () -> conf.getLongList("outer.inner.prop"));
        final Exception e3 = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getLongList("outer.inner.prop"));

        final Exception e2 = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalLongList("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = { "outer { inner { prop = [true, false, abc] } }", "outer { inner { prop = [1, 2, [3, 4]] } }", "outer { inner { prop = [1, 2, {a = 3}] } }" })
    void test_getLongList_WrongType_element(final String configuration) {
        final OptionalConfig conf = createConfiguration(configuration);

        final Exception e1 = assertThrows(ConfigException.WrongType.class, () -> conf.getLongList("outer.inner.prop"));
        final Exception e3 = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getLongList("outer.inner.prop"));

        final Exception e2 = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalLongList("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    /*** String ***/

    @Test
    void test_getString() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = FooBar XYZ } }");

        assertThat(conf.getString("outer.inner.prop")).isEqualTo("FooBar XYZ");
        assertThat(conf.getConfig().getString("outer.inner.prop")).isEqualTo("FooBar XYZ");

        assertThat(conf.getOptionalString("outer.inner.prop")).hasValue("FooBar XYZ");
    }

    @Test
    void test_getString_ConfigException_Missing() {
        final OptionalConfig conf = emptyConfiguration();

        final Exception e1 = assertThrows(ConfigException.Missing.class, () -> conf.getString("path"));
        final Exception e2 = assertThrows(ConfigException.Missing.class, () -> conf.getConfig().getString("path"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(conf.getOptionalString("path")).isEmpty();
    }

    @Test
    void test_getString_null_strictMode_false() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = null } }").setStrictMode(false);

        assertThat(conf.getString("outer.inner.prop")).isNull();
        assertThat(conf.getOptionalString("outer.inner.prop")).isEmpty();
    }

    @Test
    void test_getString_null_strictMode_true() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = null } }").setStrictMode(true);

        final Exception e1 = assertThrows(ConfigException.Null.class, () -> conf.getString("outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.Null.class, () -> conf.getConfig().getString("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(conf.getOptionalString("outer.inner.prop")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "outer { inner { prop {a = 1} } }", "outer { inner { prop = [a, b, c] } }" })
    void test_getString_WrongType(final String configuration) {
        final OptionalConfig conf = createConfiguration(configuration);

        final Exception e1 = assertThrows(ConfigException.WrongType.class, () -> conf.getString("outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getString("outer.inner.prop"));

        final Exception e3 = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalString("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    /*** String List ***/

    @Test
    void test_getStringList() {
        final OptionalConfig conf     = createConfiguration("outer { inner { prop = [a, b, c] } }");
        final List<String>   expected = Lists.newArrayList("a", "b", "c");

        assertThat(conf.getStringList("outer.inner.prop")).isEqualTo(expected);
        assertThat(conf.getOptionalStringList("outer.inner.prop")).hasValue(expected);

        assertThat(conf.getConfig().getStringList("outer.inner.prop")).isEqualTo(expected);
    }

    @Test
    void test_getStringList_null_element_strictMode_false() {
        final OptionalConfig conf     = createConfiguration("outer { inner { prop = [a, b, c, null, d] } }").setStrictMode(false);
        final List<String>   expected = Lists.newArrayList("a", "b", "c", null, "d");
        assertThat(conf.getStringList("outer.inner.prop")).isEqualTo(expected);
        assertThat(conf.getOptionalStringList("outer.inner.prop")).hasValue(expected);
    }

    @Test
    void test_getStringList_null_element_strictMode_true() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = [a, b, c, null, d] } }").setStrictMode(true);
        final Exception      e1   = assertThrows(ConfigException.WrongType.class, () -> conf.getStringList("outer.inner.prop"));
        final Exception      e2   = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getStringList("outer.inner.prop"));
        final Exception      e3   = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalStringList("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    @Test
    void test_getStringList_null_strictMode_false() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = null } }").setStrictMode(false);

        assertThat(conf.getStringList("outer.inner.prop")).isNull();
        assertThat(conf.getOptionalStringList("outer.inner.prop")).isEmpty();
    }

    @Test
    void test_getStringList_null_strictMode_true() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = null } }").setStrictMode(true);

        final Exception e1 = assertThrows(ConfigException.Null.class, () -> conf.getStringList("outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.Null.class, () -> conf.getConfig().getStringList("outer.inner.prop"));
        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());

        assertThat(conf.getOptionalStringList("outer.inner.prop")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "outer { inner { prop = 5.3 } }", "outer { inner { prop = a } }", "outer { inner { prop { abc = 1 } } }" })
    void test_getStringList_WrongType(final String configuration) {
        final OptionalConfig conf = createConfiguration(configuration);

        final Exception e1 = assertThrows(ConfigException.WrongType.class, () -> conf.getStringList("outer.inner.prop"));
        final Exception e3 = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getStringList("outer.inner.prop"));

        final Exception e2 = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalStringList("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = { "outer { inner { prop = [a, b, [1, 2]] } }", "outer { inner { prop = [a, b, {a = b}] } }" })
    void test_getStringList_WrongType_element(final String configuration) {
        final OptionalConfig conf = createConfiguration(configuration);

        final Exception e1 = assertThrows(ConfigException.WrongType.class, () -> conf.getStringList("outer.inner.prop"));
        final Exception e3 = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getStringList("outer.inner.prop"));

        final Exception e2 = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalStringList("outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    /*** Enum ***/

    public enum Day {
        SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY
    }

    @Test
    void test_getEnum() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = SUNDAY } }");

        assertThat(conf.getEnum(Day.class, "outer.inner.prop")).isEqualTo(Day.SUNDAY);
        assertThat(conf.getConfig().getEnum(Day.class, "outer.inner.prop")).isEqualTo(Day.SUNDAY);

        assertThat(conf.getOptionalEnum(Day.class, "outer.inner.prop")).hasValue(Day.SUNDAY);
    }

    @Test
    void test_getEnum_ConfigException_Missing() {
        final OptionalConfig conf = emptyConfiguration();

        final Exception e1 = assertThrows(ConfigException.Missing.class, () -> conf.getEnum(Day.class, "path"));
        final Exception e2 = assertThrows(ConfigException.Missing.class, () -> conf.getConfig().getEnum(Day.class, "path"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(conf.getOptionalEnum(Day.class, "path")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "outer { inner { prop {day = SUNDAY} } }", "outer { inner { prop = [SUNDAY, MONDAY] } }" })
    void test_getEnum_WrongType(final String configuration) {
        final OptionalConfig conf = createConfiguration(configuration);

        final Exception e1 = assertThrows(ConfigException.WrongType.class, () -> conf.getEnum(Day.class, "outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getEnum(Day.class, "outer.inner.prop"));
        final Exception e3 = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalEnum(Day.class, "outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    /*
     * We may want to file a bug because Enums are treated differently. Other entities always throw WrongType
     */
    @ParameterizedTest
    @ValueSource(strings = { "outer { inner { prop = 1.5 } }", "outer { inner { prop = true } }", "outer { inner { prop = ABC } }" })
    void test_getEnum_BadValue(final String configuration) {
        final OptionalConfig conf = createConfiguration(configuration);

        final Exception e1 = assertThrows(ConfigException.BadValue.class, () -> conf.getEnum(Day.class, "outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.BadValue.class, () -> conf.getConfig().getEnum(Day.class, "outer.inner.prop"));
        final Exception e3 = assertThrows(ConfigException.BadValue.class, () -> conf.getOptionalEnum(Day.class, "outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    @Test
    void test_getEnum_null_strictMode_false() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = null } }").setStrictMode(false);

        assertThat(conf.getEnum(Day.class, "outer.inner.prop")).isNull();
        assertThat(conf.getOptionalEnum(Day.class, "outer.inner.prop")).isEmpty();
    }

    @Test
    void test_getEnum_null_strictMode_true() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = null } }").setStrictMode(true);

        final Exception e1 = assertThrows(ConfigException.Null.class, () -> conf.getEnum(Day.class, "outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.Null.class, () -> conf.getConfig().getEnum(Day.class, "outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(conf.getOptionalEnum(Day.class, "outer.inner.prop")).isEmpty();
    }

    /*** Enum set ***/

    @Test
    void test_getEnumSet() {
        final OptionalConfig conf     = createConfiguration("outer { inner { prop = [SUNDAY, MONDAY, TUESDAY] } }");
        final EnumSet<Day>   expected = EnumSet.of(Day.SUNDAY, Day.MONDAY, Day.TUESDAY);

        assertThat(conf.getEnumSet(Day.class, "outer.inner.prop")).isEqualTo(expected);
        assertThat(conf.getOptionalEnumSet(Day.class, "outer.inner.prop")).hasValue(expected);

        assertThat(conf.getConfig().getEnumList(Day.class, "outer.inner.prop")).containsExactlyElementsIn(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = { "outer { inner { prop = 5.3 } }", "outer { inner { prop = a } }", "outer { inner { prop { abc = 1 } } }" })
    void test_getEnumSet_WrongType(final String configuration) {
        final OptionalConfig conf = createConfiguration(configuration);

        final Exception e1 = assertThrows(ConfigException.WrongType.class, () -> conf.getEnumSet(Day.class, "outer.inner.prop"));
        final Exception e3 = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getEnumList(Day.class, "outer.inner.prop"));

        final Exception e2 = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalEnumSet(Day.class, "outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    @Test
    void test_getEnumSet_null_strictMode_false() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = null } }").setStrictMode(false);

        assertThat(conf.getEnumSet(Day.class, "outer.inner.prop")).isNull();
        assertThat(conf.getOptionalEnumSet(Day.class, "outer.inner.prop")).isEmpty();
    }

    @Test
    void test_getEnumSet_null_strictMode_true() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = null } }").setStrictMode(true);

        final Exception e1 = assertThrows(ConfigException.Null.class, () -> conf.getEnumSet(Day.class, "outer.inner.prop"));
        final Exception e2 = assertThrows(ConfigException.Null.class, () -> conf.getConfig().getEnumList(Day.class, "outer.inner.prop"));
        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());

        assertThat(conf.getOptionalEnumSet(Day.class, "outer.inner.prop")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "outer { inner { prop = [SUNDAY, MONDAY, true] } }" })
    void test_getEnumSet_BadValue_element(final String configuration) {
        final OptionalConfig conf = createConfiguration(configuration);

        final Exception e1 = assertThrows(ConfigException.BadValue.class, () -> conf.getEnumSet(Day.class, "outer.inner.prop"));
        final Exception e3 = assertThrows(ConfigException.BadValue.class, () -> conf.getConfig().getEnumList(Day.class, "outer.inner.prop"));

        final Exception e2 = assertThrows(ConfigException.BadValue.class, () -> conf.getOptionalEnumSet(Day.class, "outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = { "outer { inner { prop = [SUNDAY, MONDAY, [SUNDAY, MONDAY]] } }", "outer { inner { prop = [SUNDAY, MONDAY, {a = SUNDAY}] } }" })
    void test_getEnumSet_WrongType_element(final String configuration) {
        final OptionalConfig conf = createConfiguration(configuration);

        final Exception e1 = assertThrows(ConfigException.WrongType.class, () -> conf.getEnumSet(Day.class, "outer.inner.prop"));
        final Exception e3 = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getEnumList(Day.class, "outer.inner.prop"));

        final Exception e2 = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalEnumSet(Day.class, "outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

    @Test
    void test_getEnumSet_null_element_strictMode_false() {
        final OptionalConfig conf     = createConfiguration("outer { inner { prop = [SUNDAY, MONDAY, null, TUESDAY, null] } }").setStrictMode(false);
        final HashSet<Day>   expected = Sets.newHashSet(Day.SUNDAY, Day.MONDAY, null, Day.TUESDAY, null);

        assertThat(conf.getEnumSet(Day.class, "outer.inner.prop")).isEqualTo(expected);
        assertThat(conf.getOptionalEnumSet(Day.class, "outer.inner.prop")).hasValue(expected);
    }

    @Test
    void test_getEnumSet_null_element_strictMode_true() {
        final OptionalConfig conf = createConfiguration("outer { inner { prop = [SUNDAY, MONDAY, null, TUESDAY, null] } }").setStrictMode(true);
        final Exception      e1   = assertThrows(ConfigException.WrongType.class, () -> conf.getEnumSet(Day.class, "outer.inner.prop"));
        final Exception      e2   = assertThrows(ConfigException.WrongType.class, () -> conf.getConfig().getEnumList(Day.class, "outer.inner.prop"));
        final Exception      e3   = assertThrows(ConfigException.WrongType.class, () -> conf.getOptionalEnumSet(Day.class, "outer.inner.prop"));

        assertThat(e1.getMessage()).isEqualTo(e2.getMessage());
        assertThat(e1.getMessage()).isEqualTo(e3.getMessage());
    }

}
