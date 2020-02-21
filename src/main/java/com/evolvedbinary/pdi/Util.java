package com.evolvedbinary.pdi;

import org.springframework.security.access.method.P;

import javax.xml.namespace.QName;
import java.util.Map;

public class Util {

    public static String nullIfEmpty(final String s) {
        if (s != null && !s.isEmpty()) {
            return s;
        } else {
            return null;
        }
    }

    public static String emptyIfNull(final String s) {
        if (s == null) {
            return "";
        } else {
            return s;
        }
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

    public static QName parseQName(final Map<String, String> namespaces, final String qname) {
        if (qname == null) {
            return null;
        }

        int idxColon = qname.indexOf(':');

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
}
