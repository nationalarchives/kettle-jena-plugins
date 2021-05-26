/*
 * The MIT License
 * Copyright © 2020 The National Archives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package uk.gov.nationalarchives.pdi.step;

import org.junit.jupiter.api.Test;
import uk.gov.nationalarchives.pdi.step.jena.Util;

import javax.xml.namespace.QName;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.nationalarchives.pdi.step.jena.Util.Entry;
import static uk.gov.nationalarchives.pdi.step.jena.Util.Map;

public class UtilTest {

    @Test
    public void nullIfEmpty() {
        assertNull(Util.nullIfEmpty(null));
        assertNull(Util.nullIfEmpty(""));
        assertNotNull(Util.nullIfEmpty("a"));
        assertNotNull(Util.nullIfEmpty("abc"));
    }

    @Test
    public void emptyIfNull() {
        assertTrue(Util.emptyIfNull(null).isEmpty());
        assertTrue(Util.emptyIfNull("").isEmpty());
        assertFalse(Util.emptyIfNull("a").isEmpty());
        assertFalse(Util.emptyIfNull("abc").isEmpty());
    }

    @Test
    public void isNullOrEmptyString() {
        assertTrue(Util.isNullOrEmpty((String)null));
        assertTrue(Util.isNullOrEmpty(""));
        assertFalse(Util.isNullOrEmpty("a"));
        assertFalse(Util.isNullOrEmpty("abc"));
    }

    @Test
    public void isNotEmptyString() {
        assertFalse(Util.isNotEmpty((String)null));
        assertFalse(Util.isNotEmpty(""));
        assertTrue(Util.isNotEmpty("a"));
        assertTrue(Util.isNotEmpty("abc"));
    }

    @Test
    public void isNullOrEmptyList() {
        assertTrue(Util.isNullOrEmpty((List<Object>)null));
        assertTrue(Util.isNullOrEmpty(Collections.emptyList()));
        assertFalse(Util.isNullOrEmpty(Arrays.asList("a")));
        assertFalse(Util.isNullOrEmpty(Arrays.asList("a", "b", "c")));
    }

    @Test
    public void isNotEmptyMap() {
        assertFalse(Util.isNotEmpty((Map<Object,  Object>)null));
        assertFalse(Util.isNotEmpty(Collections.emptyMap()));
        assertTrue(Util.isNotEmpty(Map(Entry("a", "aa"))));
        assertTrue(Util.isNotEmpty(Map(Entry("a", "aa"), Entry("b", "bb"), Entry("c", "cc"))));
    }

    @Test
    public void isNotEmptyList() {
        assertFalse(Util.isNotEmpty((List<Object>)null));
        assertFalse(Util.isNotEmpty(Collections.emptyList()));
        assertTrue(Util.isNotEmpty(Arrays.asList("a")));
        assertTrue(Util.isNotEmpty(Arrays.asList("a", "b", "c")));
    }

    @Test
    public void isNotEmptyArray() {
        assertFalse(Util.isNotEmpty((Object[])null));
        assertFalse(Util.isNotEmpty(new Object[0]));
        assertTrue(Util.isNotEmpty(new String[] {"a"}));
        assertTrue(Util.isNotEmpty(new String[] {"a", "b", "c"}));
    }

    @Test
    public void asPrefixString() {
        assertTrue(Util.asPrefixString(null).isEmpty());
        assertEquals("{http://ns}local-name", Util.asPrefixString(new QName("http://ns", "local-name", "")));
        assertEquals("p1:local-name", Util.asPrefixString(new QName("http://ns", "local-name", "p1")));
    }

    @Test
    public void isQName() {
        assertFalse(Util.isQName(null));
        assertFalse(Util.isQName(""));
        assertFalse(Util.isQName("prefix:"));
        assertFalse(Util.isQName(":local-name"));
        assertFalse(Util.isQName("{ns}"));
        assertFalse(Util.isQName("{ns"));
        assertFalse(Util.isQName("ns}"));
        assertFalse(Util.isQName("}ns{"));
        assertFalse(Util.isQName("}ns{local-name"));
        assertFalse(Util.isQName("invalid{ns}"));

        assertTrue(Util.isQName("prefix:local-name"));
        assertTrue(Util.isQName("{ns}local-name"));
    }

    @Test
    public void parseQName() {
        assertNull(Util.parseQName(null, null));
        assertNull(Util.parseQName(Collections.emptyMap(), null));
        assertThrows(IllegalArgumentException.class, () -> {
            Util.parseQName(null, "prefix:local-part");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            Util.parseQName(Collections.EMPTY_MAP, "prefix:local-part");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            Util.parseQName(null,"prefix:");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            Util.parseQName(null,":local-name");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            Util.parseQName(null,"{ns}");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            Util.parseQName(null,"{ns");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            Util.parseQName(null,"ns}");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            Util.parseQName(null,"}ns{");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            Util.parseQName(null,"}ns{local-name");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            Util.parseQName(null,"invalid{ns}");
        });

        assertEquals(new QName("http://p1", "local-name", "prefix"), Util.parseQName(Map(Entry("prefix", "http://p1")), "prefix:local-name"));
        assertEquals(new QName("http://p1", "local-name", "prefix"), Util.parseQName(Map(Entry("prefix", "http://p1"), Entry("prefix2", "http://p2")), "prefix:local-name"));
        assertEquals(new QName("ns", "local-name"),  Util.parseQName(null, "{ns}local-name"));
        assertEquals(new QName("ns", "local-name"),  Util.parseQName(Collections.emptyMap(), "{ns}local-name"));
        assertEquals(new QName("ns", "local-name"),  Util.parseQName(Map(Entry("prefix", "http://p1")), "{ns}local-name"));
        assertEquals(new QName("local-part"), Util.parseQName(null, "local-part"));
    }

    @Test
    public void copy() {
        assertNull(Util.copy(null));
        assertEquals(new QName("local-part"), new QName("local-part"));
        assertEquals(new QName(null, "local-part", "prefix"), new QName(null, "local-part", "prefix"));
        assertEquals(new QName("ns", "local-part"), new QName("ns", "local-part"));
        assertEquals(new QName("ns", "local-part", "prefix"), new QName("ns", "local-part", "prefix"));
    }

    @Test
    public void map() {
        assertEquals(Collections.emptyMap(), Map(null));
        assertEquals(Collections.emptyMap(), Map());

        final Map<String, String> expected1 = new HashMap<>();
        expected1.put("k1", "v1");
        assertEquals(expected1, Map(Entry("k1", "v1")));

        final Map<String, String> expected2 = new HashMap<>();
        expected2.put("k1", "v1");
        expected2.put("k2", "v2");
        expected2.put("k3", "v3");
        assertEquals(expected2, Map(Entry("k1", "v1"), Entry("k2", "v2"), Entry("k3", "v3")));

        final Map<Integer, Float> expected3 = new HashMap<>();
        expected3.put(1, 0.1f);
        expected3.put(2, 0.2f);
        expected3.put(3, 0.3f);
        assertEquals(expected3, Map(Entry(1, 0.1f), Entry(2, 0.2f), Entry(3, 0.3f)));
    }

}
