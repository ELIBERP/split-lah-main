package com.example.split_lah;

import static android.content.ContentValues.TAG;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.os.Build;
import android.Manifest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.split_lah.databinding.ActivityMainBinding;
import com.example.split_lah.login_system.Login;
import com.example.split_lah.shared_view_model.SharedViewModel;
import com.example.split_lah.ui.home.HomeFragment;
import com.example.split_lah.ui.split.SplitBillBottomSheet;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DrawerLayout drawer;
    private Toolbar toolbar;
    private NavigationView navigationView;
    private BottomNavigationView bottomNav;
    private NavController navController;
    private SharedViewModel sharedViewModel;
    private String groupId;

    // Declare the launcher at the top of your Activity/Fragment:
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notifications ALLOWED", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Notifications DENIED", Toast.LENGTH_SHORT).show();
                }
            });

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                // Already granted
                // Toast.makeText(this, "Notifications already allowed", Toast.LENGTH_SHORT).show();
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                new AlertDialog.Builder(this)
                        .setTitle("Enable Notifications")
                        .setMessage("We use notifications to let you know about updates, reminders, and group activity.")
                        .setPositiveButton("Allow", (dialog, which) -> {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                        })
                        .setNegativeButton("No Thanks", (dialog, which) -> {
                            Toast.makeText(this, "Notifications will remain disabled.", Toast.LENGTH_SHORT).show();
                        })
                        .show();
            } else {
                // First time ask
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain.toolbar);

        sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);

        // Check if the Activity was opened with an extra value called "groupId"
        if (getIntent().hasExtra("groupId")) {
            // Retrieve the groupId value passed from the notification
            String groupId = getIntent().getStringExtra("groupId");
            // Create a bundle to pass the groupId to the fragment
            HomeFragment fragment = new HomeFragment();
            Bundle bundle = new Bundle();
            bundle.putString("groupId", groupId);
            sharedViewModel.setGroupId(groupId);
            sharedViewModel.setFromNotification(true);
            Log.d("FirebaseReceiver", "fromNotification set to true");
            // Create a new instance of the HomeFragment (which shows group bill details)
            // Attach the bundle to the fragment as its arguments
            fragment.setArguments(bundle);
            // Use the FragmentManager to replace the current fragment container
            // with the new DebtRelationFragment, showing the right group info
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment_content_main, fragment) //match the container
                    .commit(); //apply change
        }
//        else {
//            sharedViewModel.setFromNotification(false);
//        }
        askNotificationPermission();

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();

                        // Log and toast
                        String msg = getString(R.string.msg_token_fmt, token);
                        Log.d(TAG, msg);
                        Log.d("Push Notifications", msg);
                        //Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();

                        storeTokeninFirestore(token);
                    }
                });

        drawer = binding.drawerLayout;
        navigationView = binding.navView;

        setupViews();
        setupNavigation();
//        addNewBill();
        fetchUserData(); // Fetch group names from db
    }

    private void setupViews() {
        drawer = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        navigationView = findViewById(R.id.nav_view);
        bottomNav = findViewById(R.id.bottom_nav_view);
    }

    private void setupNavigation() {
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_settings, R.id.nav_debt_relation, R.id.nav_records)
                .setOpenableLayout(drawer)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        // Assuming you have a NavHostFragment with the ID nav_host_fragment_content_main in app_bar_main layout
