package com.example.staffprofile;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.regex.Pattern;

public class Profile {
    private final String id;
    private final String name;
    private final String jobTitle;
    private final String skills;
    private final String certifications;
    private final String photoUrl;
    private final String email;
    private final String phone;
    private final String experience;
    private final String about;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9+._%\\-]{1,256}" +
        "@" +
        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
        "(" +
        "\\." +
        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
        ")+"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9]{10,13}$");

    private Profile(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.jobTitle = builder.jobTitle;
        this.skills = builder.skills;
        this.certifications = builder.certifications;
        this.photoUrl = builder.photoUrl;
        this.email = builder.email;
        this.phone = builder.phone;
        this.experience = builder.experience;
        this.about = builder.about;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getSkills() {
        return skills;
    }

    public String getCertifications() {
        return certifications;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getExperience() {
        return experience;
    }

    public String getAbout() {
        return about;
    }

    public static class Builder {
        private String id;
        private String name;
        private String jobTitle;
        private String skills;
        private String certifications;
        private String photoUrl;
        private String email;
        private String phone;
        private String experience;
        private String about;

        public Builder() {}

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setJobTitle(String jobTitle) {
            this.jobTitle = jobTitle;
            return this;
        }

        public Builder setSkills(String skills) {
            this.skills = skills;
            return this;
        }

        public Builder setCertifications(String certifications) {
            this.certifications = certifications;
            return this;
        }

        public Builder setPhotoUrl(String photoUrl) {
            this.photoUrl = photoUrl;
            return this;
        }

        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder setPhone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder setExperience(String experience) {
            this.experience = experience;
            return this;
        }

        public Builder setAbout(String about) {
            this.about = about;
            return this;
        }

        public Profile build() throws IllegalArgumentException {
            validateRequiredFields();
            validateEmail();
            validatePhone();
            return new Profile(this);
        }

        private void validateRequiredFields() throws IllegalArgumentException {
            if (TextUtils.isEmpty(id)) {
                throw new IllegalArgumentException("Profile ID is required");
            }
            if (TextUtils.isEmpty(name)) {
                throw new IllegalArgumentException("Name is required");
            }
            if (TextUtils.isEmpty(jobTitle)) {
                throw new IllegalArgumentException("Job title is required");
            }
        }

        private void validateEmail() throws IllegalArgumentException {
            if (!TextUtils.isEmpty(email) && !EMAIL_PATTERN.matcher(email).matches()) {
                throw new IllegalArgumentException("Invalid email format");
            }
        }

        private void validatePhone() throws IllegalArgumentException {
            if (!TextUtils.isEmpty(phone) && !PHONE_PATTERN.matcher(phone).matches()) {
                throw new IllegalArgumentException("Invalid phone number format");
            }
        }
    }

    @Override
    @NonNull
    public String toString() {
        return "Profile{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", jobTitle='" + jobTitle + '\'' +
                '}';
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Profile profile = (Profile) obj;
        return id.equals(profile.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
