package common.dto.s3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartjam.common.dto.s3.S3EventDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class S3EventDtoTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserializeMinioJsonWithExtraFieldsCorrectly() throws Exception {
        String json = """
                {
                    "EventName": "s3:ObjectCreated:Put",
                    "Key": "references/Digital Zombies.mp3",
                    "Records": [
                        {
                            "eventName": "s3:ObjectCreated:Put",
                            "s3": {
                                "bucket": {
                                    "name": "references",
                                    "arn": "arn:aws:s3:::references"
                                },
                                "object": {
                                    "key": "Digital+Zombies.mp3",
                                    "size": 7226086
                                }
                            }
                        }
                    ]
                }
                """;

        S3EventDto dto = objectMapper.readValue(json, S3EventDto.class);

        assertEquals(1, dto.records().size());
        assertEquals("references", dto.records().getFirst().s3().bucket().name());

        assertEquals(
                "Digital+Zombies.mp3", dto.records().getFirst().s3().object().key());
    }
}
