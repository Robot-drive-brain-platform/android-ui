package lt.andro.robot;

public interface LightsListenerPresenter {
    void showListening(boolean listening);

    void onTurnOnLightsCommandReceived();

    void onTurnOffLightsCommandReceived();

    void onError(Throwable throwable);
}
