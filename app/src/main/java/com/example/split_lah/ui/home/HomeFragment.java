package com.example.split_lah.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.split_lah.MainActivity;
import com.example.split_lah.R;
import com.example.split_lah.databinding.FragmentHomeBinding;
import com.example.split_lah.shared_view_model.SharedViewModel;
import com.example.split_lah.ui.debt_relation.DebtRelationViewModel;
import com.example.split_lah.ui.home.components.DebtRelationManager;
import com.example.split_lah.ui.home.components.MemberIconsManager;
import com.example.split_lah.ui.home.components.NavigationManager;
import com.example.split_lah.ui.home.components.NetBalancesManager;
import com.example.split_lah.ui.home.components.RecentSplitsManager;
import com.example.split_lah.ui.home.components.UserInfoManager;
import com.example.split_lah.ui.home.viewmodels.HomeSpendingViewModel;
import com.example.split_lah.ui.members.MembersViewModel;
import com.example.split_lah.ui.net_balances.NetBalancesViewModel;
import com.example.split_lah.ui.records.RecordsViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeFragment extends Fragment {
    // Binding
    private FragmentHomeBinding binding;

    // Data source
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String currentUserId;

    // ViewModels
    private DebtRelationViewModel debtRelationViewModel;
    private MembersViewModel membersViewModel;
    private NetBalancesViewModel netBalancesViewModel;
    private HomeSpendingViewModel homeSpendingViewModel;
    private RecordsViewModel recordsViewModel;
    private SharedViewModel sharedViewModel;

    // Component managers
    private HomeViewModelCoordinator viewModelCoordinator;
    private UserInfoManager userInfoManager;
    private MemberIconsManager memberIconsManager;
    private RecentSplitsManager recentSplitsManager;
    private DebtRelationManager debtRelationManager;
    private NetBalancesManager netBalancesManager;
    private NavigationManager navigationManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        initializeCurrentUser();
        initializeViewModels();
        initializeManagers();
        setupUI();

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set up the observer for notification-related navigation
        sharedViewModel.getFromNotification().observe(getViewLifecycleOwner(), fromNotification -> {
            if (getView() != null && fromNotification) {
                NavController navController = Navigation.findNavController(view);
                navController.navigate(R.id.nav_debt_relation);
            }
        });
    }

    private void initializeCurrentUser() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }
    }

    private void initializeViewModels() {
        // Initialize all ViewModels
        debtRelationViewModel = new ViewModelProvider(requireActivity()).get(DebtRelationViewModel.class);
        recordsViewModel = new ViewModelProvider(requireActivity()).get(RecordsViewModel.class);
        membersViewModel = new ViewModelProvider(requireActivity()).get(MembersViewModel.class);
        netBalancesViewModel = new ViewModelProvider(requireActivity()).get(NetBalancesViewModel.class);
        homeSpendingViewModel = new ViewModelProvider(this).get(HomeSpendingViewModel.class);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // Create coordinator to manage ViewModel dependencies
        viewModelCoordinator = new HomeViewModelCoordinator(
                getViewLifecycleOwner(),
                debtRelationViewModel,
                membersViewModel,
                netBalancesViewModel,
                homeSpendingViewModel,
                recordsViewModel,
                sharedViewModel,
                currentUserId
        );

        viewModelCoordinator.setupUserAndGroupObservers();
    }


    private void initializeManagers() {
        // Create all component managers
        userInfoManager = new UserInfoManager(
                binding,
                getViewLifecycleOwner(),
                sharedViewModel,
                homeSpendingViewModel
        );

        memberIconsManager = new MemberIconsManager(
                binding,
                getViewLifecycleOwner(),
                membersViewModel,
                sharedViewModel,
                currentUserId
        );

        recentSplitsManager = new RecentSplitsManager(
                requireContext(),
                getViewLifecycleOwner(),
                binding.recyclerViewHomeRecentSplit,
                binding.emptyStateHomeRecentSplits,
                recordsViewModel,
                sharedViewModel,
                ((MainActivity) requireActivity()).getNavController()
        );

        debtRelationManager = new DebtRelationManager(
                requireContext(),
                getViewLifecycleOwner(),
                binding.recyclerViewHomeDebtRelations,
                binding.emptyStateHomeDebtRelation,
                debtRelationViewModel,
                sharedViewModel
        );

        netBalancesManager = new NetBalancesManager(
                requireContext(),
                getViewLifecycleOwner(),
                binding.recyclerViewHomeNetBalances,
                binding.emptyStateHomeNetBalances,
                netBalancesViewModel,
                sharedViewModel
        );

        navigationManager = new NavigationManager(
                binding,
                ((MainActivity) requireActivity()).getNavController()
        );
    }

    private void setupUI() {
        // Setup all UI components using managers
        userInfoManager.setup();
        memberIconsManager.setup();
        recentSplitsManager.setup();
        debtRelationManager.setup();
        netBalancesManager.setup();
        navigationManager.setupNavigationButtons();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Force refresh when returning to this fragment
        viewModelCoordinator.forceRefreshAllData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
