package org.projects.shoppinglist;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;

public class LoginActivity extends BaseActivity implements
        View.OnClickListener {

    private static final String TAG = "EmailPassword";

    private TextView mStatusTextView;
    private TextView mDetailTextView;
    private EditText mEmailField;
    private EditText mPasswordField;
    private TextView mTitleTextView;
    private FirebaseAuth mAuth;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emailpassword);

        //Views
        mStatusTextView = (TextView) findViewById(R.id.status);
        mDetailTextView = (TextView) findViewById(R.id.detail);
        mEmailField = (EditText) findViewById(R.id.field_email);
        mPasswordField = (EditText) findViewById(R.id.field_password);
        mTitleTextView = (TextView) findViewById(R.id.title_text);

        //Buttons
        findViewById(R.id.email_sign_in_button).setOnClickListener(this);
        findViewById(R.id.email_create_account_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.verify_email_button).setOnClickListener(this);
        findViewById(R.id.shoppinglistbtn).setOnClickListener(this);

        //Variable der bruges vedr log ind og bruger
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onStart() {
        super.onStart();
        //Tjekker om brugeren er logget ind, og opdater UI ift.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    private void createAccount(final String email, final String password) {
        //Sikre formular er ok
        if (!validateForm()) {
            return;
        }

        //Viser procesbar
        showProgressDialog();

        try {
            //Opretter bruger med det angivne
            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            //Gemmer info om brugeren der er logget ind, og opdater GUI, sådan brugeren ser profil.
                            //Brugeren der er logget ind vil være den ny oprettede
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        }
                        else {
                            //Laver fejl, hvis brugeren ikke kan lave bruger
                            FirebaseCrash.log("Fejl i oprettelse af bruger: Email; " + email  +" Password; " + password);
                            FirebaseCrash.report(new Exception(task.getException()));

                            //Opdater UI ift. hvad der er sket, og giver bruger besked på, oprettelse ikke løkkes.
                            Toast.makeText(LoginActivity.this, "Enter an unused email and a password with length >= 6.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }

                        //Skjuler procesbar
                        hideProgressDialog();
                    }
                });

            }
            catch (Exception ex){
                FirebaseCrash.log("Uventet fejl i oprrettelse af bruger: Email; " + email  +" Password; " + password);
                FirebaseCrash.report(new Exception(ex.toString()));
            }
    }

    private void signIn(final String email, final String password)
    {
        //Sikre formular er ok
        if (!validateForm()) {
            return;
        }

        //Viser procesbar
        showProgressDialog();

        try {
            //Logger den bruger ind, som matcher angivet mail og password
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            boolean ok = task.isSuccessful();
                            if (ok) {
                                //Ok log ind

                                //Gemmer info om brugeren der er logget ind, og opdater GUI, sådan brugeren ser profil.
                                Log.d(TAG, "signInWithEmail:success");
                                FirebaseUser user = mAuth.getCurrentUser();
                                updateUI(user);

                                //Gemmer mailen, som brugeren er logget ind med, som værdi til "name" præferencen
                                SharedPreferences pref  = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
                                SharedPreferences.Editor editor = pref.edit();
                                editor.putString("name", email);
                                editor.commit();

                                handleLayout(user);
                            }
                            else {

                                //Laver fejl, hvis brugeren ikke kan lave bruger
                                FirebaseCrash.log("Fejl i login af bruger: Email; " + email  +" Password; " + password);
                                FirebaseCrash.report(new Exception(task.getException()));

                                //Opdater UI ift. hvad der er sket, og giver bruger besked på, login ikke løkkes.
                                Toast.makeText(LoginActivity.this, "We couldn't sign you in -> sorry. :( ",
                                        Toast.LENGTH_SHORT).show();
                                updateUI(null);
                            }

                            //Skjuler procesbar
                            hideProgressDialog();
                        }
                    });
        }
        catch (Exception ex){
            FirebaseCrash.log("Uventet fejl i login af bruger: Email; " + email  +" Password; " + password);
            FirebaseCrash.report(new Exception(ex.toString()));
        }
    }

    private void signOut() {
        mAuth.signOut();
        updateUI(null);
    }

    private void sendEmailVerification() {
        // Disable knappen, da man kun skulle kun gøre dette en gang.
        findViewById(R.id.verify_email_button).setEnabled(false);

        //Finder brugeren der er logget ind.
        final FirebaseUser user = mAuth.getCurrentUser();

        if(user != null) {
            try {
            // Sender verification email
            user.sendEmailVerification()
                    .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            findViewById(R.id.verify_email_button).setEnabled(true);

                            //Ok sending
                            if (task.isSuccessful()) {
                                Toast.makeText(LoginActivity.this,
                                        "Verification email sent to " + user.getEmail(),
                                        Toast.LENGTH_SHORT).show();

                            }
                            else {
                                //Ikke ok sending
                                Log.e(TAG, "sendEmailVerification", task.getException());
                                Toast.makeText(LoginActivity.this,
                                        "Failed to send verification email.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
            }
            catch (Exception ex){
                FirebaseCrash.log("Uventet fejl i sending af verification mail til " + user.getEmail());
                FirebaseCrash.report(new Exception(ex.toString()));
            }
        }
        else {
            Toast.makeText(LoginActivity.this,
                    "Need to be signed in to do this.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void handleLayout(FirebaseUser user) {
        if (user != null) {
            //Går til shopping listen
            Intent intentLogin = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intentLogin);
           // finish();
        }
    }

    /** Tjekker input */
    private boolean validateForm() {
        boolean valid = true;

        String email = mEmailField.getText().toString();
        if (TextUtils.isEmpty(email)) {
            mEmailField.setError("Required.");
            valid = false;
        } else {
            mEmailField.setError(null);
        }

        String password = mPasswordField.getText().toString();
        if (TextUtils.isEmpty(password)) {
            mPasswordField.setError("Required.");
            valid = false;
        } else {
            mPasswordField.setError(null);
        }

        return valid;
    }

    /** Opdater UI alt efter, om brugeren er i profil eller login/lav bruger tilstand.  */
    private void updateUI(FirebaseUser user) {
        hideProgressDialog();
        if (user != null) {
            String verified = "Yes";
            if(!user.isEmailVerified()) {
                verified = "No - click the button below to be it.";
            }

            mStatusTextView.setText("Email: " + user.getEmail() + " \nAre you verified ? " + verified);

            mTitleTextView.setText("Profile");

            findViewById(R.id.email_password_buttons).setVisibility(View.GONE);
            findViewById(R.id.email_password_fields).setVisibility(View.GONE);
            findViewById(R.id.signed_in_buttons).setVisibility(View.VISIBLE);

            findViewById(R.id.verify_email_button).setEnabled(!user.isEmailVerified());
        }
        else {
            mStatusTextView.setText(R.string.detail);
            mDetailTextView.setText(null);

            mTitleTextView.setText(R.string.emailpassword_title_text);
            findViewById(R.id.email_password_buttons).setVisibility(View.VISIBLE);
            findViewById(R.id.email_password_fields).setVisibility(View.VISIBLE);
            findViewById(R.id.signed_in_buttons).setVisibility(View.GONE);
        }
    }

    /** Håndter knap click events  */
    @Override
    public void onClick(View v) {
        int i = v.getId();
        Log.d("ERROR", ""+i);
        if (i == R.id.email_create_account_button) {
            createAccount(mEmailField.getText().toString(), mPasswordField.getText().toString());
        } else if (i == R.id.email_sign_in_button) {
            signIn(mEmailField.getText().toString(), mPasswordField.getText().toString());
        } else if (i == R.id.sign_out_button) {
            signOut();
        } else if (i == R.id.verify_email_button) {
            sendEmailVerification();
        } else if(i == R.id.shoppinglistbtn) {
            FirebaseUser user = mAuth.getCurrentUser();
            Log.d("ERROR", "" +user.getEmail());
            handleLayout(user);
        }
    }

    /*
    @Override
    public void finish() {
        Intent data = new Intent();
        setResult(RESULT_OK, data);
        super.finish();
    }*/
}