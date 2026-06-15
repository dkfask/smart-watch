package com.example.demo.socket.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImeiValidatorTest {

    @Test
    void acceptsRealBraceletImei() {
        assertThat(ImeiValidator.isValid("355932600124999")).isTrue();
    }

    @Test
    void acceptsGeneratedTestBraceletRange() {
        assertThat(ImeiValidator.isValid("359999000000001")).isTrue();
    }

    @Test
    void rejectsNumbersParsedFromLocationPayload() {
        assertThat(ImeiValidator.isValid("001450000880000")).isFalse();
        assertThat(ImeiValidator.isValid("001490000990000")).isFalse();
        assertThat(ImeiValidator.isValid("001450000980000")).isFalse();
    }
}
