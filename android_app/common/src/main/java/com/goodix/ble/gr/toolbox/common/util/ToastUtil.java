package com.goodix.ble.gr.toolbox.common.util;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.CheckResult;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.goodix.ble.gr.toolbox.common.R;
import com.muddzdev.styleabletoast.StyleableToast;

public class ToastUtil {
    @CheckResult
    public static StyleableToast.Builder normal(@NonNull Context context, @StringRes int message) {
        return normal(context, context.getString(message), Toast.LENGTH_SHORT);
    }

    @CheckResult
    public static StyleableToast.Builder normal(@NonNull Context context, @NonNull String message) {
        return normal(context, message, Toast.LENGTH_SHORT);
    }

//    @CheckResult
//    public static StyleableToast.Builder normal(@NonNull Context context, @StringRes int message, Drawable icon) {
//        return normal(context, context.getString(message), Style.DURATION_SHORT, icon, true);
//    }
//
//    @CheckResult
//    public static StyleableToast.Builder normal(@NonNull Context context, @NonNull String message, Drawable icon) {
//        return normal(context, message, Style.DURATION_SHORT, icon, true);
//    }

    @CheckResult
    public static StyleableToast.Builder normal(@NonNull Context context, @StringRes int message, int duration) {
        return normal(context, context.getString(message), duration);
    }

    @CheckResult
    public static StyleableToast.Builder normal(@NonNull Context context, @NonNull String message, int duration) {
        return make(context, message, duration, false, 0);
    }

//    @CheckResult
//    public static StyleableToast.Builder normal(@NonNull Context context, @StringRes int message, int duration,
//                                    Drawable icon) {
//        return normal(context, context.getString(message), duration, icon, true);
//    }
//
//    @CheckResult
//    public static StyleableToast.Builder normal(@NonNull Context context, @NonNull String message, int duration,
//                                    Drawable icon) {
//        return normal(context, message, duration, icon, true);
//    }
//
//    @CheckResult
//    public static StyleableToast.Builder normal(@NonNull Context context, @StringRes int message, @Style.Duration int duration,
//                                    @DrawableRes int iconResId, boolean withIcon) {
//
//        return custom(context, context.getString(message), icon, ToastyUtils.getColor(context, R.color.normalColor),
//                ToastyUtils.getColor(context, R.color.defaultTextColor), duration, withIcon, true);
//    }
//
//    @CheckResult
//    public static StyleableToast.Builder normal(@NonNull Context context, @NonNull String message, int duration,
//                                    @DrawableRes int iconResId, boolean withIcon) {
//        return custom(context, message, icon, ToastyUtils.getColor(context, R.color.normalColor),
//                ToastyUtils.getColor(context, R.color.defaultTextColor), duration, withIcon, true);
//    }

    @CheckResult
    public static StyleableToast.Builder warning(@NonNull Context context, @StringRes int message) {
        return warning(context, context.getString(message), Toast.LENGTH_SHORT, true);
    }

    @CheckResult
    public static StyleableToast.Builder warning(@NonNull Context context, @NonNull String message) {
        return warning(context, message, Toast.LENGTH_SHORT, true);
    }

    @CheckResult
    public static StyleableToast.Builder warning(@NonNull Context context, @StringRes int message, int duration) {
        return warning(context, context.getString(message), duration, true);
    }

    @CheckResult
    public static StyleableToast.Builder warning(@NonNull Context context, @NonNull String message, int duration) {
        return warning(context, message, duration, true);
    }

    @CheckResult
    public static StyleableToast.Builder warning(@NonNull Context context, @StringRes int message, int duration, boolean withIcon) {
        return warning(context, context.getString(message), duration, withIcon);
    }

    @CheckResult
    public static StyleableToast.Builder warning(@NonNull Context context, @NonNull String message, int duration, boolean withIcon) {
        return make(context, message, duration, withIcon, R.drawable.toast_warning)
                .backgroundColor(ContextCompat.getColor(context, R.color.toastWarning));
    }

    @CheckResult
    public static StyleableToast.Builder info(@NonNull Context context, @StringRes int message) {
        return info(context, context.getString(message), Toast.LENGTH_SHORT, true);
    }

    @CheckResult
    public static StyleableToast.Builder info(@NonNull Context context, @NonNull String message) {
        return info(context, message, Toast.LENGTH_SHORT, true);
    }

    @CheckResult
    public static StyleableToast.Builder info(@NonNull Context context, @StringRes int message, int duration) {
        return info(context, context.getString(message), duration, true);
    }

    @CheckResult
    public static StyleableToast.Builder info(@NonNull Context context, @NonNull String message, int duration) {
        return info(context, message, duration, true);
    }

    @CheckResult
    public static StyleableToast.Builder info(@NonNull Context context, @StringRes int message, int duration, boolean withIcon) {
        return info(context, context.getString(message), duration, withIcon);
    }

