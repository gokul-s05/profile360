package com.example.staffprofile;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class DatabaseHelper {

    private DatabaseReference employeeRef;
    private StorageReference storageRef;
    private DatabaseReference usersRef;
    private Context context; // Store the context
    private static final int MAX_IMAGE_SIZE = 1024 * 1024; // 1MB

    public DatabaseHelper(Context context) {
        this.context = context.getApplicationContext(); // Use application context
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
        employeeRef = firebaseDatabase.getReference("employees");
        usersRef = firebaseDatabase.getReference("users");
        storageRef = firebaseStorage.getReference("employee_photos");
    }

    private String hashPassword(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean insertUser(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.length() < 6) {
            Toast.makeText(context, "Invalid username or password. Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
            return false;
        }

        String hashedPassword = hashPassword(password);
        if (hashedPassword == null) {
            Toast.makeText(context, "Error processing password", Toast.LENGTH_SHORT).show();
            return false;
        }

        String userId = usersRef.push().getKey();
        if (userId == null) {
            Toast.makeText(context, "Error generating user ID", Toast.LENGTH_SHORT).show();
            return false;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username.trim());
        userData.put("password", hashedPassword);
        userData.put("created_at", System.currentTimeMillis());

        usersRef.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Toast.makeText(context, "Username already exists", Toast.LENGTH_SHORT).show();
                } else {
                    usersRef.child(userId).setValue(userData).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(context, "User registered successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Failed to register user", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(context, "Error checking username: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        return true;
    }

    public void insertEmployee(String employeeId, String name, String jobTitle, String skills, String certifications, String photoUrl, String email, String phone, String experience, String about,  OnCompleteListener<Void> onCompleteListener) {
        // Input validation
        if (employeeId == null || name == null || name.trim().isEmpty()) {
            if (onCompleteListener != null) {
                TaskCompletionSource<Void> failureSource = new TaskCompletionSource<>();
                failureSource.setException(new IllegalArgumentException("Employee ID and name are required"));
                onCompleteListener.onComplete(failureSource.getTask());
            }
            return;
        }

        Map<String, Object> employeeData = new HashMap<>();
        employeeData.put("name", name.trim());
        employeeData.put("jobTitle", jobTitle != null ? jobTitle.trim() : "");
        employeeData.put("skills", skills != null ? skills.trim() : "");
        employeeData.put("certifications", certifications != null ? certifications.trim() : "");
        employeeData.put("photoUrl", photoUrl != null ? photoUrl.trim() : "");
        employeeData.put("email", email != null ? email.trim() : "");
        employeeData.put("phone", phone != null ? phone.trim() : "");
        employeeData.put("experience", experience != null ? experience.trim() : "");
        employeeData.put("about", about != null ? about.trim() : "");

        // Set the value in Firebase and add a listener to handle success/failure
        employeeRef.child(employeeId).setValue(employeeData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Show a notification with the employee's name
                showNotification(name);
                Toast.makeText(context, "Employee added successfully", Toast.LENGTH_SHORT).show();
            } else {
                String errorMessage = task.getException() != null ? 
                    task.getException().getMessage() : 
                    "Failed to add employee";
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
            }

            // Invoke the original onCompleteListener passed in as a parameter
            if (onCompleteListener != null) {
                onCompleteListener.onComplete(task);
            }
        });
    }

    public StorageReference getStorageReference() {
        return storageRef;
    }

    public DatabaseReference getEmployeeRef() {
        return employeeRef;
    }

    public void getAllProfiles(OnCompleteListener<DataSnapshot> onCompleteListener) {
        employeeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                TaskCompletionSource<DataSnapshot> source = new TaskCompletionSource<>();
                source.setResult(dataSnapshot);
                onCompleteListener.onComplete(source.getTask());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                TaskCompletionSource<DataSnapshot> source = new TaskCompletionSource<>();
                source.setException(databaseError.toException());
                onCompleteListener.onComplete(source.getTask());
            }
        });
    }

    public void updateEmployeeProfile(String id, String name, String jobTitle, String skills, String certifications, Bitmap photo, String email, String phone, String experience, String about,  OnCompleteListener<Void> onCompleteListener) {
        String photoPath = id + ".jpg";
        uploadImage(photo, photoPath).addOnCompleteListener(uriTask -> {
            if (uriTask.isSuccessful() && uriTask.getResult() != null) {
                Uri downloadUri = uriTask.getResult();
                Map<String, Object> updates = new HashMap<>();
                updates.put("name", name);
                updates.put("jobTitle", jobTitle);
                updates.put("skills", skills);
                updates.put("certifications", certifications);
                updates.put("photoUrl", downloadUri.toString());
                updates.put("email", email);
                updates.put("phone", phone);
                updates.put("experience", experience);
                updates.put("about", about);

                employeeRef.child(id).updateChildren(updates).addOnCompleteListener(onCompleteListener);
            } else if (onCompleteListener != null) {
                TaskCompletionSource<Void> failureSource = new TaskCompletionSource<>();
                failureSource.setException(uriTask.getException());
                onCompleteListener.onComplete(failureSource.getTask());
            }
        });
    }

    public void getProfileData(String employeeId, OnCompleteListener<DataSnapshot> onCompleteListener) {
        employeeRef.child(employeeId).get().addOnCompleteListener(onCompleteListener);
    }

    private Task<Uri> uploadImage(Bitmap photo, String photoPath) {
        TaskCompletionSource<Uri> source = new TaskCompletionSource<>();
        
        if (photo == null) {
            source.setException(new IllegalArgumentException("Photo cannot be null"));
            return source.getTask();
        }

        // Calculate image size
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] photoData = baos.toByteArray();

            // Check if image is too large
            if (photoData.length > MAX_IMAGE_SIZE) {
                // Compress with lower quality until size is acceptable
                int quality = 90;
                while (photoData.length > MAX_IMAGE_SIZE && quality > 10) {
                    baos.reset();
                    photo.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                    photoData = baos.toByteArray();
                    quality -= 10;
                }
                
                if (photoData.length > MAX_IMAGE_SIZE) {
                    source.setException(new IllegalArgumentException("Image is too large and cannot be compressed further"));
                    return source.getTask();
                }
            }

            StorageReference photoRef = storageRef.child(photoPath);
            photoRef.putBytes(photoData)
                .addOnSuccessListener(taskSnapshot -> {
                    photoRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> source.setResult(uri))
                        .addOnFailureListener(e -> source.setException(e));
                })
                .addOnFailureListener(e -> source.setException(e));

        } catch (Exception e) {
            source.setException(e);
        } finally {
            if (baos != null) {
                try {
                    baos.close();
                } catch (Exception e) {
                    // Log the error but don't fail the upload
                    e.printStackTrace();
                }
            }
        }
        
        return source.getTask();
    }

    public void getUser(String username, String password, OnCompleteListener<DataSnapshot> onCompleteListener) {
        if (username == null || password == null) {
            TaskCompletionSource<DataSnapshot> source = new TaskCompletionSource<>();
            source.setException(new IllegalArgumentException("Username and password cannot be null"));
            onCompleteListener.onComplete(source.getTask());
            return;
        }

        String hashedPassword = hashPassword(password);
        if (hashedPassword == null) {
            TaskCompletionSource<DataSnapshot> source = new TaskCompletionSource<>();
            source.setException(new Exception("Error processing password"));
            onCompleteListener.onComplete(source.getTask());
            return;
        }

        usersRef.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                TaskCompletionSource<DataSnapshot> source = new TaskCompletionSource<>();
                boolean userFound = false;

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String storedPassword = userSnapshot.child("password").getValue(String.class);
                    if (hashedPassword.equals(storedPassword)) {
                        source.setResult(userSnapshot);
                        onCompleteListener.onComplete(source.getTask());
                        userFound = true;
                        break;
                    }
                }

                if (!userFound) {
                    source.setException(new Exception("Invalid username or password"));
                    onCompleteListener.onComplete(source.getTask());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                TaskCompletionSource<DataSnapshot> source = new TaskCompletionSource<>();
                source.setException(databaseError.toException());
                onCompleteListener.onComplete(source.getTask());
            }
        });
    }

    public void deleteEmployeeProfile(String employeeId, OnCompleteListener<Void> onCompleteListener) {
        employeeRef.child(employeeId).removeValue().addOnCompleteListener(onCompleteListener);
    }

    public void updateCertificateUrl(String employeeId, String certUrl, OnCompleteListener<Void> onCompleteListener) {
        employeeRef.child(employeeId).child("certificateUrl").setValue(certUrl).addOnCompleteListener(onCompleteListener);
    }

    public void showNotification(String name) {
        // Check if the app has POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Request the permission (this must be done from an Activity)
                if (context instanceof Activity) {
                    ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
                }
                return; // Do not proceed with showing the notification if permission is not granted
            }
        }

        // Proceed to show the notification
        String channelId = "profile_notification_channel"; // Must match channelId in your MainActivity class
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification) // Replace with your app's icon
                .setContentTitle("New Profile Added")
                .setContentText("Profile for " + name + " has been added successfully!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(1, builder.build());
        }
    }


}
