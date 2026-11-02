package com.yavanika.urlshortener;

import com.yavanika.urlshortener.util.Base62Encoder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Base62EncoderTest {

    @Test
    void encodesKnownValues() {
        assertEquals("0", Base62Encoder.encode(0));
        assertEquals("Z", Base62Encoder.encode(61));
        assertEquals("10", Base62Encoder.encode(62));
    }

    @Test
    void encodeAndDecodeAreInverse() {
        for (long v : new long[]{1, 99, 100_000, 3_844_000, Long.MAX_VALUE / 2}) {
            assertEquals(v, Base62Encoder.decode(Base62Encoder.encode(v)));
        }
    }

    @Test
    void rejectsNegative() {
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.encode(-1));
    }
}