    @CheckResult
    public static StyleableToast.Builder info(@NonNull Context context, @NonNull String message, int duration, boolean withIcon) {
        return make(context, message, duration, withIcon, R.drawable.toast_info)
                .backgroundColor(ContextCompat.getColor(context, R.color.toastInfo));
    }

    @CheckResult
    public static StyleableToast.Builder success(@NonNull Context context, @StringRes int message) {
        return success(context, context.getString(message), Toast.LENGTH_SHORT, true);
    }

    @CheckResult
    public static StyleableToast.Builder success(@NonNull Context context, @NonNull String message) {
        return success(context, message, Toast.LENGTH_SHORT, true);
    }

    @CheckResult
    public static StyleableToast.Builder success(@NonNull Context context, @StringRes int message, int duration) {
        return success(context, context.getString(message), duration, true);
    }

    @CheckResult
    public static StyleableToast.Builder success(@NonNull Context context, @NonNull String message, int duration) {
        return success(context, message, duration, true);
    }

    @CheckResult
    public static StyleableToast.Builder success(@NonNull Context context, @StringRes int message, int duration, boolean withIcon) {
        return success(context, context.getString(message), duration, withIcon);
    }

    @CheckResult
    public static StyleableToast.Builder success(@NonNull Context context, @NonNull String message, int duration, boolean withIcon) {
        return make(context, message, duration, withIcon, R.drawable.toast_success)
                .backgroundColor(ContextCompat.getColor(context, R.color.toastSuccess));
    }

    @CheckResult
    public static StyleableToast.Builder error(@NonNull Context context, @StringRes int message) {
        return error(context, context.getString(message), Toast.LENGTH_SHORT, true);
    }

    @CheckResult
    public static StyleableToast.Builder error(@NonNull Context context, @NonNull String message) {
        return error(context, message, Toast.LENGTH_SHORT, true);
    }

    @CheckResult
    public static StyleableToast.Builder error(@NonNull Context context, @StringRes int message, int duration) {
        return error(context, context.getString(message), duration, true);
    }

    //    @CheckResult
//    public static StyleableToast.Builder error(@NonNull Context context, @NonNull String message, int duration, boolean withIcon) {
//        Toast toast = make(context, message, duration, false, 0);
//        toast.setColor(ContextCompat.getColor(context, R.color.toastError));
//        // if (withIcon) {
//        //     toast.setIconResource();
//        // }
//        return toast;
//    }

    @CheckResult
    public static StyleableToast.Builder error(@NonNull Context context, @NonNull String message, int duration, boolean withIcon) {
        return make(context, message, duration, withIcon, R.drawable.toast_error)
                .backgroundColor(ContextCompat.getColor(context, R.color.toastError));
    }

//    @CheckResult
//    public static StyleableToast.Builder error(@NonNull Context context, @StringRes int message, int duration, boolean withIcon) {
//        return custom(context, context.getString(message), ToastyUtils.getDrawable(context, R.drawable.ic_clear_white_24dp),
//                ToastyUtils.getColor(context, R.color.errorColor), ToastyUtils.getColor(context, R.color.defaultTextColor),
//                duration, withIcon, true);
//    }
//
//    @CheckResult
//    public static StyleableToast.Builder error(@NonNull Context context, @NonNull String message, int duration, boolean withIcon) {
//        return custom(context, message, ToastyUtils.getDrawable(context, R.drawable.ic_clear_white_24dp),
//                ToastyUtils.getColor(context, R.color.errorColor), ToastyUtils.getColor(context, R.color.defaultTextColor),
//                duration, withIcon, true);
//    }

//    @CheckResult
//    public static StyleableToast.Builder make(@NonNull Context context, @NonNull String message, int duration, boolean withIcon, @DrawableRes int iconResId) {
//
//        Toast toast = new Toast(context);
//        toast.setDuration(duration);
//        toast.setText(message);
//        if (withIcon) {
//            toast.setIconResource(iconResId);
//        }
//        toast.setTextColor(ContextCompat.getColor(context, R.color.toastDefaultText));
//        toast.setFrame(Style.FRAME_LOLLIPOP);
//
//        return toast;
//    }

    @SuppressWarnings("WeakerAccess")
    @CheckResult
    public static StyleableToast.Builder make(@NonNull Context context, @NonNull String message, int duration, boolean withIcon, @DrawableRes int iconResId) {

        StyleableToast.Builder builder = new StyleableToast.Builder(context.getApplicationContext());
        builder.text(message)
                .textColor(ContextCompat.getColor(context, R.color.toastDefaultText))
                .cornerRadius(600) // 为了显示半圆的边
                .length(duration);
        if (withIcon) {
            builder.iconStart(iconResId);
        }
        return builder;
    }

    @CheckResult
    public static AlertDialog.Builder dialog(@NonNull Context context, @StringRes int message) {
        return dialog(context, context.getString(message));
    }

    @CheckResult
    public static AlertDialog.Builder dialog(@NonNull Context context, @NonNull String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message)
                .setCancelable(true)
                .setPositiveButton(R.string.common_sure, null);
        return builder;
    }
}
