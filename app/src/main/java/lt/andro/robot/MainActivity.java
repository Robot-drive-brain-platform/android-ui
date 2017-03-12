package lt.andro.robot;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.appindexing.Indexable;
import com.google.firebase.appindexing.builders.Indexables;
import com.google.firebase.appindexing.builders.PersonBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.lang.annotation.Retention;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;
import hugo.weaving.DebugLog;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static java.lang.annotation.RetentionPolicy.SOURCE;
import static lt.andro.robot.MainActivity.BluetoothCommandRepeat.COUNT_LONG;
import static lt.andro.robot.MainActivity.Direction.AROUND;
import static lt.andro.robot.MainActivity.Direction.BACK;
import static lt.andro.robot.MainActivity.Direction.LEFT;
import static lt.andro.robot.MainActivity.Direction.LEFT_QUICK;
import static lt.andro.robot.MainActivity.Direction.NORTH_EAST;
import static lt.andro.robot.MainActivity.Direction.NORTH_WEST;
import static lt.andro.robot.MainActivity.Direction.RIGHT;
import static lt.andro.robot.MainActivity.Direction.RIGHT_QUICK;
import static lt.andro.robot.MainActivity.Direction.SOUTH_EAST;
import static lt.andro.robot.MainActivity.Direction.SOUTH_WEST;
import static lt.andro.robot.MainActivity.Direction.UPWARD;
import static lt.andro.robot.MainActivity.Emotion.EXCLAMATION;
import static lt.andro.robot.MainActivity.Emotion.HAPPY;
import static lt.andro.robot.MainActivity.Emotion.MUSIC;
import static lt.andro.robot.MainActivity.Emotion.NEUTRAL;
import static lt.andro.robot.MainActivity.Emotion.SAD;
import static lt.andro.robot.MainActivity.Emotion.SAD_THINKING;
import static lt.andro.robot.MainActivity.Emotion.TALKING;
import static lt.andro.robot.MainActivity.Emotion.VERY_HAPPY;
import static lt.andro.robot.MainActivity.RobotCommand.COMMAND_BACK;
import static lt.andro.robot.MainActivity.RobotCommand.COMMAND_FLAG_OFF;
import static lt.andro.robot.MainActivity.RobotCommand.COMMAND_FLAG_ON;
import static lt.andro.robot.MainActivity.RobotCommand.COMMAND_FORWARD;
import static lt.andro.robot.MainActivity.RobotCommand.COMMAND_LED_OFF;
import static lt.andro.robot.MainActivity.RobotCommand.COMMAND_LED_ON;
import static lt.andro.robot.MainActivity.RobotCommand.COMMAND_LEFT;
import static lt.andro.robot.MainActivity.RobotCommand.COMMAND_LEFT_QUICK;
import static lt.andro.robot.MainActivity.RobotCommand.COMMAND_NORTH_EAST;
import static lt.andro.robot.MainActivity.RobotCommand.COMMAND_NORTH_WEST;
import static lt.andro.robot.MainActivity.RobotCommand.COMMAND_RIGHT;
import static lt.andro.robot.MainActivity.RobotCommand.COMMAND_RIGHT_QUICK;
import static lt.andro.robot.MainActivity.RobotCommand.COMMAND_SOUTH_EAST;
import static lt.andro.robot.MainActivity.RobotCommand.COMMAND_SOUTH_WEST;
import static lt.andro.robot.MainActivity.RobotCommand.COMMAND_STOP;
import static lt.andro.robot.MainActivity.RobotCommand.SPEED_NORMAL;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        MainActivityView,
        BluetoothSPP.BluetoothStateListener,
        BluetoothSPP.OnDataReceivedListener {

    public static final String RECORD_AUDIO_PERMISSION = Manifest.permission.RECORD_AUDIO;
    public static final int FLAG_SHAKE_DELAY = 800;
    public static final int DRIVE_DELAY = 1000;

    @BindView(R.id.main_voice_button)
    ImageButton voiceButton;
    @BindView(R.id.main_face)
    ImageView faceView;

    private static final int DELAY_LONG = 1500;

    private TextToSpeech tts;
    private BluetoothSPP bt;
    private Handler handler;
    private PermissionsController permissionsController;
    private VoiceController voiceController;

    @Retention(SOURCE)
    @IntDef({
            Emotion.HAPPY,
            Emotion.NEUTRAL,
            Emotion.SAD,
            MUSIC,
            EXCLAMATION,
            SAD_THINKING,
            TALKING,
            VERY_HAPPY,
    })
    public @interface Emotion {
        int HAPPY = 201;
        int NEUTRAL = 202;
        int SAD = 203;
        int MUSIC = 204;
        int EXCLAMATION = 205;
        int SAD_THINKING = 206;
        int TALKING = 207;
        int VERY_HAPPY = 208;
    }

    @Retention(SOURCE)
    @IntDef({
            NORTH_WEST,
            UPWARD,
            NORTH_EAST,
            LEFT_QUICK,
            LEFT,
            AROUND,
            RIGHT_QUICK,
            RIGHT,
            SOUTH_WEST,
            BACK,
            SOUTH_EAST,
    })
    public @interface Direction {
        int NORTH_WEST = 301;
        int UPWARD = 302;
        int NORTH_EAST = 303;
        int LEFT_QUICK = 304;
        int LEFT = 3041;
        int AROUND = 305;
        int RIGHT_QUICK = 306;
        int RIGHT = 3061;
        int SOUTH_WEST = 307;
        int BACK = 308;
        int SOUTH_EAST = 309;
    }

    @Retention(SOURCE)
    @StringDef({
            COMMAND_NORTH_WEST,
            COMMAND_FORWARD,
            COMMAND_NORTH_EAST,
            COMMAND_LEFT,
            COMMAND_LEFT_QUICK,
            COMMAND_RIGHT,
            COMMAND_RIGHT_QUICK,
            COMMAND_SOUTH_WEST,
            COMMAND_BACK,
            COMMAND_SOUTH_EAST,
            COMMAND_FLAG_ON,
            COMMAND_FLAG_OFF,
            COMMAND_LED_ON,
            COMMAND_LED_OFF,
            COMMAND_STOP,
            SPEED_NORMAL,
    })
    public @interface RobotCommand {
        String COMMAND_NORTH_WEST = "G";
        String COMMAND_FORWARD = "F";
        String COMMAND_NORTH_EAST = "I";
        String COMMAND_LEFT = "L";
        String COMMAND_LEFT_QUICK = "l";
        String COMMAND_RIGHT = "R";
        String COMMAND_RIGHT_QUICK = "r";
        String COMMAND_SOUTH_WEST = "H";
        String COMMAND_BACK = "B";
        String COMMAND_SOUTH_EAST = "J";
        String COMMAND_FLAG_ON = "U";
        String COMMAND_FLAG_OFF = "u";
        String COMMAND_LED_ON = "X";
        String COMMAND_LED_OFF = "x";
        String COMMAND_STOP = "S";
        String SPEED_NORMAL = "3";
    }

    @Retention(SOURCE)
    @IntDef()
    public @interface BluetoothCommandRepeat {
        int COUNT_SINGLE = 1;
        int COUNT_SHORT = 3;
        int COUNT_NORMAL = 5;
        int COUNT_LONG = 7;
    }


    @DebugLog
    @Override
    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onServiceStateChanged(int state) {
        switch (state) {
            case BluetoothState.STATE_CONNECTED:
                showMessage("Bluetooth: Connected");
                return;
            case BluetoothState.STATE_CONNECTING:
                showMessage("Bluetooth: Connecting");
                return;
            case BluetoothState.STATE_LISTEN:
                showMessage("Bluetooth: Listen");
                return;
            case BluetoothState.STATE_NONE:
                showMessage("Bluetooth: None");
                return;
            case BluetoothState.STATE_NULL:
                showMessage("Bluetooth: Null");
                return;
            default:
                showMessage("Unknown message");
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        TextView messengerTextView;
        CircleImageView messengerImageView;

        public MessageViewHolder(View v) {
            super(v);
            messageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
            messengerTextView = (TextView) itemView.findViewById(R.id.messengerTextView);
            messengerImageView = (CircleImageView) itemView.findViewById(R.id.messengerImageView);
        }
    }

    private static final String TAG = "MainActivity";
    public static final String MESSAGES_CHILD = "messages";
    private static final int REQUEST_INVITE = 1;
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 500;
    public static final String ANONYMOUS = "anonymous";
    private static final String MESSAGE_SENT_EVENT = "message_sent";
    private static final String MESSAGE_URL = "http://robot-drive-brain-platform.firebase.google.com/message/";

    private String mUsername;
    private String mPhotoUrl;

    private Button mSendButton;
    @BindView(R.id.messageRecyclerView)
    public RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private FirebaseRecyclerAdapter<RobotMessage, MessageViewHolder> mFirebaseAdapter;

    @BindView(R.id.progressBar)
    public ProgressBar mProgressBar;

    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private FirebaseAnalytics mFirebaseAnalytics;

    @BindView(R.id.messageEditText)
    public EditText mMessageEditText;
    @BindView(R.id.main_message_input_container)
    public View messageInputContainer;

    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private GoogleApiClient mGoogleApiClient;

    @DebugLog
    @Override
    public void executeCommand(String text) {
        switch (text) {
            case "#starting-writing":
                showEmotion(SAD_THINKING);
                return;
            case "#stopped-writing":
                showEmotion(TALKING);
                return;
            case "#emotion-happy":
                showEmotion(HAPPY);
                return;
            case "#emotion-neutral":
                showEmotion(NEUTRAL);
                return;
            case "#emotion-sad":
                showEmotion(SAD);
                return;
            case "#drive-north-west":
                drive(NORTH_WEST);
                return;
            case "#drive-upward":
                drive(UPWARD);
                return;
            case "#drive-north-east":
                drive(NORTH_EAST);
                return;
            case "#drive-left":
                drive(LEFT);
                return;
            case "#drive-left-quick":
                drive(LEFT_QUICK);
                return;
            case "#drive-around":
                drive(AROUND);
                return;
            case "#drive-right":
                drive(RIGHT);
                return;
            case "#drive-right-quick":
                drive(RIGHT_QUICK);
                return;
            case "#drive-south-west":
                drive(SOUTH_WEST);
                return;
            case "#drive-back":
                drive(BACK);
                return;
            case "#drive-south-east":
                drive(SOUTH_EAST);
                return;
            case "#play-music":
                playMusic();
                return;
            case "#led-on":
                turnLed(true);
                return;
            case "#led-off":
                turnLed(false);
                return;
            case "#celebrate-lithuanian-birthday":
                celebrate();
                return;
            default:
                showMessage("Unknown command: " + text);
        }
    }

    @DebugLog
    @Override
    public void celebrate() {
        showEmotion(VERY_HAPPY);
        sendBluetoothCommandDirect(COMMAND_FLAG_ON);
        sendDelayed(FLAG_SHAKE_DELAY, COMMAND_FLAG_OFF);
        sendDelayed(2 * FLAG_SHAKE_DELAY, COMMAND_FLAG_ON);
        sendDelayed(3 * FLAG_SHAKE_DELAY, COMMAND_FLAG_OFF);
        sendDelayed(4 * FLAG_SHAKE_DELAY, COMMAND_FLAG_ON);
        sendDelayed(5 * FLAG_SHAKE_DELAY, COMMAND_FLAG_OFF);
    }

    @Override
    public void showListening(boolean listening) {
        voiceButton.setImageResource(listening ? R.drawable.ic_mic_white_24dp : R.drawable.ic_mic_off_white_24dp);
        if (listening) {
            showEmotion(NEUTRAL);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mUsername = ANONYMOUS;

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if (mFirebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        } else {
            mUsername = mFirebaseUser.getDisplayName();
            Uri photoUrl = mFirebaseUser.getPhotoUrl();
            if (photoUrl != null) {
                mPhotoUrl = photoUrl.toString();
            }
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();

        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mFirebaseAdapter = new FirebaseRecyclerAdapter<RobotMessage, MessageViewHolder>(
                RobotMessage.class,
                R.layout.item_message,
                MessageViewHolder.class,
                mFirebaseDatabaseReference.child(MESSAGES_CHILD)) {

            @Override
            protected RobotMessage parseSnapshot(DataSnapshot snapshot) {
                RobotMessage robotMessage = super.parseSnapshot(snapshot);
                if (robotMessage != null) {
                    robotMessage.setId(snapshot.getKey());
                }
                return robotMessage;
            }

            @Override
            protected void populateViewHolder(MessageViewHolder viewHolder, RobotMessage robotMessage, int position) {
                mProgressBar.setVisibility(INVISIBLE);
                viewHolder.messageTextView.setText(robotMessage.getText());
                viewHolder.messengerTextView.setText(robotMessage.getName());
                if (robotMessage.getPhotoUrl() == null) {
                    viewHolder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(MainActivity.this,
                            R.drawable.ic_account_circle_black_36dp));
                } else {
                    Glide.with(MainActivity.this)
                            .load(robotMessage.getPhotoUrl())
                            .into(viewHolder.messengerImageView);
                }

                // write this message to the on-device index
                FirebaseAppIndex.getInstance().update(getMessageIndexable(robotMessage));

                // log a view action on it
                FirebaseUserActions.getInstance().end(getMessageViewAction(robotMessage));
            }
        };

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                mProgressBar.setVisibility(GONE);
                int msgCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the user is at the bottom of the list, scroll
                // to the bottom of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (msgCount - 1) && lastVisiblePosition == (positionStart - 1))) {
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
                if (lastVisiblePosition > 1) {
                    RobotMessage item = mFirebaseAdapter.getItem(msgCount - 1);
                    String text = item.getText();
                    if (!text.startsWith("#")) {
                        speakText(text, item.getId());
                    } else {
                        executeCommand(text);
                    }
                }
            }
        });

        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);
        mMessageRecyclerView.setAdapter(mFirebaseAdapter);

        // Initialize Firebase Measurement.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Initialize Firebase Remote Config.
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        // Define Firebase Remote Config Settings.
        FirebaseRemoteConfigSettings firebaseRemoteConfigSettings =
                new FirebaseRemoteConfigSettings.Builder()
                        .setDeveloperModeEnabled(true)
                        .build();

        // Define default config values. Defaults are used when fetched config values are not
        // available. Eg: if an error occurred fetching values from the server.
        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put(RobotPreferences.ROBOT_MSG_LENGTH, 10L);

        // Apply config settings and default values.
        mFirebaseRemoteConfig.setConfigSettings(firebaseRemoteConfigSettings);
        mFirebaseRemoteConfig.setDefaults(defaultConfigMap);

        // Fetch remote config.
        fetchConfig();

        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(mSharedPreferences
                .getInt(RobotPreferences.ROBOT_MSG_LENGTH, DEFAULT_MSG_LENGTH_LIMIT))});
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mSendButton = (Button) findViewById(R.id.sendButton);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg = mMessageEditText.getText().toString();
                RobotMessage robotMessage = new RobotMessage(msg, mUsername,
                        mPhotoUrl);
                mFirebaseDatabaseReference.child(MESSAGES_CHILD).push().setValue(robotMessage);
                mMessageEditText.setText("");
                mFirebaseAnalytics.logEvent(MESSAGE_SENT_EVENT, null);
            }
        });

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                Log.d("Speech", "OnInit - Status [" + status + "]");

                if (status == TextToSpeech.SUCCESS) {
                    Log.d("Speech", "Success!");
                    tts.setLanguage(Locale.UK);
                }
            }
        });
        tts.setLanguage(Locale.US);

        bt = new BluetoothSPP(this);

        bt.setBluetoothStateListener(this);
        bt.setOnDataReceivedListener(this);
        bt.setupService();

        if (!bt.isBluetoothAvailable()) {
            showMessage("Bluetooth is not available.");
        }

        handler = new Handler();


        initAudioRecognition();
    }

    @OnClick(R.id.main_voice_button)
    public void onVoiceButtonClick(View button) {
        voiceController.onVoiceButtonClicked();
    }

    @OnClick(R.id.main_settings_button)
    public void onSettingsButtonClick(View button) {
        boolean visible = mMessageRecyclerView.getVisibility() != VISIBLE;

        mMessageRecyclerView.setVisibility(visible ? VISIBLE : INVISIBLE);
        messageInputContainer.setVisibility(visible ? VISIBLE : GONE);

        faceView.setVisibility(visible ? GONE : VISIBLE);
    }

    private void initAudioRecognition() {
        permissionsController = new PermissionsControllerImpl(this, this);
        if (!permissionsController.hasPermissionGranted(RECORD_AUDIO_PERMISSION)) {
            permissionsController.requestPermission(RECORD_AUDIO_PERMISSION);
        }
        voiceController = new VoiceController(this, this);
    }

    @Override
    public void turnLed(boolean ledLightingState) {
        showEmotion(EXCLAMATION);
        if (ledLightingState) {
            sendBluetoothCommandDirect(COMMAND_LED_ON);
        } else {
            sendBluetoothCommandDirect(COMMAND_LED_OFF);
        }
    }

    private void playMusic() {
        showEmotion(MUSIC);
        // TODO
    }

    private void sendDelayed(long delay, @RobotCommand final String command) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendBluetoothCommandDirect(command);
            }
        }, delay);
    }

    private void drive(@Direction int direction) {
        sendBluetoothCommandDirect(SPEED_NORMAL);
        switch (direction) {
            case AROUND:
                turnAround();
                break;
            case BACK:
                sendBluetoothCommand(COUNT_LONG, COMMAND_BACK, true);
                break;
            case LEFT:
                sendBluetoothCommand(COUNT_LONG, COMMAND_LEFT, true);
                break;
            case LEFT_QUICK:
                sendBluetoothCommand(COUNT_LONG, COMMAND_LEFT_QUICK, true);
                break;
            case NORTH_EAST:
                sendBluetoothCommand(COUNT_LONG, COMMAND_NORTH_EAST, true);
                break;
            case NORTH_WEST:
                sendBluetoothCommand(COUNT_LONG, COMMAND_NORTH_WEST, true);
                break;
            case RIGHT:
                sendBluetoothCommand(COUNT_LONG, COMMAND_RIGHT, true);
                break;
            case RIGHT_QUICK:
                sendBluetoothCommand(COUNT_LONG, COMMAND_RIGHT_QUICK, true);
                break;
            case SOUTH_EAST:
                sendBluetoothCommand(COUNT_LONG, COMMAND_SOUTH_EAST, true);
                break;
            case SOUTH_WEST:
                sendBluetoothCommand(COUNT_LONG, COMMAND_SOUTH_WEST, true);
                break;
            case UPWARD:
                sendBluetoothCommand(COUNT_LONG, COMMAND_FORWARD, true);
                break;
        }
    }

    private void turnAround() {
        sendBluetoothCommandDirect(RobotCommand.COMMAND_NORTH_EAST);
        sendDelayed(DRIVE_DELAY, COMMAND_SOUTH_WEST);
        sendDelayed(2 * DRIVE_DELAY, COMMAND_NORTH_EAST);
        sendDelayed(3 * DRIVE_DELAY, COMMAND_STOP);
    }

    @DebugLog
    private void sendBluetoothCommand(@BluetoothCommandRepeat int repeatCount, @RobotCommand String commandLetter) {
        for (int i = 0; i < repeatCount; i++) {
            sendBluetoothCommandDirect(commandLetter);
        }
    }

    @DebugLog
    private void sendBluetoothCommand(@BluetoothCommandRepeat int repeatCount, @RobotCommand String commandLetter, boolean stopAfterDelay) {
        sendBluetoothCommand(repeatCount, commandLetter);
        if (stopAfterDelay) {
            sendDelayed(DELAY_LONG, COMMAND_STOP);
        }
    }


    @DebugLog
    private void sendBluetoothCommandDirect(@RobotCommand String commandLetter) {
        bt.send(commandLetter, true);
    }

    @Override
    public void showEmotion(@Emotion int emotion) {
        @DrawableRes int face = R.drawable.face_happy;
        switch (emotion) {
            case EXCLAMATION:
                face = R.drawable.face_exclamation;
                break;
            case Emotion.HAPPY:
                face = R.drawable.face_very_happy;
                break;
            case MUSIC:
                face = R.drawable.face_music;
                break;
            case Emotion.NEUTRAL:
                face = R.drawable.face_happy;
                break;
            case SAD_THINKING:
                face = R.drawable.face_sad_thinking;
                break;
            case Emotion.SAD:
                face = R.drawable.face_sad;
                break;
            case TALKING:
                face = R.drawable.face_talking;
                break;
            case VERY_HAPPY:
                face = R.drawable.face_very_happy;
                break;
        }
        faceView.setImageResource(face);
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (bt.isBluetoothEnabled()) {
            bt.startService(BluetoothState.DEVICE_OTHER);
            bt.autoConnect(getString(R.string.device_name));
        } else {
            Toast.makeText(this, "Bluetooth is not enabled", Toast.LENGTH_SHORT).show();
            openBluetoothSettings();
        }
    }

    private void openBluetoothSettings() {
        Intent intent = new Intent(getApplicationContext(), DeviceList.class);
        startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bt.stopService();
    }

    @DebugLog
    @Override
    public void onDataReceived(byte[] data, String message) {
        showMessage("BT: " + message);
    }

    @Override
    public void speakText(String message, String id) {
        tts.speak(message, TextToSpeech.QUEUE_ADD, null, id);
        showEmotion(TALKING);
    }

    private Action getMessageViewAction(RobotMessage robotMessage) {
        return new Action.Builder(Action.Builder.VIEW_ACTION)
                .setObject(robotMessage.getName(), MESSAGE_URL.concat(robotMessage.getId()))
                .setMetadata(new Action.Metadata.Builder().setUpload(false))
                .build();
    }

    private Indexable getMessageIndexable(RobotMessage robotMessage) {
        PersonBuilder sender = Indexables.personBuilder()
                .setIsSelf(robotMessage.getName().equalsIgnoreCase(mUsername))
                .setName(robotMessage.getName())
                .setUrl(MESSAGE_URL.concat(robotMessage.getId() + "/sender"));

        PersonBuilder recipient = Indexables.personBuilder()
                .setName(mUsername)
                .setUrl(MESSAGE_URL.concat(robotMessage.getId() + "/recipient"));

        return Indexables.messageBuilder()
                .setName(robotMessage.getText())
                .setUrl(MESSAGE_URL.concat(robotMessage.getId()))
                .setSender(sender)
                .setRecipient(recipient)
                .build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.invite_menu:
                sendInvitation();
                return true;
            case R.id.crash_menu:
                FirebaseCrash.logcat(Log.ERROR, TAG, "crash caused");
                causeCrash();
                return true;
            case R.id.sign_out_menu:
                mFirebaseAuth.signOut();
                Auth.GoogleSignInApi.signOut(mGoogleApiClient);
                mFirebaseUser = null;
                mUsername = ANONYMOUS;
                mPhotoUrl = null;
                startActivity(new Intent(this, SignInActivity.class));
                return true;
            case R.id.fresh_config_menu:
                fetchConfig();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void causeCrash() {
        throw new NullPointerException("Fake null pointer exception");
    }

    private void sendInvitation() {
        Intent intent = new AppInviteInvitation.IntentBuilder(getString(R.string.invitation_title))
                .setMessage(getString(R.string.invitation_message))
                .setCallToActionText(getString(R.string.invitation_cta))
                .build();
        startActivityForResult(intent, REQUEST_INVITE);
    }

    // Fetch the config to determine the allowed length of messages.
    public void fetchConfig() {
        long cacheExpiration = 3600; // 1 hour in seconds
        // If developer mode is enabled reduce cacheExpiration to 0 so that each fetch goes to the
        // server. This should not be used in release builds.
        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Make the fetched config available via FirebaseRemoteConfig get<type> calls.
                        mFirebaseRemoteConfig.activateFetched();
                        applyRetrievedLengthLimit();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // There has been an error fetching the config
                        Log.w(TAG, "Error fetching config: " + e.getMessage());
                        applyRetrievedLengthLimit();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_INVITE) {
            if (resultCode == RESULT_OK) {
                // Use Firebase Measurement to log that invitation was sent.
                Bundle payload = new Bundle();
                payload.putString(FirebaseAnalytics.Param.VALUE, "inv_sent");

                // Check how many invitations were sent and log.
                String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);
                Log.d(TAG, "Invitations sent: " + ids.length);
            } else {
                // Use Firebase Measurement to log that invitation was not sent
                Bundle payload = new Bundle();
                payload.putString(FirebaseAnalytics.Param.VALUE, "inv_not_sent");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, payload);

                // Sending failed or it was canceled, show failure message to the user
                Log.d(TAG, "Failed to send invitation.");
            }
        }
    }

    /**
     * Apply retrieved length limit to edit text field. This result may be fresh from the server or it may be from
     * cached values.
     */
    private void applyRetrievedLengthLimit() {
        Long msgLen = mFirebaseRemoteConfig.getLong(RobotPreferences.ROBOT_MSG_LENGTH);
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(msgLen.intValue())});
        Log.d(TAG, "RML is: " + msgLen);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }
}
