package com.nargiz.chess.client;

import com.nargiz.chess.client.network.TCPClient;
import com.nargiz.chess.shared.events.ApplicationStopEvent;
import com.nargiz.chess.shared.events.ApplicationEventBus;
import com.nargiz.chess.client.ui.screens.MainScreenController;
import com.nargiz.chess.shared.ioc.Container;
import com.nargiz.chess.shared.ioc.Context;
import javafx.application.Application;
import javafx.stage.Stage;

public class Launcher extends Application {

    ApplicationEventBus eventBus;

    @Override
    public void start(Stage primaryStage) {
        Context context = Container.createContext("com.nargiz.chess");
        context.addNewInstance(Stage.class, primaryStage);

        MainScreenController mainScreen = context.get(MainScreenController.class);
        eventBus = context.get(ApplicationEventBus.class);

        primaryStage.setTitle("Chess game");

        primaryStage.setOnCloseRequest(event -> {
            TCPClient tcpClient = context.get(TCPClient.class);
            if (tcpClient != null) {
                tcpClient.stop();
            }
            eventBus.publish(new ApplicationStopEvent());
        });

        primaryStage.show();
        primaryStage.setMaximized(true);

        mainScreen.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}