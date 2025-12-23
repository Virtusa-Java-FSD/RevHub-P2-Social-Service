package com.revhub.util;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class ContentParser {
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#(\\w+)");
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");
    public static List<String> extractHashtags(String content) {
        List<String> hashtags = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return hashtags;
        }
        Matcher matcher = HASHTAG_PATTERN.matcher(content);
        while (matcher.find()) {
            String hashtag = matcher.group(1).toLowerCase();
            if (!hashtags.contains(hashtag)) {
                hashtags.add(hashtag);
            }
        }
        return hashtags;
    }
    public static List<String> extractMentionUsernames(String content) {
        List<String> usernames = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return usernames;
        }
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            String username = matcher.group(1);
            if (!usernames.contains(username)) {
                usernames.add(username);
            }
        }
        return usernames;
    }
}
