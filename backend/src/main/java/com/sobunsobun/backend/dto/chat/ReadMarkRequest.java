package com.sobunsobun.backend.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReadMarkRequest {
    private Long roomId;
    private Long lastReadMessageId;
}
