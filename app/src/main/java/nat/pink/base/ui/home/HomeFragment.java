package nat.pink.base.ui.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.utils.widget.ImageFilterView;
import androidx.core.util.Consumer;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import nat.pink.base.MainActivity;
import nat.pink.base.R;
import nat.pink.base.base.BaseFragment;
import nat.pink.base.customView.ExtTextView;
import nat.pink.base.dao.DatabaseController;
import nat.pink.base.databinding.HomeFragmentBinding;
import nat.pink.base.dialog.DialogErrorLink;
import nat.pink.base.dialog.DialogLoading;
import nat.pink.base.dialog.DialogShowError;
import nat.pink.base.dialog.DialogShowTimer;
import nat.pink.base.model.ObjectCalling;
import nat.pink.base.model.ObjectSpin;
import nat.pink.base.model.ObjectsContentSpin;
import nat.pink.base.utils.Config;
import nat.pink.base.utils.Const;
import nat.pink.base.utils.PreferenceUtil;
import nat.pink.base.utils.StringUtils;
import nat.pink.base.utils.UriUtils;
import nat.pink.base.utils.Utils;

public class HomeFragment extends BaseFragment<HomeFragmentBinding, HomeViewModel> {
    public static final String TAG = "HomeFragment";

    @NonNull
    @Override
    public HomeViewModel getViewModel() {
        return new ViewModelProvider(getActivity()).get(HomeViewModel.class);
    }

    private ObjectCalling objectIncoming = new ObjectCalling();
    private ObjectCalling objectCalling = new ObjectCalling();
    private boolean isCalling;
    private Context context;

    @Override
    public void initView() {
        super.initView();
        showIncomingCall(true);
    }


    @Override
    public void initData() {
        super.initData();
        objectIncoming.setCalling(false);
        objectCalling.setCalling(true);
    }

    @Override
    public void initEvent() {
        super.initEvent();
        binding.txtIncomingCall.setOnClickListener(v -> showIncomingCall(true));
        binding.txtCalling.setOnClickListener(v -> showIncomingCall(false));
        binding.edtName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (isCalling)
                    objectCalling.setName(charSequence.toString());
                else
                    objectIncoming.setName(charSequence.toString());
                binding.edtError.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        binding.txtTimer.setOnClickListener(view -> {
            DialogShowTimer dialogShowTimer = new DialogShowTimer(getContext(), R.style.MaterialDialogSheet, isCalling ? objectCalling.getTimer() : objectIncoming.getTimer(), true, o -> {
                Utils.hiddenKeyboard(getActivity(), binding.edtName);
                if (isCalling)
                    objectCalling.setTimer(o);
                else
                    objectIncoming.setTimer(o);
                binding.txtTimer.setText(Utils.getStringTimer(getContext(), o));
            });
            dialogShowTimer.setPickup(false, false, true);
            dialogShowTimer.show();
            dialogShowTimer.setCanceledOnTouchOutside(true);
        });
        binding.extChangePicture.setOnClickListener(view -> {
            Utils.hiddenKeyboard(getActivity(), binding.edtName);
            Utils.openGallery(getActivity(), false);
        });
        binding.layoutActionBar.txtAction.setOnClickListener(v -> {
            if (checkError()) {
                binding.edtError.setVisibility(View.VISIBLE);
                return;
            } else if (checkErrorPath()) {
                new DialogShowError(getContext(), R.style.MaterialDialogSheet, o -> {
                }).show();
                return;
            }

            actionDone();

        });
    }

    private void actionDone() {
        if (isCalling) {
            if (objectCalling.getTimer() == Const.KEY_TIME_NOW) {
                Intent intent = new Intent(requireContext(), OutCommingActivity.class);
                intent.putExtra(Const.PUT_EXTRAL_OBJECT_CALL, objectCalling);
                intent.putExtra("show_icon_video", true);
                startActivityForResult(intent, Config.CHECK_TURN_OFF_VOICE);
            } else {
                PreferenceUtil.saveLong(requireContext(), PreferenceUtil.KEY_CURRENT_TIME, System.currentTimeMillis() + Utils.getTimeFromKey(requireContext(), objectCalling.getTimer()));
                PreferenceUtil.saveKey(requireContext(), PreferenceUtil.KEY_CALLING_VOICE);
                Utils.startAlarmService(requireActivity(), Utils.getTimeFromKey(requireContext(), objectCalling.getTimer()), Const.ACTION_CALL_VOICE, objectCalling);
                backStackFragment();
            }
        } else {
            if (objectIncoming.getTimer() == Const.KEY_TIME_NOW) {
                Intent intent = new Intent(getActivity(), VideoCallActivity.class);
                intent.putExtra("show_icon_video", true);
                intent.putExtra(Const.PUT_EXTRAL_OBJECT_CALL, objectIncoming);
                startActivityForResult(intent, Config.CHECK_TURN_OFF_VOICE);
            } else {
                PreferenceUtil.saveLong(requireContext(), PreferenceUtil.KEY_CURRENT_TIME, System.currentTimeMillis() + Utils.getTimeFromKey(requireContext(), objectIncoming.getTimer()));
                PreferenceUtil.saveKey(requireContext(), PreferenceUtil.KEY_COMMING_VOICE);
                Utils.startAlarmService(requireActivity(), Utils.getTimeFromKey(requireContext(), objectIncoming.getTimer()), Const.ACTION_COMMING_VOICE, objectIncoming);
                backStackFragment();
            }
        }
    }

