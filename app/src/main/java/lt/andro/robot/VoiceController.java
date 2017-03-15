package lt.andro.robot;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.google.gson.JsonElement;

import java.util.HashMap;

import ai.api.AIListener;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIResponse;
import ai.api.model.Fulfillment;
import ai.api.model.Result;
import hugo.weaving.DebugLog;
import timber.log.Timber;

import static lt.andro.robot.MainActivity.Emotion.SAD_THINKING;

/**
 * @author Vilius Kraujutis
 * @since 2017-03-12
 */
class VoiceController implements AIListener {
    private final Handler handler;
    private MainActivityView view;
    private final AIService aiService;
    private boolean continuousListening = false;

    @DebugLog
    @SuppressWarnings("WeakerAccess")
    public VoiceController(Context context, MainActivityView view) {
        this.view = view;
        AIConfiguration config = new AIConfiguration("5b5862e0d90b4069af746eff2fd57267",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);
        aiService = AIService.getService(context, config);
        aiService.setListener(this);
        handler = new Handler();
    }

    @DebugLog
    @Override
    public void onResult(AIResponse result) {
        Result res = result.getResult();
        if (res != null) {
            onResultReceived(res);
        } else {
            view.showMessage("No result understood");
            view.showEmotion(SAD_THINKING);
        }
    }

    @DebugLog
    private void onResultReceived(@NonNull Result result) {
        Fulfillment fulfillment = result.getFulfillment();

        if (fulfillment != null) {
            onFulfillmentReceived(fulfillment);
        } else {
            Timber.e("Fulfilment not received");
        }

        String action = result.getAction();
        if (action != null) {
            onActionReceived(action, result);
        } else {
            Timber.e("No action received");
        }
    }

    @DebugLog
    private void onActionReceived(@NonNull String action, Result result) {
        switch (action) {
            case "celebrate":
                onCelebrateActionReceived();
                break;
            case "input.unknown":
                break;
            case "input.welcome":
                break;
            case "switch-light":
                onLightSwitchReceived(result);
                break;
            case "drive":
                onDriveActionReceived(result);
                break;
            case "question.whats-my-purpose":
                break;
            default:
                String unknown = "Unknown action " + action;
                view.showMessage(unknown);
                Timber.e(unknown);
        }
    }

    @DebugLog
    private void onLightSwitchReceived(Result result) {
        if (result == null) return;
        HashMap<String, JsonElement> parameters = result.getParameters();
        if (parameters != null) {
            JsonElement stateElement = parameters.get("light-state");
            if (stateElement != null) {
                String state = stateElement.getAsString();
                if (state.equalsIgnoreCase("on")) {
                    view.turnLed(true);
                } else {
                    view.turnLed(false);
                }
            }
        }
    }

    @DebugLog
    private void onCelebrateActionReceived() {
        view.celebrate();
    }

    @DebugLog
    private void onDriveActionReceived(Result result) {
        HashMap<String, JsonElement> params = result.getParameters();
        JsonElement directionElement = params.get("Direction");
        if (directionElement != null) {
            view.executeCommand(directionElement.getAsString());
        } else {
            talkAndShow("Unknown driving direction");
        }
    }

    @DebugLog
    private void talkAndShow(String message) {
        view.speakText(message);
        view.showMessage(message);
    }

    @DebugLog
    private void onFulfillmentReceived(@NonNull Fulfillment fulfillment) {
        talkAndShow(fulfillment.getSpeech());
    }

    @DebugLog
    @Override
    public void onError(AIError error) {
        handler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        continueListening();
                    }
                },
                300
        );
    }

    @Override
    public void onAudioLevel(float level) {

    }

    @DebugLog
    @Override
    public void onListeningStarted() {
        view.showListening(true);
    }

    @DebugLog
    @Override
    public void onListeningCanceled() {
        listen(continuousListening);
    }

    @DebugLog
    @Override
    public void onListeningFinished() {
    }

    @SuppressWarnings("WeakerAccess")
    @DebugLog
    public void onVoiceButtonClicked() {
        listen(!continuousListening);
    }

    @SuppressWarnings("WeakerAccess")
    @DebugLog
    public void listen(boolean listening) {
        if (continuousListening && !listening) {
            view.showMessage("Stop listening");
        }

        continuousListening = listening;

        if (listening) {
            aiService.startListening();
            view.showListening(true);
        } else {
            aiService.stopListening();
            view.showListening(false);
        }
    }

    @SuppressWarnings("WeakerAccess")
    @DebugLog
    public void continueListening() {
        listen(continuousListening);
    }
}
