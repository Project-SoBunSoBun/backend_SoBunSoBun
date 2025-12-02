package com.sobunsobun.backend.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LeaveChatRoomsRequest {

    private List<Long> roomIds;
}
