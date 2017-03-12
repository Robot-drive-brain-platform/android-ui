package lt.andro.robot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
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
import de.hdodenhof.circleimageview.CircleImageView;
import hugo.weaving.DebugLog;
import timber.log.Timber;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        MainActivityView,
        BluetoothSPP.BluetoothStateListener,
        BluetoothSPP.OnDataReceivedListener {

    private static final int EMOTION_HAPPY = 201;
    private static final int EMOTION_NEUTRAL = 202;
    private static final int EMOTION_SAD = 203;
    private static final int DIRECTION_NORTH_WEST = 301;
    private static final int DIRECTION_UPWARD = 302;
    private static final int DIRECTION_NORTH_EAST = 303;
    private static final int DIRECTION_LEFT_QUICK = 304;
    private static final int DIRECTION_LEFT = 3041;
    private static final int DIRECTION_AROUND = 305;
    private static final int DIRECTION_RIGHT_QUICK = 306;
    private static final int DIRECTION_RIGHT = 3061;
    private static final int DIRECTION_SOUTH_WEST = 307;
    private static final int DIRECTION_BACK = 308;
    private static final int DIRECTION_SOUTH_EAST = 309;

    public static final String BT_COMMAND_SPEED_NORMAL = "6";
    public static final int BT_REPEAT_COUNT_SINGLE = 1;
    public static final int BT_REPEAT_COUNT_SHORT = 3;
    public static final int BT_REPEAT_COUNT_NORMAL = 5;
    public static final int BT_REPEAT_COUNT_LONG = 10;

    public static final String COMMAND_LETTER_NORTH_WEST = "G";
    public static final String COMMAND_LETTER_FORWARD = "F";
    public static final String COMMAND_LETTER_NORTH_EAST = "I";
    public static final String COMMAND_LETTER_LEFT = "L";
    public static final String COMMAND_LETTER_LEFT_QUICK = "l";
    public static final String COMMAND_LETTER_RIGHT = "R";
    public static final String COMMAND_LETTER_RIGHT_QUICK = "r";
    public static final String COMMAND_LETTER_SOUTH_WEST = "H";
    public static final String COMMAND_LETTER_BACK = "B";
    public static final String COMMAND_LETTER_SOUTH_EAST = "J";
    public static final String COMMAND_LETTER_FLAG_ON = "U";
    public static final String COMMAND_LETTER_FLAG_OFF = "u";
    public static final String COMMAND_LETTER_LED_ON = "X";
    public static final String COMMAND_LETTER_LED_OFF = "x";
    public static final String COMMAND_LETTER_STOP = "S";
    private static final int DELAY_LONG = 3000;

    private TextToSpeech tts;
    private BluetoothSPP bt;
    private Handler handler;

    @Retention(SOURCE)
    @IntDef({
            DIRECTION_NORTH_WEST,
            DIRECTION_UPWARD,
            DIRECTION_NORTH_EAST,
            DIRECTION_LEFT,
            DIRECTION_LEFT_QUICK,
            DIRECTION_AROUND,
            DIRECTION_RIGHT_QUICK,
            DIRECTION_RIGHT,
            DIRECTION_SOUTH_WEST,
            DIRECTION_BACK,
            DIRECTION_SOUTH_EAST
    })
    @interface Direction {
    }

    @Retention(SOURCE)
    @StringDef({
            COMMAND_LETTER_NORTH_WEST,
            COMMAND_LETTER_FORWARD,
            COMMAND_LETTER_NORTH_EAST,
            COMMAND_LETTER_LEFT_QUICK,
            COMMAND_LETTER_LEFT,
            COMMAND_LETTER_RIGHT_QUICK,
            COMMAND_LETTER_RIGHT,
            COMMAND_LETTER_SOUTH_WEST,
            COMMAND_LETTER_BACK,
            COMMAND_LETTER_SOUTH_EAST,
            COMMAND_LETTER_FLAG_ON,
            COMMAND_LETTER_FLAG_OFF,
            COMMAND_LETTER_LED_ON,
            COMMAND_LETTER_LED_OFF,
            COMMAND_LETTER_STOP,
            BT_COMMAND_SPEED_NORMAL
    })
    @interface RobotCommand {
    }

    @Retention(SOURCE)
    @IntDef({
            BT_REPEAT_COUNT_SINGLE,
            BT_REPEAT_COUNT_SHORT,
            BT_REPEAT_COUNT_NORMAL,
            BT_REPEAT_COUNT_LONG
    })
    @interface BluetoothCommandRepeat {
    }


    @DebugLog
    @Override
    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
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
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private FirebaseRecyclerAdapter<RobotMessage, MessageViewHolder> mFirebaseAdapter;
    private ProgressBar mProgressBar;
    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private FirebaseAnalytics mFirebaseAnalytics;
    private EditText mMessageEditText;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageRecyclerView = (RecyclerView) findViewById(R.id.messageRecyclerView);
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
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
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

        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
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
    }

    private void executeCommand(String text) {
        switch (text) {
            case "#emotion-happy":
                showEmotion(EMOTION_HAPPY);
                return;
            case "#emotion-neutral":
                showEmotion(EMOTION_NEUTRAL);
                return;
            case "#emotion-sad":
                showEmotion(EMOTION_SAD);
                return;
            case "#drive-north-west":
                drive(DIRECTION_NORTH_WEST);
                return;
            case "#drive-upward":
                drive(DIRECTION_UPWARD);
                return;
            case "#drive-north-east":
                drive(DIRECTION_NORTH_EAST);
                return;
            case "#drive-left":
                drive(DIRECTION_LEFT);
                return;
            case "#drive-left-quick":
                drive(DIRECTION_LEFT_QUICK);
                return;
            case "#drive-around":
                drive(DIRECTION_AROUND);
                return;
            case "#drive-right":
                drive(DIRECTION_RIGHT);
                return;
            case "#drive-right-quick":
                drive(DIRECTION_RIGHT_QUICK);
                return;
            case "#drive-south-west":
                drive(DIRECTION_SOUTH_WEST);
                return;
            case "#drive-back":
                drive(DIRECTION_BACK);
                return;
            case "#drive-south-east":
                drive(DIRECTION_SOUTH_EAST);
                return;
            case "#play-music":
                playMusic();
                return;
            case "#celebrate-lithuanian-birthday":
                sendBluetoothCommandDirect(COMMAND_LETTER_FLAG_ON);
                sendDelayed(DELAY_LONG, COMMAND_LETTER_FLAG_OFF);
                return;
            default:
                Timber.e("Unknown command: " + text);
        }
    }

    private void playMusic() {
        // FIXME
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
        sendBluetoothCommandDirect(BT_COMMAND_SPEED_NORMAL);
        switch (direction) {
            case DIRECTION_AROUND:
                turnAround();
                break;
            case DIRECTION_BACK:
                sendBluetoothCommand(BT_REPEAT_COUNT_LONG, COMMAND_LETTER_BACK, true);
                break;
            case DIRECTION_LEFT:
                sendBluetoothCommand(BT_REPEAT_COUNT_LONG, COMMAND_LETTER_LEFT, true);
                break;
            case DIRECTION_LEFT_QUICK:
                sendBluetoothCommand(BT_REPEAT_COUNT_LONG, COMMAND_LETTER_LEFT_QUICK, true);
                break;
            case DIRECTION_NORTH_EAST:
                sendBluetoothCommand(BT_REPEAT_COUNT_LONG, COMMAND_LETTER_NORTH_EAST, true);
                break;
            case DIRECTION_NORTH_WEST:
                sendBluetoothCommand(BT_REPEAT_COUNT_LONG, COMMAND_LETTER_NORTH_WEST, true);
                break;
            case DIRECTION_RIGHT:
                sendBluetoothCommand(BT_REPEAT_COUNT_LONG, COMMAND_LETTER_RIGHT, true);
                break;
            case DIRECTION_RIGHT_QUICK:
                sendBluetoothCommand(BT_REPEAT_COUNT_LONG, COMMAND_LETTER_RIGHT_QUICK, true);
                break;
            case DIRECTION_SOUTH_EAST:
                sendBluetoothCommand(BT_REPEAT_COUNT_LONG, COMMAND_LETTER_SOUTH_EAST, true);
                break;
            case DIRECTION_SOUTH_WEST:
                sendBluetoothCommand(BT_REPEAT_COUNT_LONG, COMMAND_LETTER_SOUTH_WEST, true);
                break;
            case DIRECTION_UPWARD:
                sendBluetoothCommand(BT_REPEAT_COUNT_LONG, COMMAND_LETTER_FORWARD, true);
                break;
        }
    }

    private void turnAround() {
//        sendBluetoothCommand(BT_REPEAT_COUNT_SHORT, COMMAND_LETTER_NORTH_EAST);
//        sendBluetoothCommand(BT_REPEAT_COUNT_SHORT, COMMAND_LETTER_SOUTH_WEST);
//        sendBluetoothCommand(BT_REPEAT_COUNT_SHORT, COMMAND_LETTER_NORTH_EAST);
        sendBluetoothCommand(BT_REPEAT_COUNT_SINGLE, COMMAND_LETTER_STOP, true);
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
            sendDelayed(DELAY_LONG, COMMAND_LETTER_STOP);
        }
    }


    @DebugLog
    private void sendBluetoothCommandDirect(@RobotCommand String commandLetter) {
        bt.send(commandLetter, true);
    }

    private void showEmotion(int emotion) {

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

    private void speakText(String message, String id) {
        tts.speak(message, TextToSpeech.QUEUE_ADD, null, id);
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
