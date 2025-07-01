package com.example.staffprofile;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.storage.StorageReference;
import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;

import java.io.IOException;

public class EditProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;


    private EditText editTextName, editTextJobTitle, editTextSkills, editTextCertifications,editTextEmail,editTextPhone,editTextExperience,editTextAbout;
    private ImageView imageViewProfilePhoto;
    private Button buttonSave, buttonSelectPhoto, buttonSelectCertificate;
    private String employeeId;
    private Bitmap selectedPhotoBitmap;
    private Uri certificateUri;
    private DatabaseHelper databaseHelper;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        editTextName = findViewById(R.id.etName);
        editTextJobTitle = findViewById(R.id.etJobTitle);
        editTextSkills = findViewById(R.id.etSkills);
        editTextCertifications = findViewById(R.id.etCertifications);
        imageViewProfilePhoto = findViewById(R.id.imageViewProfilePhoto);
        buttonSave = findViewById(R.id.btnSaveProfile);
        editTextEmail=findViewById(R.id.etEmail);
        editTextPhone=findViewById(R.id.etPhone);
        editTextExperience=findViewById(R.id.etExperience);
        editTextAbout=findViewById(R.id.etAbout);
        buttonSelectPhoto = findViewById(R.id.buttonSelectImage);


        databaseHelper = new DatabaseHelper(this);
        employeeId = getIntent().getStringExtra("employeeId");

        loadProfileData();

        buttonSelectPhoto.setOnClickListener(v -> selectImage());

        buttonSave.setOnClickListener(v -> saveProfile());
    }



    private void loadProfileData() {
        databaseHelper.getProfileData(employeeId, new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    DataSnapshot snapshot = task.getResult();
                    String photoUrl = snapshot.child("photoUrl").getValue(String.class);

                    // Load photo using Glide if URL is available
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        StorageReference photoRef = databaseHelper.getStorageReference().child(photoUrl);
                        photoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            Glide.with(EditProfileActivity.this)
                                    .load(uri)
                                    .placeholder(R.drawable.img) // placeholder image
                                    .error(R.drawable.img) // error image
                                    .into(imageViewProfilePhoto);
                        }).addOnFailureListener(e -> {
                            Toast.makeText(EditProfileActivity.this, "Failed to load profile image", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        imageViewProfilePhoto.setImageResource(R.drawable.img); // default image if no photo URL
                    }
                } else {
                    Toast.makeText(EditProfileActivity.this, "Failed to load profile data", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            try {
                selectedPhotoBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                imageViewProfilePhoto.setImageBitmap(selectedPhotoBitmap);
            } catch (IOException e) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveProfile() {
        String name = editTextName.getText().toString().trim();
        String jobTitle = editTextJobTitle.getText().toString().trim();
        String skills = editTextSkills.getText().toString().trim();
        String certifications = editTextCertifications.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();
        String experience = editTextExperience.getText().toString().trim();
        String about = editTextAbout.getText().toString().trim();

        // Check for empty fields
        if (name.isEmpty() || jobTitle.isEmpty() || skills.isEmpty() || certifications.isEmpty() ||
                email.isEmpty() || phone.isEmpty() || experience.isEmpty() || about.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Proceed with photo upload if selected
        if (selectedPhotoBitmap != null) {
            databaseHelper.updateEmployeeProfile(employeeId, name, jobTitle, skills, certifications, selectedPhotoBitmap, email, phone, experience, about,  new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        navigateToViewProfile();

                    }
                }
            });
        } else {
            Toast.makeText(this, "No profile photo selected", Toast.LENGTH_SHORT).show();
        }
    }

        private void navigateToViewProfile() {
        Intent intent = new Intent(EditProfileActivity.this, ViewProfileActivity.class);
        intent.putExtra("employeeId", employeeId);
        startActivity(intent);
        finish(); // Close EditProfileActivity after saving
    }
}
