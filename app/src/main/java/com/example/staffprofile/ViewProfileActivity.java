package com.example.staffprofile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

public class ViewProfileActivity extends AppCompatActivity {

    private TextView textViewName, textViewJobTitle, textViewSkills, textViewCertifications, textViewEmail, textViewPhone, textViewExperience, textViewAbout;
    private ImageView imageViewProfilePhoto;
    private Button buttonEdit, buttonDelete, buttonViewCertificate;
    private String certificateUrl;
    private DatabaseHelper databaseHelper;
    private String employeeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.employee_profile);

        // Initialize UI elements
        textViewName = findViewById(R.id.textViewName);
        textViewJobTitle = findViewById(R.id.textViewJobTitle);
        textViewSkills = findViewById(R.id.textViewSkills);
        textViewCertifications = findViewById(R.id.textViewCertifications);
        imageViewProfilePhoto = findViewById(R.id.imageViewProfilePhoto);
        textViewAbout = findViewById(R.id.etAbout);
        textViewPhone = findViewById(R.id.etPhone);
        textViewExperience = findViewById(R.id.etExperience);
        textViewEmail = findViewById(R.id.etEmail);


        buttonEdit = findViewById(R.id.buttonEditProfile);
        buttonDelete = findViewById(R.id.buttonDeleteProfile);

        // Get employee ID from Intent
        Intent intent = getIntent();
        employeeId = intent.getStringExtra("employeeId");

        databaseHelper = new DatabaseHelper(this);

        // Load the employee profile
        loadProfileData();

        // Handle edit button click
        buttonEdit.setOnClickListener(v -> {
            Intent editIntent = new Intent(ViewProfileActivity.this, EditProfileActivity.class);
            editIntent.putExtra("employeeId", employeeId);
            startActivity(editIntent);
            finish();
        });

        // Handle delete button click
        buttonDelete.setOnClickListener(v -> deleteProfile());

        // Handle view certificate button click

    }

    private void loadProfileData() {
        databaseHelper.getProfileData(employeeId, new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    DataSnapshot dataSnapshot = task.getResult();
                    if (dataSnapshot.exists()) {
                        String name = dataSnapshot.child("name").getValue(String.class);
                        String jobTitle = dataSnapshot.child("jobTitle").getValue(String.class);
                        String skills = dataSnapshot.child("skills").getValue(String.class);
                        String certifications = dataSnapshot.child("certifications").getValue(String.class);
                        String photoUrl = dataSnapshot.child("photoUrl").getValue(String.class);

                        String email = dataSnapshot.child("email").getValue(String.class);
                        String phone = dataSnapshot.child("phone").getValue(String.class);
                        String experience = dataSnapshot.child("experience").getValue(String.class);
                        String about = dataSnapshot.child("about").getValue(String.class);

                        textViewName.setText(name);
                        textViewJobTitle.setText(jobTitle);
                        textViewSkills.setText(skills);
                        textViewCertifications.setText(certifications);
                        textViewEmail.setText(email);
                        textViewPhone.setText(phone);
                        textViewExperience.setText(experience);
                        textViewAbout.setText(about);

                        // Log the certificate URL to verify it is being fetched correctly


                        // Display the certificate URL in a toast for verification


                        // Load image using Glide
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(ViewProfileActivity.this)
                                    .load(photoUrl)
                                    .into(imageViewProfilePhoto);
                        } else {
                            Log.d("ViewProfileActivity", "No photo URL found.");
                        }
                    } else {
                        Log.d("ViewProfileActivity", "Profile data not found for employee ID: " + employeeId);
                    }
                } else {
                    Log.e("ViewProfileActivity", "Error loading profile data: " + task.getException().getMessage());
                }
            }
        });
    }


    private void deleteProfile() {
        databaseHelper.deleteEmployeeProfile(employeeId, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(ViewProfileActivity.this, "Profile deleted successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(ViewProfileActivity.this, "Failed to delete profile", Toast.LENGTH_SHORT).show();
                Log.e("ViewProfileActivity", "Error deleting profile: " + task.getException().getMessage());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileData();
    }
}
