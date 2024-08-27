// Generated by view binder compiler. Do not edit!
package com.firebase.storages.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.firebase.storages.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class DialogChangephotoBinding implements ViewBinding {
  @NonNull
  private final RelativeLayout rootView;

  @NonNull
  public final TextView dialogChoosePhoto;

  @NonNull
  public final TextView dialogOpenCamera;

  private DialogChangephotoBinding(@NonNull RelativeLayout rootView,
      @NonNull TextView dialogChoosePhoto, @NonNull TextView dialogOpenCamera) {
    this.rootView = rootView;
    this.dialogChoosePhoto = dialogChoosePhoto;
    this.dialogOpenCamera = dialogOpenCamera;
  }

  @Override
  @NonNull
  public RelativeLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static DialogChangephotoBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static DialogChangephotoBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.dialog_changephoto, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static DialogChangephotoBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.dialogChoosePhoto;
      TextView dialogChoosePhoto = ViewBindings.findChildViewById(rootView, id);
      if (dialogChoosePhoto == null) {
        break missingId;
      }

      id = R.id.dialogOpenCamera;
      TextView dialogOpenCamera = ViewBindings.findChildViewById(rootView, id);
      if (dialogOpenCamera == null) {
        break missingId;
      }

      return new DialogChangephotoBinding((RelativeLayout) rootView, dialogChoosePhoto,
          dialogOpenCamera);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
