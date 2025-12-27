package org.slowcoders.hyperql.sample.hq.bookstore;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class UserDto {
    private Long id;
    private String email;
    private String name;
    private OffsetDateTime createdAt;

}



