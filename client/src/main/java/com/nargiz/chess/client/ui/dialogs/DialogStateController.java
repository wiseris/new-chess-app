package com.nargiz.chess.client.ui.dialogs;

import com.nargiz.chess.shared.ioc.anotation.Component;
import com.nargiz.chess.shared.ioc.anotation.PostConstruct;
import com.nargiz.chess.shared.models.enums.GameState;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

@Component
public class DialogStateController extends DialogController {

    @FXML
    Label stateLabel;

    @Override
    protected String getFormUrl() {
        return "/forms/GameResultDialog.fxml";
    }

    @PostConstruct
    @Override
    public void loadScene() {
        super.loadScene();
    }

    public void setState(GameState state) {
        Platform.runLater(() -> {
            stateLabel.setText(
                    switch (state) {
                        case WHITE_WINS -> "Белые выиграли";
                        case BLACK_WINS -> "Черные выиграли";
                        case DRAW -> "Ничья";
                        case PLAYING -> "Игра идет";
                    }
            );
        });
    }

}
