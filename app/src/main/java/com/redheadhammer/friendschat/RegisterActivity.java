package com.redheadhammer.friendschat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.redheadhammer.friendschat.databinding.ActivityRegisterBinding;

import java.util.Objects;
import java.util.UUID;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private ActivityResultLauncher<Intent> chooseImageLauncher;

    private static final String TAG = "RegisterActivity";
    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference reference;
    private final FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
    private StorageReference storageReference;
    private boolean isImageSelected = false;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        reference = database.getReference();
        storageReference = firebaseStorage.getReference();
        chooseImageLauncherInit();

        Log.d(TAG, "onCreate: " + reference.toString());

        binding.appBarLayout.setExpanded(false);

        binding.userImage.setOnClickListener(this::setImage);
        binding.signUpRegister.setOnClickListener(this::signUp);
    }

    private void signUp(View view) {
        binding.progressBar.setVisibility(View.VISIBLE);

        String email = String.valueOf(binding.emailRegister.getText());
        String password = String.valueOf(binding.passwordRegister.getText());
        String username = String.valueOf(binding.userNameRegister.getText());

        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, R.string.empty_upass,
                    Toast.LENGTH_SHORT).show();
        } else {
            signUpProcess(email, password, username);
        }
    }

    private void setImage(View view) {
        imageChooser();
    }

    private void imageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        chooseImageLauncher.launch(intent);
    }

    private void signUpProcess(String email, String password, String username) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(
                        task -> {
                            if (task.isSuccessful()) {
                                // Create user in the database
                                reference.child("Users")
                                        .child(firebaseAuth.getUid())
                                        .child("username")
                                        .setValue(username);

                                // if image is selected than save it to the storage and
                                // save reference of image in database to load later
                                if (isImageSelected) {
                                    UUID randomId = UUID.randomUUID();
                                    String imageName = String.format("images/%s.jpg", randomId);
                                    // put image in Firebase Storage
                                    storageReference.child(imageName).putFile(imageUri)
                                            .addOnSuccessListener(uri -> {
                                                String filePath = uri.toString();
                                                reference.child("Users")
                                                        .child(firebaseAuth.getUid())
                                                        .child("image")
                                                        .setValue(filePath)
                                                        .addOnCompleteListener(task1 -> {
                                                            if (task1.isSuccessful()) {
                                                                Toast.makeText(this,
                                                                        R.string.database_upload_success, Toast.LENGTH_SHORT).show();
                                                            } else {
                                                                Toast.makeText(this,
                                                                        R.string.upload_database_fail, Toast.LENGTH_SHORT).show();
                                                            }
                                                            });
                                            });
                                } else {
                                    reference.child("Users")
                                            .child(firebaseAuth.getUid())
                                            .child("image")
                                            .setValue("null");
                                }
                                binding.progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Sign Up Successful", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                try {
                                    throw Objects.requireNonNull(task.getException());
                                } catch (FirebaseAuthInvalidCredentialsException e) {
                                    Toast.makeText(this,
                                            R.string.email_bad_format, Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Toast.makeText(RegisterActivity.this,
                                            R.string.error_occurred, Toast.LENGTH_SHORT).show();
                                }
                                binding.progressBar.setVisibility(View.GONE);
                            }
                        }
                );
    }

    private void chooseImageLauncherInit() {
        chooseImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    int result_code = result.getResultCode();
                    Intent data = result.getData();

                    if (result_code == RESULT_OK && data != null) {
                        imageUri = data.getData();
                        Glide.with(RegisterActivity.this)
                                .load(imageUri)
                                .centerCrop()
                                .into(binding.userImage);
                        isImageSelected = true;
                    } else {
                        isImageSelected = false;
                    }
                }
        );
    }
}