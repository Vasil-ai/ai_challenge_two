package com.events.platform.web;

import com.events.platform.domain.EventLifecycle;
import com.events.platform.repo.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class ShareMvcController {

    private final EventRepository eventRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @GetMapping(value = "/share/event/{id}", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String shareEvent(@PathVariable Long id) {
        var ev =
                eventRepository
                        .findById(id)
                        .filter(e -> e.getLifecycle() == EventLifecycle.PUBLISHED && !e.isModerationHidden())
                        .orElse(null);
        if (ev == null) {
            return "<!DOCTYPE html><html><head><title>Event</title></head><body>Not found</body></html>";
        }
        String title = escapeHtml(ev.getTitle());
        String desc = escapeHtml(ev.getDescription() != null ? ev.getDescription().substring(0, Math.min(200, ev.getDescription().length())) : "");
        String image =
                ev.getCoverImageUrl() != null && ev.getCoverImageUrl().startsWith("http")
                        ? ev.getCoverImageUrl()
                        : baseUrl.replaceAll("/$", "")
                                + (ev.getCoverImageUrl() != null ? ev.getCoverImageUrl() : "");
        String url = baseUrl.replaceAll("/$", "") + "/events/" + id;
        return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"/><title>"
                + title
                + "</title><meta property=\"og:title\" content=\""
                + title
                + "\"/><meta property=\"og:description\" content=\""
                + desc
                + "\"/><meta property=\"og:image\" content=\""
                + escapeHtml(image)
                + "\"/><meta property=\"og:url\" content=\""
                + escapeHtml(url)
                + "\"/><meta name=\"twitter:card\" content=\"summary_large_image\"/><meta http-equiv=\"refresh\" content=\"0; url="
                + url
                + "\"/></head><body><p><a href=\""
                + url
                + "\">Open event</a></p></body></html>";
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
