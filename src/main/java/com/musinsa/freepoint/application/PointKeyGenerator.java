package com.musinsa.freepoint.application;

import com.musinsa.freepoint.common.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class PointKeyGenerator {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int SUFFIX_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TimeProvider timeProvider;

    public String generate() {
        String timestamp = timeProvider.now().format(TIMESTAMP_FORMAT);
        return "PNT-" + timestamp + "-" + randomSuffix();
    }

    private static String randomSuffix() {
        char[] buf = new char[SUFFIX_LENGTH];
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            buf[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
        }
        return new String(buf);
    }
}
