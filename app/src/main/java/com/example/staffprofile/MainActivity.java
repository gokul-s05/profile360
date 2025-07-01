package com.example.staffprofile;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_MANAGE_ALL_FILES = 1;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 2;
    private static final String CHANNEL_ID = "profile_notification_channel";
    
    private ListView listViewProfiles;
    private EditText searchBar;
    private DatabaseHelper databaseHelper;
    private ArrayList<String> profileList;
    private ArrayList<String> filteredProfileList;
    private ArrayList<String> employeeIds;
    private ArrayList<String> filteredEmployeeIds;
    private ArrayAdapter<String> adapter;
    private Button buttonAddProfile;
    private ProgressBar progressBar;
    private boolean isDataLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupListeners();
        checkAndRequestPermissions();

        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        } else {
            initializeLists();
        }
    }

    private void initializeViews() {
        listViewProfiles = findViewById(R.id.listViewProfiles);
        searchBar = findViewById(R.id.searchBar);
        buttonAddProfile = findViewById(R.id.buttonAddProfile);
        progressBar = findViewById(R.id.progressBar);
        
        databaseHelper = new DatabaseHelper(this);
    }

    private void initializeLists() {
        profileList = new ArrayList<>();
        employeeIds = new ArrayList<>();
        filteredProfileList = new ArrayList<>();
        filteredEmployeeIds = new ArrayList<>();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filteredProfileList);
        listViewProfiles.setAdapter(adapter);

        loadProfileData();
    }

    private void restoreState(Bundle savedInstanceState) {
        profileList = savedInstanceState.getStringArrayList("profileList");
        employeeIds = savedInstanceState.getStringArrayList("employeeIds");
        filteredProfileList = savedInstanceState.getStringArrayList("filteredProfileList");
        filteredEmployeeIds = savedInstanceState.getStringArrayList("filteredEmployeeIds");
        isDataLoaded = savedInstanceState.getBoolean("isDataLoaded");

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filteredProfileList);
        listViewProfiles.setAdapter(adapter);

        if (!isDataLoaded) {
            loadProfileData();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("profileList", profileList);
        outState.putStringArrayList("employeeIds", employeeIds);
        outState.putStringArrayList("filteredProfileList", filteredProfileList);
        outState.putStringArrayList("filteredEmployeeIds", filteredEmployeeIds);
        outState.putBoolean("isDataLoaded", isDataLoaded);
    }

    private void setupListeners() {
        listViewProfiles.setOnItemClickListener((parent, view, position, id) -> {
            String employeeId = filteredEmployeeIds.get(position);
            Intent intent = new Intent(MainActivity.this, ViewProfileActivity.class);
            intent.putExtra("employeeId", employeeId);
            startActivity(intent);
        });

        buttonAddProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddProfileActivity.class);
            startActivity(intent);
        });

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProfiles(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_MANAGE_ALL_FILES);
            }
        }

        createNotificationChannel();
    }

    private void loadProfileData() {
        showLoading(true);
        databaseHelper.getAllProfiles(task -> {
            if (!isFinishing() && !isDestroyed()) {
                if (task.isSuccessful() && task.getResult() != null) {
                    profileList.clear();
                    employeeIds.clear();
                    filteredProfileList.clear();
                    filteredEmployeeIds.clear();

                    for (DataSnapshot snapshot : task.getResult().getChildren()) {
                        String employeeId = snapshot.getKey();
                        HashMap<String, Object> employeeData = (HashMap<String, Object>) snapshot.getValue();

                        if (employeeId != null && employeeData != null) {
                            String name = (String) employeeData.get("name");
                            if (name != null) {
                                profileList.add(name);
                                employeeIds.add(employeeId);
                            }
                        }
                    }

                    filteredProfileList.addAll(profileList);
                    filteredEmployeeIds.addAll(employeeIds);
                    adapter.notifyDataSetChanged();
                    isDataLoaded = true;

                    if (profileList.isEmpty() && !isFinishing()) {
                        Toast.makeText(MainActivity.this, "No profiles found.", Toast.LENGTH_SHORT).show();
                    }
                } else if (!isFinishing()) {
                    String error = task.getException() != null ? task.getException().getMessage() : "Error loading profiles.";
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                }
                showLoading(false);
            }
        });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            listViewProfiles.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @SuppressLint("NewApi")
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence channelName = "Profile Notifications";
            String description = "Notification channel for new profiles";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, channelName, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied. Some features may be limited.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void filterProfiles(String query) {
        filteredProfileList.clear();
        filteredEmployeeIds.clear();

        String lowercaseQuery = query.toLowerCase().trim();
        for (int i = 0; i < profileList.size(); i++) {
            if (profileList.get(i).toLowerCase().contains(lowercaseQuery)) {
                filteredProfileList.add(profileList.get(i));
                filteredEmployeeIds.add(employeeIds.get(i));
            }
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isDataLoaded) {
            loadProfileData();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up any remaining resources
        databaseHelper = null;
    }
}
