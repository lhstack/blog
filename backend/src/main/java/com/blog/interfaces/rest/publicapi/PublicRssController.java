package com.blog.interfaces.rest.publicapi;

import com.blog.application.site.RssFeedApplicationService;
import com.blog.application.site.RssFeedApplicationService.FeedView;
import com.blog.domain.article.ArticleFeedEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

@RestController
public class PublicRssController {

    private static final String RSS_TYPE = "application/rss+xml";
    private static final String ATOM_NS = "http://www.w3.org/2005/Atom";
    private static final MediaType RSS_UTF8 = MediaType.parseMediaType(RSS_TYPE + ";charset=UTF-8");
    private static final CacheControl FEED_CACHE = CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic().mustRevalidate();
    private static final Pattern INVALID_XML_CHARS = Pattern.compile("[^\\x09\\x0A\\x0D\\x20-\\uD7FF\\uE000-\\uFFFD\\x{10000}-\\x{10FFFF}]");

    private final RssFeedApplicationService feedService;
    private final URI siteRoot;

    public PublicRssController(RssFeedApplicationService feedService, @Value("${blog.site.url}") String siteUrl) {
        this.feedService = feedService;
        this.siteRoot = parseSiteRoot(siteUrl);
    }

    @GetMapping("/rss.xml")
    public ResponseEntity<byte[]> rss() throws XMLStreamException {
        byte[] body = render(feedService.latest());
        return ResponseEntity.ok()
                .contentType(RSS_UTF8)
                .cacheControl(FEED_CACHE)
                .eTag(DigestUtils.md5DigestAsHex(body))
                .body(body);
    }

    private byte[] render(FeedView feed) throws XMLStreamException {
        var output = new ByteArrayOutputStream();
        XMLStreamWriter xml = XMLOutputFactory.newDefaultFactory().createXMLStreamWriter(output, StandardCharsets.UTF_8.name());
        try {
            xml.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
            xml.writeStartElement("rss");
            xml.writeAttribute("version", "2.0");
            xml.writeNamespace("atom", ATOM_NS);
            xml.writeStartElement("channel");
            text(xml, "title", feed.title());
            text(xml, "link", siteRoot.toASCIIString());
            description(xml, feed.description());
            xml.writeEmptyElement("atom", "link", ATOM_NS);
            xml.writeAttribute("href", siteRoot.resolve("rss.xml").toASCIIString());
            xml.writeAttribute("rel", "self");
            xml.writeAttribute("type", RSS_TYPE);
            for (ArticleFeedEntry entry : feed.entries()) {
                writeItem(xml, entry);
            }
            xml.writeEndDocument();
            xml.flush();
        } finally {
            xml.close();
        }
        return output.toByteArray();
    }

    private void writeItem(XMLStreamWriter xml, ArticleFeedEntry entry) throws XMLStreamException {
        String url = siteRoot.resolve("articles/" + entry.id()).toASCIIString();
        xml.writeStartElement("item");
        text(xml, "title", entry.title());
        text(xml, "link", url);
        text(xml, "guid", url); // RSS 2.0 默认将 guid 视为永久链接。
        if (entry.summary() != null && !entry.summary().isBlank()) {
            description(xml, entry.summary());
        }
        if (entry.publishedAt() != null) {
            // 文章时间按 JVM 本地时间写入，转换为 RSS 阅读器可解析的 GMT 日期。
            var utc = entry.publishedAt().atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC);
            text(xml, "pubDate", DateTimeFormatter.RFC_1123_DATE_TIME.format(utc));
        }
        xml.writeEndElement();
    }

    private static void description(XMLStreamWriter xml, String value) throws XMLStreamException {
        // 阅读器可能将 description 当作 HTML，保留摘要的纯文本语义。
        text(xml, "description", HtmlUtils.htmlEscape(value, StandardCharsets.UTF_8.name()));
    }

    private static void text(XMLStreamWriter xml, String name, String value) throws XMLStreamException {
        xml.writeStartElement(name);
        xml.writeCharacters(INVALID_XML_CHARS.matcher(value).replaceAll(""));
        xml.writeEndElement();
    }

    private static URI parseSiteRoot(String siteUrl) {
        URI uri = URI.create(siteUrl);
        String scheme = uri.getScheme();
        if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                || uri.getHost() == null || uri.getPort() > 65535 || uri.getUserInfo() != null
                || uri.getRawQuery() != null || uri.getRawFragment() != null
                || !(uri.getRawPath().isEmpty() || "/".equals(uri.getRawPath()))) {
            throw new IllegalArgumentException("blog.site.url 必须是站点的绝对 HTTP(S) 根地址");
        }
        return uri.resolve("/");
    }
}
