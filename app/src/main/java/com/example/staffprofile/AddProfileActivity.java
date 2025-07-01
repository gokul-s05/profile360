package com.example.staffprofile;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.StorageReference;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AddProfileActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_PICK = 1;
    private static final int REQUEST_CERTIFICATE_PICK = 2;

    private EditText etName, etJobTitle, etSkills, etCertifications, etEmail, etPhone, etExperience, etAbout;
    private Button btnSaveProfile, btnSelectImage, btnSelectCertificate;
    private ImageView imageViewProfilePhoto;
    private DatabaseHelper databaseHelper;
    private byte[] photoByteArray;
    private Uri certificateUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_profile);

        etName = findViewById(R.id.etName);
        etJobTitle = findViewById(R.id.etJobTitle);
        etSkills = findViewById(R.id.etSkills);
        etCertifications = findViewById(R.id.etCertifications);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etExperience = findViewById(R.id.etExperience);
        etAbout = findViewById(R.id.etAbout);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnSelectImage = findViewById(R.id.buttonSelectImage);

        imageViewProfilePhoto = findViewById(R.id.imageViewProfilePhoto);

        databaseHelper = new DatabaseHelper(this);

        btnSaveProfile.setOnClickListener(v -> saveProfile());

        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_IMAGE_PICK);
        });


    }

    private void saveProfile() {
        String employeeId = databaseHelper.getEmployeeRef().push().getKey();
        if (employeeId == null) {
            Toast.makeText(this, "Failed to create employee ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Gather all input fields
        String name = etName.getText().toString().trim();
        String jobTitle = etJobTitle.getText().toString().trim();
        String skills = etSkills.getText().toString().trim();
        String certifications = etCertifications.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String experience = etExperience.getText().toString().trim();
        String about = etAbout.getText().toString().trim();

        // Validation
        if (name.isEmpty() || jobTitle.isEmpty() || skills.isEmpty() || certifications.isEmpty() || email.isEmpty() || phone.isEmpty() || experience.isEmpty() || about.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the photo is selected
        if (photoByteArray == null || photoByteArray.length == 0) {
            Toast.makeText(this, "No photo selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Upload profile photo
        StorageReference photoRef = databaseHelper.getStorageReference().child("profile_photos/" + employeeId + ".jpg");
        photoRef.putBytes(photoByteArray)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get photo URL
                    photoRef.getDownloadUrl().addOnSuccessListener(photoUri -> {
                        String photoUrl = photoUri.toString();
                        // Proceed to upload certificate if available
                        if (certificateUri != null) {
                            insertEmployeeToDatabase(employeeId, name, jobTitle, skills, certifications, photoUrl, email, phone, experience, about, certificateUri);
                        } else {
                            insertEmployeeToDatabase(employeeId, name, jobTitle, skills, certifications, photoUrl, email, phone, experience, about, null);
                        }
                    }).addOnFailureListener(e -> {
                        Toast.makeText(AddProfileActivity.this, "Failed to retrieve image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }).addOnFailureListener(e -> {
                    Toast.makeText(AddProfileActivity.this, "Failed to upload photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void insertEmployeeToDatabase(String employeeId, String name, String jobTitle, String skills, String certifications, String photoUrl, String email, String phone, String experience, String about, Uri certificateUri) {
        // Use your updated insertEmployee method to save both profile and certificate URLs
        databaseHelper.insertEmployee(employeeId, name, jobTitle, skills, certifications, photoUrl, email, phone, experience, about, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Intent intent = new Intent(AddProfileActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                if (!task.isSuccessful()) {
                    Toast.makeText(AddProfileActivity.this, "Profile creation failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                    imageViewProfilePhoto.setImageBitmap(bitmap);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    photoByteArray = stream.toByteArray();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == REQUEST_CERTIFICATE_PICK && resultCode == Activity.RESULT_OK && data != null) {
            certificateUri = data.getData();
            if (certificateUri != null) {
                Toast.makeText(this, "Certificate selected", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
