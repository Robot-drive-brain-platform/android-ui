package lt.andro.robot;

import android.content.Context;
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
    private Context context;
    private MainActivityView view;
    private final AIConfiguration config;
    private final AIService aiService;

    @DebugLog
    @SuppressWarnings("WeakerAccess")
    public VoiceController(Context context, MainActivityView view) {
        this.context = context;
        this.view = view;
        config = new AIConfiguration("5b5862e0d90b4069af746eff2fd57267",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);
        aiService = AIService.getService(context, config);
        aiService.setListener(this);
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
            onFulfillmentReceived(result, fulfillment);
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
                onLightSwitchReceived(action, result);
                break;
            case "drive":
                onDriveActionReceived(action, result);
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
    private void onLightSwitchReceived(String action, Result result) {
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

    private void onCelebrateActionReceived() {
        view.celebrate();
    }

    private void onDriveActionReceived(@NonNull String action, Result result) {
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
        view.speakText(message, message);
        view.showMessage(message);
    }

    @DebugLog
    private void onFulfillmentReceived(Result result, @NonNull Fulfillment fulfillment) {
        talkAndShow(fulfillment.getSpeech());
    }

    @DebugLog
    @Override
    public void onError(AIError error) {
        view.showListening(false);
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
        view.showListening(false);

    }

    @DebugLog
    @Override
    public void onListeningFinished() {
        view.showListening(false);

    }

    @DebugLog
    public void onVoiceButtonClicked() {
        aiService.startListening();
        view.showListening(true);
    }
}
