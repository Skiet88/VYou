package za.ac.skiet.vyou2.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class AesGcmStringConverterTest {

    private static final String KEY = Base64.getEncoder().encodeToString("01234567890123456789012345678901".getBytes());

    private final AesGcmStringConverter converter = new AesGcmStringConverter(KEY);

    @Test
    void roundTripsThroughEncryptionAndDecryption() {
        String plaintext = "I built a REST API using Spring Boot and PostgreSQL.";
        String ciphertext = converter.convertToDatabaseColumn(plaintext);

        assertThat(ciphertext).isNotEqualTo(plaintext).doesNotContain("REST API");
        assertThat(converter.convertToEntityAttribute(ciphertext)).isEqualTo(plaintext);
    }

    @Test
    void encryptingTheSameValueTwiceGivesDifferentCiphertext() {
        String a = converter.convertToDatabaseColumn("same value");
        String b = converter.convertToDatabaseColumn("same value");
        assertThat(a).isNotEqualTo(b); // random IV per call
    }

    @Test
    void nullPassesThrough() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void rejectsAKeyThatIsNotThirtyTwoBytes() {
        String shortKey = Base64.getEncoder().encodeToString("too-short".getBytes());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new AesGcmStringConverter(shortKey))
                .isInstanceOf(IllegalStateException.class);
    }
}
