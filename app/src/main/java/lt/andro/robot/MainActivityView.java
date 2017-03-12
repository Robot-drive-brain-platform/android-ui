package lt.andro.robot;

import lt.andro.robot.MainActivity.Emotion;

/**
 * @author Vilius Kraujutis
 * @since 2017-03-12
 */
interface MainActivityView {
    void showMessage(String message);

    void showEmotion(@Emotion int emotion);

    void executeCommand(String text);

    void showListening(boolean listening);

    void speakText(String message, String id);

    void celebrate();

    void turnLed(boolean ledLightingState);
}
