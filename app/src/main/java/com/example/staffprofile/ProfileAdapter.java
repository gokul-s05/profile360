package com.example.staffprofile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import java.util.List;

public class ProfileAdapter extends ArrayAdapter<Profile> {
    private final Context context;
    private final List<Profile> profiles;
    private final RequestOptions glideOptions;

    public ProfileAdapter(Context context, List<Profile> profiles) {
        super(context, R.layout.profile_list_item, profiles);
        this.context = context;
        this.profiles = profiles;

        // Configure Glide options
        glideOptions = new RequestOptions()
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.profile_list_item, parent, false);
            holder = new ViewHolder();
            holder.imageViewProfile = convertView.findViewById(R.id.imageViewProfile);
            holder.textViewName = convertView.findViewById(R.id.textViewName);
            holder.textViewJobTitle = convertView.findViewById(R.id.textViewJobTitle);
            holder.textViewSkills = convertView.findViewById(R.id.textViewSkills);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Profile profile = profiles.get(position);
        if (profile != null) {
            // Load profile image
            if (profile.getPhotoUrl() != null && !profile.getPhotoUrl().isEmpty()) {
                Glide.with(context)
                        .load(profile.getPhotoUrl())
                        .apply(glideOptions)
                        .into(holder.imageViewProfile);
            } else {
                holder.imageViewProfile.setImageResource(R.drawable.ic_profile_placeholder);
            }

            // Set text fields
            holder.textViewName.setText(profile.getName());
            holder.textViewJobTitle.setText(profile.getJobTitle());
            
            String skills = profile.getSkills();
            if (skills != null && !skills.isEmpty()) {
                holder.textViewSkills.setVisibility(View.VISIBLE);
                holder.textViewSkills.setText(context.getString(R.string.skills_format, skills));
            } else {
                holder.textViewSkills.setVisibility(View.GONE);
            }
        }

        return convertView;
    }

    @Override
    public int getCount() {
        return profiles != null ? profiles.size() : 0;
    }

    @Nullable
    @Override
    public Profile getItem(int position) {
        return position >= 0 && position < profiles.size() ? profiles.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        Profile profile = getItem(position);
        return profile != null ? profile.getId().hashCode() : 0;
    }

    public void updateProfiles(List<Profile> newProfiles) {
        profiles.clear();
        if (newProfiles != null) {
            profiles.addAll(newProfiles);
        }
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        ImageView imageViewProfile;
        TextView textViewName;
        TextView textViewJobTitle;
        TextView textViewSkills;
    }
}
