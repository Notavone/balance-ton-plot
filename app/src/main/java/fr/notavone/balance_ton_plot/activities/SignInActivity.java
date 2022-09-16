package fr.notavone.balance_ton_plot.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class SignInActivity extends AppCompatActivity {
    private final Logger logger = Logger.getLogger(SignInActivity.class.getName());
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final AuthUI authUI = AuthUI.getInstance();

    private final List<AuthUI.IdpConfig> providers = Arrays.asList(
            new AuthUI.IdpConfig.PhoneBuilder().build(),
            new AuthUI.IdpConfig.EmailBuilder().build()
    );

    private final Intent signInIntent = authUI.createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setIsSmartLockEnabled(false)
            .build();

    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            this::onSignInResult
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (auth.getCurrentUser() != null) {
            logger.info("User already signed in, skipping sign in activity");
            logger.info("User: " + auth.getCurrentUser().getUid());
            switchToMain();
        } else signInLauncher.launch(signInIntent);
    }

    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        if (result.getResultCode() == RESULT_OK) {
            logger.info("Result code is OK");

            if (auth.getCurrentUser() != null) {
                logger.info("User is not null");
                logger.info("User is " + auth.getCurrentUser().getDisplayName());
                logger.info("User is " + auth.getCurrentUser().getPhoneNumber());
                logger.info("User is " + auth.getCurrentUser().getEmail());

                switchToMain();
            } else {
                logger.info("User is null");
                restartActivity();
            }
        } else {
            logger.info("Result code is not OK");
            restartActivity();
        }
    }

    private void restartActivity() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    private void switchToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        finish();
        startActivity(intent);
    }
}