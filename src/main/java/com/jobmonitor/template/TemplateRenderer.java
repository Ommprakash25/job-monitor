package com.jobmonitor.template;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.web.util.HtmlUtils;

/**
 * Fills a template for one recruiter.
 * <ul>
 *   <li>{name}, {company} and {role} are replaced, with fallbacks when blank.</li>
 *   <li>**text** becomes bold in the HTML version and plain in the text version.</li>
 *   <li>Web addresses become clickable links.</li>
 *   <li>[square brackets] and unknown {placeholders} are reported as warnings.</li>
 * </ul>
 */
public final class TemplateRenderer {

    public static final String NAME_FALLBACK = "Hiring Team";
    public static final String COMPANY_FALLBACK = "your company";

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z_][A-Za-z0-9_]*)}");
    private static final Pattern BRACKET = Pattern.compile("\\[[^\\[\\]\\r\\n]{1,80}]");
    private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern LINK = Pattern.compile("(?:https?://|www\\.)[^\\s<]+");
    private static final Pattern PARAGRAPH_BREAK = Pattern.compile("\\R[ \\t]*\\R(?:[ \\t]*\\R)*");
    private static final String TRAILING_PUNCTUATION = ".,;:!?)";

    public RenderedEmail render(String subject, String body, PlaceholderValues values) {
        String rawSubject = subject == null ? "" : subject;
        String rawBody = body == null ? "" : body;
        Map<String, String> fills = Map.of(
                "name", firstNonBlank(values.name(), NAME_FALLBACK),
                "company", firstNonBlank(values.company(), COMPANY_FALLBACK),
                "role", firstNonBlank(values.role(), firstNonBlank(values.roleFallback(), "")));

        Set<String> unknown = new TreeSet<>();
        String filledSubject = fill(rawSubject, fills, unknown);
        String filledBody = fill(rawBody, fills, unknown);

        List<String> warnings = new ArrayList<>();
        for (String placeholder : unknown) {
            warnings.add("{" + placeholder + "} isn't a known placeholder, so it's sent exactly as typed. "
                    + "Use {name}, {company} or {role}.");
        }
        List<String> brackets = findBrackets(rawSubject + "\n" + rawBody);
        if (!brackets.isEmpty()) {
            warnings.add("Still to fill in: " + String.join(", ", brackets) + ".");
        }

        String subjectText = unbold(filledSubject).replaceAll("\\s+", " ").strip();
        return new RenderedEmail(subjectText, unbold(filledBody).strip(), toHtml(filledBody), List.copyOf(warnings));
    }

    private static String fill(String text, Map<String, String> fills, Set<String> unknown) {
        Matcher matcher = PLACEHOLDER.matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String value = fills.get(matcher.group(1).toLowerCase(Locale.ROOT));
            if (value == null) {
                unknown.add(matcher.group(1));
                value = matcher.group();
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String unbold(String text) {
        return BOLD.matcher(text).replaceAll("$1");
    }

    static String toHtml(String text) {
        String html = HtmlUtils.htmlEscape(text.strip());
        html = BOLD.matcher(html).replaceAll("<strong>$1</strong>");
        html = linkify(html);
        StringBuilder out = new StringBuilder("<div dir=\"ltr\">");
        for (String paragraph : PARAGRAPH_BREAK.split(html)) {
            if (!paragraph.isBlank()) {
                out.append("<p>").append(paragraph.strip().replaceAll("\\R", "<br>")).append("</p>");
            }
        }
        return out.append("</div>").toString();
    }

    private static String linkify(String escapedHtml) {
        Matcher matcher = LINK.matcher(escapedHtml);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String url = matcher.group();
            StringBuilder trailing = new StringBuilder();
            while (!url.isEmpty() && TRAILING_PUNCTUATION.indexOf(url.charAt(url.length() - 1)) >= 0) {
                trailing.insert(0, url.charAt(url.length() - 1));
                url = url.substring(0, url.length() - 1);
            }
            String href = url.startsWith("www.") ? "https://" + url : url;
            String link = "<a href=\"" + href + "\">" + url + "</a>" + trailing;
            matcher.appendReplacement(out, Matcher.quoteReplacement(link));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static List<String> findBrackets(String text) {
        Matcher matcher = BRACKET.matcher(text);
        Set<String> found = new LinkedHashSet<>();
        while (matcher.find()) {
            found.add(matcher.group());
        }
        return List.copyOf(found);
    }

    private static String firstNonBlank(String value, String fallback) {
        return value != null && !value.isBlank() ? value.strip() : fallback;
    }
}
