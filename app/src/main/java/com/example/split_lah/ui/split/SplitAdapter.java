package com.example.split_lah.ui.split;

import static java.lang.Double.parseDouble;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.split_lah.R;
import com.example.split_lah.models.IconUtils;
import com.example.split_lah.models.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SplitAdapter extends RecyclerView.Adapter<SplitAdapter.ViewHolder> {
    private List<User> userList;
    private List<String> checkedUsers = new ArrayList<>();
    private OnCheckedUsersChangedListener listener;

    // Interface for callbacks when checked users change
    public interface OnCheckedUsersChangedListener {
        void onCheckedUsersChanged(List<String> checkedUsers);
    }

    // Set the listener
    public void setOnCheckedUsersChangedListener(OnCheckedUsersChangedListener listener) {
        this.listener = listener;
    }

    public SplitAdapter(List<User> userList) {
        this.userList = userList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_split_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = userList.get(position);
        holder.userName.setText(user.getFirstName() + " " + user.getLastName());
        holder.userSplit.setText(user.getSplit());
        String icon = user.getIcon();

        // Placeholder for profile image
        holder.userImage.setImageResource(IconUtils.getIconResourceId(icon));
        holder.userCheckbox.setChecked(user.isSelected());

        // Toggle checkbox for selection
        holder.userCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            user.setSelected(isChecked);

            // track which users are involved in the split
            if (isChecked) {
                checkedUsers.add(user.getId());
            } else {
                checkedUsers.remove(user.getId());
            }
            Log.d("SaveBill", "Checkbox checked users: " + checkedUsers.toString());

            // Notify listener about the change
            if (listener != null) {
                listener.onCheckedUsersChanged(checkedUsers);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public List<User> getSelectedUsers() {
        return userList;
    }

    public List<String> getCheckedUsers() {
        return checkedUsers;
    }

    public void setSplits(HashMap<String, String> split) {
        // Iterate through all user in the map and set their split values
        for (Map.Entry<String, String> entry : split.entrySet()) {
            String userId = entry.getKey();
            String splitValue = entry.getValue();

            for (User user : userList) {
                if (user.getId().equals(userId)) {
                    user.setSplit(splitValue);
                    notifyDataSetChanged();
                    break; // Exit the inner loop since we found the correct user
                }
            }
        }

        // Iterate through the list of users and set the split value to 0 if the user was not in the split map
        for(User user: userList){
            boolean presentInSplit = false;
            for(String key: split.keySet()){
                if(user.getId().equals(key)){
                    presentInSplit = true;
                    break;
                }
            }
            if (!presentInSplit){
                user.setSplit(String.valueOf("0.00"));
                notifyDataSetChanged();
            }
        }
    }

    public HashMap<String, String> getSplits() {
        HashMap<String, String> splits = new HashMap<>();
        for (User user : userList) {
            splits.put(user.getId(), user.getSplit());
        }
        return splits;
    }

    public boolean checkIfSplitsAreBalanced(String totalAmount) {
        double totalSplit = 0.0;
        for (User user : userList) {
            totalSplit += parseDouble(user.getSplit());
        }
        return totalSplit == parseDouble(totalAmount);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView userName;
        ImageView userImage;
        CheckBox userCheckbox;
        TextView userSplit;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.user_name);
            userImage = itemView.findViewById(R.id.user_image);
            userCheckbox = itemView.findViewById(R.id.user_checkbox);
            userSplit = itemView.findViewById(R.id.user_split);
        }
    }
}
