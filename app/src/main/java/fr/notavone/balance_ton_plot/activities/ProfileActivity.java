package fr.notavone.balance_ton_plot.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.logging.Logger;

import fr.notavone.balance_ton_plot.R;
import fr.notavone.balance_ton_plot.entities.User;

public class ProfileActivity extends AppCompatActivity {
    private static final Logger logger = Logger.getLogger(ProfileActivity.class.getName());

    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final CollectionReference users = FirebaseFirestore.getInstance().collection("users");

    private User me;
    private TextInputLayout uuidInput;
    private TextInputLayout usernameInput;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        FloatingActionButton fab = findViewById(R.id.profileBackButton);
        fab.setOnClickListener(view -> finish());

        FloatingActionButton saveBtn = findViewById(R.id.save);
        saveBtn.setOnClickListener(this::onSaveClick);

        uuidInput = findViewById(R.id.profileUuidLayout);
        usernameInput = findViewById(R.id.profileNameLayout);

        getUser();
    }

    private void getUser() {
        if (mAuth.getCurrentUser() != null) {
            users.document(mAuth.getCurrentUser().getUid()).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            me = task.getResult().toObject(User.class);
                            if (me == null) {
                                me = new User(mAuth.getCurrentUser().getUid(), null);
                                logger.info("User not found, creating new one");
                            } else {
                                logger.info("User found: " + me);
                            }
                            updateInfos();
                        } else {
                            logger.warning("Failed to get user: " + task.getException());
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> finish());
        } else {
            finish();
        }
    }

    private void updateInfos() {
        if (me != null) {
            if (uuidInput.getEditText() != null) {
                uuidInput.getEditText().setText(me.getUuid());
            }

            if (usernameInput.getEditText() != null) {
                usernameInput.getEditText().setText(me.getUsername());
            }
        }
    }


    private void onSaveClick(View view) {
        if (usernameInput.getEditText() != null) {
            me.setUsername(usernameInput.getEditText().getText().toString());
            users.document(me.getUuid()).set(me)
                    .addOnSuccessListener(e -> {
                        Toast.makeText(this, "Profil mis à jour", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Erreur lors de la mise à jour du profil", Toast.LENGTH_SHORT).show();
                        finish();
                    });
        }
    }
}