    @Override
    public void onDestroy() {
        Utils.hiddenKeyboard(getActivity(), binding.edtName);
        super.onDestroy();
    }

    @Override
    public void onPause() {
        Utils.hiddenKeyboard(getActivity(), binding.edtName);
        super.onPause();
    }

    private void showIncomingCall(boolean isShow) {
        isCalling = !isShow;
        binding.txtIncomingCall.setBackgroundResource(isShow ? R.drawable.bg_button_call_on : R.drawable.bg_button_call_off);
        binding.txtCalling.setBackgroundResource(!isShow ? R.drawable.bg_button_call_on : R.drawable.bg_button_call_off);
        binding.txtIncomingCall.setTextColor(getResources().getColor(isShow ? R.color.white : R.color.color_9E9E9E));
        binding.txtCalling.setTextColor(getResources().getColor(!isShow ? R.color.white : R.color.color_9E9E9E));

        // set name
        binding.edtName.setText(isCalling ? objectCalling.getName() : objectIncoming.getName());

        // set timer
        binding.txtTimer.setText(Utils.getStringTimer(getContext(), isCalling ? objectCalling.getTimer() : objectIncoming.getTimer()));

        // check uri image

        Uri image = isCalling ? (objectCalling.getPathImage() == null || objectCalling.getPathImage().equals("") ? null : Uri.parse(objectCalling.getPathImage()))
                : (objectIncoming.getPathImage() == null || objectIncoming.getPathImage().equals("") ? null : Uri.parse(objectIncoming.getPathImage()));
        if (image != null)
            Glide.with(getContext()).load(image).into(binding.ivChangePic);
        else
            Glide.with(getContext()).load(R.drawable.ic_circe_outgoing).into(binding.ivChangePic);

        binding.txtTitleTime.setText(isCalling ? getResources().getText(R.string.pick_up_the_phone_later) : getResources().getText(R.string.call_later));
        binding.layoutActionBar.txtTitle.setText(getResources().getText(R.string.make_voice_call_title));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Const.REQUEST_CODE_GALLERY) {
            Utils.openGallery(getActivity(), false);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Const.ALBUM_REQUEST_CODE && data != null && data.getData() != null) {
            if (isCalling)
                objectCalling.setPathImage(data.getData().toString());
            else {
                objectIncoming.setPathImage(data.getData().toString());
            }
            Glide.with(getContext()).load(data.getData()).into(binding.ivChangePic);
        } else if (requestCode == Config.CHECK_TURN_OFF_VOICE) {
            if (resultCode == OutCommingActivity.RESULT_PAUSE)
                backStackFragment();
        }
    }

    private boolean checkError() {
        if (isCalling) {
            if (objectCalling.getName().trim().isEmpty()) {
                binding.edtError.setText(getResources().getText(R.string.please_fill_in_the_information));
                return true;
            } else if (objectCalling.getName().trim().length() > 25) {
                binding.edtError.setText(getResources().getText(R.string.maxium_characters));
                return true;
            }
        } else {
            if (objectIncoming.getName().trim().isEmpty()) {
                binding.edtError.setText(getResources().getText(R.string.please_fill_in_the_information));
                return true;
            } else if (objectIncoming.getName().trim().length() > 25) {
                binding.edtError.setText(getResources().getText(R.string.maxium_characters));
                return true;
            }
        }
        return false;
    }

    private boolean checkErrorPath() {
        if (isCalling) {
            return objectCalling.getPathImage() == null;
        } else {
            return objectIncoming.getPathImage() == null;
        }
    }
}
