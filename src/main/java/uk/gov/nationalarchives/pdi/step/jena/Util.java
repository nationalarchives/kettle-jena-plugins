/**
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
package uk.gov.nationalarchives.pdi.step.jena;

import org.pentaho.di.i18n.BaseMessages;
import uk.gov.nationalarchives.pdi.step.jena.model.JenaModelStepMeta;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Util {

    public static final String BLANK_NODE_NAME =
            BaseMessages.getString(JenaModelStepMeta.class, "JenaModelStepDialog.TabBNode");

    public static final String BLANK_NODE_INTERNAL_URI = "tag:nationalarchives.gov.uk,2020-07-16:kettle-jena-plugins#bNode";

    public static final Pattern BLANK_NODE_ID_PATTERN =
            Pattern.compile(BLANK_NODE_NAME + ":([0-9]+)");

    public static final String BLANK_NODE_FIELD_NAME = "<N/A: " + BLANK_NODE_NAME + ">";

    /**
     * Given a String, returns null if the String is empty, or
     * otherwise the String.
     *
     * @param s a String
     *
     * @return a non-empty String, else null
     */
    public static @Nullable String nullIfEmpty(@Nullable final String s) {
        if (s != null && !s.isEmpty()) {
            return s;
        } else {
            return null;
        }
    }

    /**
     * Given a String, returns an empty String if the string is null, or
     * otherwise the String.
     *
     * @param s a String
     *
     * @return an empty String if {@code s == null}, else a string.
     */
    public static String emptyIfNull(@Nullable final String s) {
        if (s == null) {
            return "";
        } else {
            return s;
        }
    }

    /**
     * Returns true if the String is either null or empty.
     *
     * @param s a String
     *
     * @return true if the String is null or empty, false otherwise.
     */
    public static boolean isNullOrEmpty(@Nullable final String s) {
        return s == null || s.isEmpty();
    }

    /**
     * Returns true if the String is not-empty (or null).
     *
     * @param s a String
     *
     * @return true if the String is not null and not empty, false otherwise.
     */
    public static boolean isNotEmpty(@Nullable final String s) {
        return s != null && !s.isEmpty();
    }

    /**
     * Returns true if the List is either null or empty.
     *
     * @param l a List
     *
     * @return true if the List is null or empty, false otherwise.
     */
    public static <T> boolean isNullOrEmpty(@Nullable final List<T> l) {
        return l == null || l.isEmpty();
    }

    /**
     * Returns true if the Map is not-empty (or null).
     *
     * @param m a Map
     *
     * @return true if the Map is not null and not empty, false otherwise.
     */
    public static <K,V> boolean isNotEmpty(@Nullable final Map<K, V> m) {
        return m != null && !m.isEmpty();
    }

    /**
     * Returns true if the List is not-empty (or null).
     *
     * @param l a List
     *
     * @return true if the List is not null and not empty, false otherwise.
     */
    public static <T> boolean isNotEmpty(@Nullable final List<T> l) {
        return l != null && !l.isEmpty();
    }

    /**
     * Returns true if the Array is not-empty (or null).
     *
     * @param a an Array
     *
     * @return true if the Array is not null and not empty, false otherwise.
     */
    public static <T> boolean isNotEmpty(@Nullable final T[] a) {
        return a != null && a.length > 0;
    }

    public static String asPrefixString(final QName qname) {
        if (qname == null) {
            return "";
        } else if (qname.getPrefix() != null && !qname.getPrefix().isEmpty()) {
            return qname.getPrefix() + ":" + qname.getLocalPart();
        } else {
            return qname.toString();
        }
    }

    public static boolean isQName(final String qname) {
        if (emptyIfNull(qname) == null) {
            return false;
        }

        final int idxColon = qname.indexOf(':');
        if (idxColon > -1 && idxColon < qname.length() - 1) {
            return true;
        }

        final int idxOpenBrace = qname.indexOf('{');
        final int idxCloseBrace = qname.indexOf('}');
        return (idxOpenBrace > -1 && idxCloseBrace > idxOpenBrace && idxCloseBrace < qname.length() - 1);
    }

    public static QName parseQName(final Map<String, String> namespaces, final String qname) {
        if (qname == null) {
            return null;
        }

        final int idxColon = qname.indexOf(':');

        if (idxColon > -1 && idxColon < qname.length() - 1) {
            final String prefix = qname.substring(0, idxColon);
            final String localPart = qname.substring(idxColon + 1);

            if (namespaces == null) {
                throw new IllegalStateException("No namespaces for prefix lookup");
            }

            final String uri = namespaces.get(prefix);
            return new QName(uri, localPart, prefix);

        } else {
            final int idxOpenBrace = qname.indexOf('{');
            final int idxCloseBrace = qname.indexOf('}');
            if (idxOpenBrace > -1 && idxCloseBrace > idxOpenBrace && idxCloseBrace < qname.length() - 1) {
                final String ns = qname.substring(idxOpenBrace + 1, idxCloseBrace);
                final String localPart = qname.substring(idxCloseBrace + 1);

                //TODO(AR) should we auto number a prefix, e.g. ns1, ns2, etc?

                return new QName(ns, localPart);

            } else {
                return new QName(qname);
            }
        }
    }

    public static QName copy(final QName qname) {
        if (qname == null) {
            return null;
        }
        return new QName(qname.getNamespaceURI(), qname.getLocalPart(), qname.getPrefix());
    }
}
