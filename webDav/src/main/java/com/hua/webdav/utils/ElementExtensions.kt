package com.hua.webdav.utils

import org.jsoup.nodes.Element
import org.jsoup.select.Elements

fun Element.findNS(tag: String, namespace: HashSet<String>): Elements {
    return select("*|$tag").filter { el ->
        namespace.contains(el.tagName().substringBefore(":"))
    }.toElements()
}

fun Element.findNSPrefix(namespaceURI: String): HashSet<String> {
    return select("[^xmlns:]").map { element ->
        element.attributes().filter { it.value == namespaceURI }.map { it.key.substring(6) }
    }.flatten().toHashSet()
}

fun List<Element>.toElements() = Elements(this)