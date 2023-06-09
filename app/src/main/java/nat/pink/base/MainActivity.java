package nat.pink.base;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;

import nat.pink.base.databinding.ActivityMainBinding;
import nat.pink.base.model.ObjectLocation;
import nat.pink.base.ui.home.HomeFragment;
import nat.pink.base.ui.tool.FragmentMain;
import nat.pink.base.ui.tool.FragmentSplash;
import nat.pink.base.utils.Const;
import nat.pink.base.utils.PreferenceUtil;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ArrayList<String> fragmentStates = new ArrayList<>();
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        fragmentManager = getSupportFragmentManager();
        initView();
        initData();
    }

    private void initData() {
        if (PreferenceUtil.getBoolean(this, Const.FIRST_APP, true)) {
            addFragment(new FragmentSplash(), FragmentSplash.TAG);
            return;
        }
        ObjectLocation objectLocation = PreferenceUtil.getFirstApp(this);
        if (objectLocation != null) {
            if (objectLocation.getLct().equals("true")) {
                replaceFragment(new FragmentMain(), FragmentMain.TAG);
            } else {
                replaceFragment(new HomeFragment(), HomeFragment.TAG);
            }
            return;
        }
        replaceFragment(new HomeFragment(), HomeFragment.TAG);
    }

    private void initView() {
    }

    public void replaceFragment(Fragment fragment, String tag) {
        fragmentManager.beginTransaction()
                .replace(R.id.content, fragment, tag)
                .addToBackStack(tag)
                .commit();
        if (!fragmentStates.contains(tag))
            fragmentStates.add(tag);
    }

    public void addFragment(Fragment fragment, String tag) {
        fragmentManager.beginTransaction()
                .add(R.id.content, fragment, tag)
                .addToBackStack(tag)
                .commit();
        if (!fragmentStates.contains(tag))
            fragmentStates.add(tag);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (fragmentStates.size() > 1 && !fragmentStates.get(fragmentStates.size() - 2).contains(FragmentSplash.TAG)) {
            getSupportFragmentManager().popBackStack(fragmentStates.get(fragmentStates.size() - 1), FragmentManager.POP_BACK_STACK_INCLUSIVE);
            fragmentStates.remove(fragmentStates.size() - 1);
            return;
        }
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        for (Fragment item : getSupportFragmentManager().getFragments()) {
            item.onActivityResult(requestCode, resultCode, data);
        }
    }
}