//        NavigationUI.setupWithNavController(bottomNav, navController);
// Set up custom handling for bottom navigation
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            // Handle specific destinations
            if (id == R.id.nav_home || id == R.id.nav_settings ||
                    id == R.id.nav_debt_relation || id == R.id.nav_records) {

                // Don't navigate if we're already on this destination
                if (navController.getCurrentDestination().getId() != id) {
                    navController.navigate(id);
                }
                return true;
            } else if (id == R.id.nav_add_split) {
                // Handle the new expense item - show bottom sheet
                String groupId = sharedViewModel.getGroupId().getValue();
                if (groupId != null) {
                    SplitBillBottomSheet bottomSheet = new SplitBillBottomSheet(groupId);
                    bottomSheet.show(getSupportFragmentManager(), "splitBillBottomSheet");
                }
                return false; // Don't select this item in bottom nav
            }

            return false;
        });

        // ADD THIS: Listen for navigation changes and update bottom nav accordingly
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destId = destination.getId();

            // Check if this destination is in our bottom nav
            if (destId == R.id.nav_home) {
                bottomNav.setSelectedItemId(R.id.nav_home);
            } else if (destId == R.id.nav_settings) {
                bottomNav.setSelectedItemId(R.id.nav_settings);
            } else if (destId == R.id.nav_debt_relation) {
                bottomNav.setSelectedItemId(R.id.nav_debt_relation);
            } else if (destId == R.id.nav_records) {
                bottomNav.setSelectedItemId(R.id.nav_records);
            }
            // Don't update selection for destinations not in the bottom nav
        });

        NavigationUI.setupWithNavController(navigationView, navController);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
    }

    public NavController getNavController() {
        return navController;
    }

    public void fetchUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        // Fetch the user's data including friends list
        db.collection("users")
                .document(userId)
                .get()
                .addOnCompleteListener(userTask -> {
                    if (userTask.isSuccessful() && userTask.getResult().exists()) {
                        String firstName = userTask.getResult().getString("first_name");
                        String icon = userTask.getResult().getString("icon");

                        // Retrieve friends list
                        List<String> friends = (List<String>) userTask.getResult().get("friends");
                        if (friends == null) {
                            friends = new ArrayList<>(); // Initialize with empty list if null
                        }

                        // Add data to SharedViewModel
                        sharedViewModel.setUserFirstName(firstName);
                        sharedViewModel.setUserIcon(icon);
                        sharedViewModel.setFriendsList(friends); // Add this to store friends list

                        Log.d("MainActivity", "Set user first name: " + firstName);
                        Log.d("MainActivity", "Retrieved " + friends.size() + " friends");

                        // Now continue with fetching groups
                        fetchUserGroups(userId);
                    } else {
                        Log.e("MainActivity", "Failed to fetch user data", userTask.getException());
                        fetchUserGroups(userId); // Continue anyway to show groups
                    }
                });
    }

    private void fetchUserGroups(String userId) {
        CollectionReference groupsRef = db.collection("permanent_grp");

        // Query for the groups where the logged-in user is a member
        groupsRef.whereArrayContains("members", userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Menu menu = navigationView.getMenu();
                        menu.clear();

                        if (!task.getResult().isEmpty()) {
                            // Add dynamic group items at the top
                            int index = 0;
                            for (DocumentSnapshot groupDoc : task.getResult()) {
                                String documentId = groupDoc.getId();  // Get the document ID for the group
                                String groupName = groupDoc.getString("group_name"); // Retrieve the group name

                                groupId = documentId;
                                if (groupName != null) {
                                    updateNavMenu(documentId, groupName, index); // Pass the document ID and group name
                                    if (index == 0) {
                                        sharedViewModel.setGroupId(documentId);
                                        sharedViewModel.setGroupName(groupName);

                                        Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
                                                .navigate(R.id.nav_home);
                                    }
                                    index++;
                                }
                            }
                        } else {
                            updateNavMenu("No Groups Found", "", -1);
                        }

                        // Add the static "New Group" and "New Temporary Group" items after the dynamic ones
                        addStaticGroupItems(menu);
                    } else {
                        updateNavMenu("Error Fetching Groups", "", -1);
                    }
                });
    }

    private void updateNavMenu(String documentId, String groupName, int index) {
        Menu menu = navigationView.getMenu();

        MenuItem menuItem = menu.add(Menu.NONE, index, Menu.NONE, groupName)
                .setCheckable(false);

        // Store the documentId and groupName in the Intent associated with the MenuItem
        Intent itemIntent = new Intent();
        itemIntent.putExtra("documentId", documentId);
        itemIntent.putExtra("groupName", groupName);
        menuItem.setIntent(itemIntent);

        menuItem.setOnMenuItemClickListener(item -> {
            // Retrieve the documentId and groupName from the Intent when menu item is clicked
            String retrievedDocId = item.getIntent().getStringExtra("documentId");
            String retrievedGroupName = item.getIntent().getStringExtra("groupName");

            if (retrievedDocId != null && retrievedGroupName != null) {
                // Fetch the additional fields (currency_code, members) from Firestore
                db.collection("permanent_grp")
                        .document(retrievedDocId)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                DocumentSnapshot groupDoc = task.getResult();
                                if (groupDoc.exists()) {
                                    // Retrieve the fields from the document
                                    String currencyCode = groupDoc.getString("currency_code");
                                    List<String> members = (List<String>) groupDoc.get("members");

                                    // Pass the retrieved data to the SharedViewModel
                                    sharedViewModel.setGroupId(retrievedDocId);
                                    sharedViewModel.setGroupName(retrievedGroupName);
                                    sharedViewModel.setCurrencyCode(currencyCode);
                                    sharedViewModel.setMembers(members);

                                    Navigation.findNavController(this, R.id.nav_host_fragment_content_main)
                                            .navigate(R.id.nav_home);

                                    drawer.closeDrawer(Gravity.LEFT);
                                }
                            } else {
                                Log.d("Firestore", "Error getting document: ", task.getException());
                            }
                        });
            }
            return true;
        });
    }

    private void addStaticGroupItems(Menu menu) {
        menu.add(Menu.NONE, R.id.nav_new_perm_grp, Menu.NONE, getString(R.string.menu_new_group))
                .setIcon(R.drawable.baseline_add_box_24);
        menu.add(Menu.NONE, R.id.nav_new_temp_grp, Menu.NONE, getString(R.string.menu_new_temporary_group))
                .setIcon(R.drawable.baseline_add_box_24);
    }

