package com.nargiz.chess.client.process.impl;

import com.nargiz.chess.client.model.events.ErrorEvent;
import com.nargiz.chess.client.process.ClientCommandProcessor;
import com.nargiz.chess.shared.command.response.ErrorResponse;
import com.nargiz.chess.shared.events.ApplicationEventBus;
import com.nargiz.chess.shared.ioc.anotation.Component;
import com.nargiz.chess.shared.ioc.anotation.Inject;

@Component
public class ErrorResponseProcessor implements ClientCommandProcessor<ErrorResponse> {
    @Inject
    ApplicationEventBus eventBus;

    @Override
    public void processCommand(ErrorResponse command) {
        System.out.println("ErrorResponse received: " + command.getMessage());
        eventBus.publish(new ErrorEvent(command.getMessage()));
    }

    @Override
    public Class<ErrorResponse> getCommandClass() {
        return ErrorResponse.class;
    }
}