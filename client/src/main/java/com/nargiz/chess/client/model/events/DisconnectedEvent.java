package com.nargiz.chess.client.model.events;

import com.nargiz.chess.shared.events.GameEvent;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DisconnectedEvent implements GameEvent {
    private String reason;
    private boolean isRST;
    private boolean isHostDisconnected;
}