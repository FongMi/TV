package com.hiker.drpy;

import android.text.TextUtils;

import androidx.media3.common.util.UriUtil;

import com.hiker.drpy.bean.Cache;
import com.hiker.drpy.bean.Info;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

    private final Pattern p1 = Pattern.compile("url\\((.*?)\\)", Pattern.MULTILINE | Pattern.DOTALL);
    private final Pattern p2 = Pattern.compile(":eq|:lt|:gt|:first|:last|^body$|^#");
    private final Pattern p3 = Pattern.compile("(url|src|href|-original|-src|-play|-url)$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    private final Cache cache;

    public Parser() {
        cache = new Cache();
    }

    private Info getParseInfo(String rule) {
        Info info = new Info(rule);
        if (rule.contains(":eq")) {
            info.setRule(rule.split(":")[0]);
            info.setInfo(rule.split(":")[1]);
        } else if (rule.contains("--")) {
            String[] rules = rule.split("--");
            info.setExcludes(rules);
            info.setRule(rules[0]);
        }
        return info;
    }

    private String parseHikerToJq(String parse, boolean first) {
        if (!parse.contains("&&")) {
            String[] split = parse.split(" ");
            return (p2.matcher(split[split.length - 1]).find() || !first) ? parse : parse + ":eq(0)";
        }
        String[] parses = parse.split("&&");
        List<String> items = new ArrayList<>();
        for (int i = 0; i < parses.length; i++) {
            String[] split = parses[i].split(" ");
            if (p2.matcher(split[split.length - 1]).find()) {
                items.add(parses[i]);
            } else {
                if (!first && i >= parses.length - 1) items.add(parses[i]);
                else items.add(parses[i] + ":eq(0)");
            }
        }
        return TextUtils.join(" ", items);
    }

    private Elements parseOneRule(Document doc, String parse, Elements elements) {
        Info info = getParseInfo(parse);
        if (parse.contains(":eq")) {
            if (elements.isEmpty()) {
                if (info.index < 0) {
                    Elements r = doc.select(info.rule);
                    elements = r.eq(r.size() + info.index);
                } else {
                    elements = doc.select(info.rule).eq(info.index);
                }
            } else {
                if (info.index < 0) {
                    Elements r = elements.select(info.rule);
                    elements = r.eq(r.size() + info.index);
                } else {
                    elements = elements.select(info.rule).eq(info.index);
                }
            }
        } else {
            if (elements.isEmpty()) {
                elements = doc.select(parse);
            } else {
                elements = elements.select(parse);
            }
        }
        if (info.excludes != null && !elements.isEmpty()) {
            elements = elements.clone();
            for (String exclude : info.excludes) {
                elements.select(exclude).remove();
            }
        }
        return elements;
    }

    public String joinUrl(String parent, String child) {
        try {
            return UriUtil.resolve(parent, child);
        } catch (Throwable e) {
            return "";
        }
    }

    public List<String> pdfa(String html, String rule) {
        Document doc = cache.getPdfa(html);
        rule = parseHikerToJq(rule, false);
        String[] parses = rule.split(" ");
        Elements elements = new Elements();
        for (String parse : parses) {
            elements = parseOneRule(doc, parse, elements);
            if (elements.isEmpty()) return Collections.emptyList();
        }
        List<String> items = new ArrayList<>();
        for (Element element : elements) items.add(element.outerHtml());
        return items;
    }

    public String pdfh(String html, String rule, String addUrl) {
        Document doc = cache.getPdfh(html);
        if (rule.equals("body&&Text") || rule.equals("Text")) {
            return doc.text();
        } else if (rule.equals("body&&Html") || rule.equals("Html")) {
            return doc.html();
        }
        String option = "";
        if (rule.contains("&&")) {
            String[] rs = rule.split("&&");
            option = rs[rs.length - 1];
            List<String> excludes = new ArrayList<>(Arrays.asList(rs));
            excludes.remove(rs.length - 1);
            rule = TextUtils.join("&&", excludes);
        }
        rule = parseHikerToJq(rule, true);
        String[] parses = rule.split(" ");
        Elements elements = new Elements();
        for (String parse : parses) {
            elements = parseOneRule(doc, parse, elements);
            if (elements.isEmpty()) return "";
        }
        if (TextUtils.isEmpty(option)) return elements.outerHtml();
        if (option.equals("Text")) {
            return elements.text();
        } else if (option.equals("Html")) {
            return elements.html();
        } else {
            String result = elements.attr(option);
            if (option.toLowerCase().contains("style") && result.contains("url(")) {
                Matcher matcher = p1.matcher(result);
                if (matcher.find()) result = matcher.group(1);
            }
            if (!TextUtils.isEmpty(result) && !TextUtils.isEmpty(addUrl) && p3.matcher(option).find()) {
                if (result.contains("http")) result = result.substring(result.indexOf("http"));
                else result = joinUrl(addUrl, result);
            }
            return result;
        }
    }

    public List<String> pdfl(String html, String rule, String texts, String urls, String urlKey) {
        String[] parses = parseHikerToJq(rule, false).split(" ");
        Elements elements = new Elements();
        for (String parse : parses) {
            elements = parseOneRule(cache.getPdfa(html), parse, elements);
            if (elements.isEmpty()) return Collections.emptyList();
        }
        List<String> items = new ArrayList<>();
        for (Element element : elements) {
            html = element.outerHtml();
            items.add(pdfh(html, texts, "").trim() + '$' + pdfh(html, urls, urlKey));
        }
        return items;
    }
}