//    private void addNewBill() {
//        binding.appBarMain.fab.setOnClickListener(view -> {
            // Get the current group ID from the SharedViewModel
//            String groupId = sharedViewModel.getGroupId().getValue();

//            if (groupId != null) {
//                // Directly access SharedViewModel in SplitBillBottomSheet
//                SplitBillBottomSheet bottomSheet = new SplitBillBottomSheet(groupId);
//                bottomSheet.show(getSupportFragmentManager(), "splitBillBottomSheet");
//            }
//        });
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // Handle logout menu item click
        if (id == R.id.action_logout) {
            logout();
            return true; // Return true to indicate the click was handled
        }

        return super.onOptionsItemSelected(item); // Let the system handle other items
    }

    private void logout() {
        // Sign out the user
        mAuth.signOut();

        // Clear "Remember Me" state
        SharedPreferences sharedPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("rememberMe", false); // Clear the "Remember Me" state
        editor.apply();

        // Show a toast message
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Navigate to the login screen
        Intent intent = new Intent(this, Login.class);
        startActivity(intent);
        finish(); // Close MainActivity
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // User is not signed in, redirect to LoginActivity
            Intent intent = new Intent(this, Login.class);
            startActivity(intent);
            finish(); // Close MainActivity to prevent going back
        }
    }

    private void storeTokeninFirestore(String newToken) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        db.collection("users")
                .document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String storedToken = document.getString("fcm_token");

                            if (storedToken == null || !storedToken.equals(newToken)) {
                                db.collection("users").document(userId)
                                        .update("fcm_token", newToken)  // Update the token in Firestore
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "Token updated successfully");
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.w(TAG, "Error updating token", e);
                                        });
                            } else {
                                // If the token is the same, log that no update is needed
                                Log.d(TAG, "Token is the same, no update needed");
                            }
                        } else {
                            db.collection("users").document(userId)
                                    .set(new HashMap<String, Object>() {{
                                        put("fcm_token", newToken);  // Store the FCM token in the "fcm_token" field
                                    }})
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "FCM token stored successfully");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.w(TAG, "Error storing token", e);
                                    });
                        }
                    } else {
                        Log.w(TAG, "Error getting user document", task.getException());
                    }
                });
    }
}