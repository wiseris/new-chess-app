package com.nargiz.chess.client;

import com.nargiz.chess.client.network.impl.TCPClientImpl;
import com.nargiz.chess.shared.events.ApplicationStopEvent;
import com.nargiz.chess.shared.events.ApplicationEventBus;
import com.nargiz.chess.client.ui.screens.MainScreenController;
import com.nargiz.chess.shared.ioc.Container;
import com.nargiz.chess.shared.ioc.Context;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Launcher extends Application {

    private ApplicationEventBus eventBus;
    private TCPClientImpl tcpClient;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setOnCloseRequest(this::onWindowClose);

        Context context = Container.createContext("com.nargiz.chess");
        context.addNewInstance(Stage.class, primaryStage);

        MainScreenController mainScreen = context.get(MainScreenController.class);
        eventBus = context.get(ApplicationEventBus.class);
        tcpClient = context.get(TCPClientImpl.class);

        primaryStage.setTitle("Chess game");
        primaryStage.show();
        primaryStage.setMaximized(true);

        mainScreen.show();
    }

    private void onWindowClose(WindowEvent event) {
        System.out.println("Window close button (X) clicked - sending Disconnect");

        if (tcpClient != null) {
            tcpClient.exitAbruptly();
        }

        if (eventBus != null) {
            eventBus.publish(new ApplicationStopEvent());
        }

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}