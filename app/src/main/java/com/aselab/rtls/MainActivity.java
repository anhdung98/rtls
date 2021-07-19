package com.aselab.rtls;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aselab.rtls.model.Anchor;
import com.aselab.rtls.model.FirebaseAnchor;
import com.aselab.rtls.model.FirebaseTag;
import com.aselab.rtls.model.FirebaseTarget;
import com.aselab.rtls.model.Point;
import com.aselab.rtls.model.Tag;
import com.aselab.rtls.model.Target;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, PointSelectorListener {

    enum Command {
        STOP,
        LINE,
        RECTANGLE,
        CONFIG,
        TARGET,
        TARGET90
    };

    DescartesView descartesView;
    DescartesLogic cl = new DescartesLogic();
    ArrayList<Anchor> anchors;
    Target target;
    Context mContext;
    SlidingUpPanelLayout sliding;
    BottomSheetDialog anchorDialog, tagTrailerDialog;

    float viewX, viewY;

    SharedPreferences prefs;

    boolean showTag;
    boolean showAnchor;
    boolean showTagLabel;
    boolean showAnchorLabel;
    boolean showGrid;
    boolean showAxis;
    int numberTrailer;

    boolean showPressedPoint = false;
    boolean moving = false;
    int startMoveTopMargin = 0;
    int startMoveLeftMargin = 0;
    int originTopMargin = 0;
    int originLeftMargin = 0;

    FirebaseDatabase database;

    private void loadConfigFromSharedPreferences() {
        if (prefs == null) prefs = this.getSharedPreferences(getString(R.string.RTLS_CONFIG_PREFS), Context.MODE_PRIVATE);
        showTag = prefs.getBoolean(getString(R.string.PREF_SHOW_TAG), true);
        showAnchor = prefs.getBoolean(getString(R.string.PREF_SHOW_ANCHOR), true);
        showTagLabel = prefs.getBoolean(getString(R.string.PREF_SHOW_TAG_LABEL), true);
        showAnchorLabel = prefs.getBoolean(getString(R.string.PREF_SHOW_ANCHOR_LABEL), true);
        showGrid = prefs.getBoolean(getString(R.string.PREF_SHOW_GRID), true);
        showAxis = prefs.getBoolean(getString(R.string.PREF_SHOW_AXIS), true);
        numberTrailer = prefs.getInt(getString(R.string.PREF_NUMBER_TRAILER), -1);

        double a1x = prefs.getFloat("pref_anchor_1_x", 0);
        double a1y = prefs.getFloat("pref_anchor_1_y", 0);
        double a2x = prefs.getFloat("pref_anchor_2_x", 150);
        double a2y = prefs.getFloat("pref_anchor_2_y", 0);
        double a3x = prefs.getFloat("pref_anchor_3_x", 150);
        double a3y = prefs.getFloat("pref_anchor_3_y", 250);
        double a4x = prefs.getFloat("pref_anchor_4_x", 0);
        double a4y = prefs.getFloat("pref_anchor_4_y", 250);

        double tx = prefs.getFloat(getString(R.string.PREF_TARGET_X), 75);
        double ty = prefs.getFloat(getString(R.string.PREF_TARGET_Y), 125);
        target = new Target(tx, ty);

        anchors = new ArrayList<>();
        anchors.add(new Anchor(a1x,a1y,"Anchor 1"));
        anchors.add(new Anchor(a2x,a2y,"Anchor 2"));
        anchors.add(new Anchor(a3x,a3y,"Anchor 3"));
        anchors.add(new Anchor(a4x,a4y,"Anchor 4"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;

        loadConfigFromSharedPreferences();
        if (prefs == null) prefs = this.getSharedPreferences(getString(R.string.RTLS_CONFIG_PREFS), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        sliding = findViewById(R.id.sliding_layout);
        sliding.setFadeOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sliding.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });

        database = FirebaseDatabase.getInstance();

        descartesView = findViewById(R.id.CanvasView);
        descartesView.setBackgroundColor(Color.WHITE);//Color.rgb(245,244,244)

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        for (int i = 0; i < anchors.size(); i++) {
            setAnchorValueView(i+1, anchors.get(i));
        }
        calcAnchorTitle();

        descartesView.setHeightAndWidth(width,height);
        descartesView.setAnchors(anchors);
        descartesView.setTarget(target);
        descartesView.setNumberTrailer(numberTrailer);
        descartesView.centerPlane();

        firebase();

        showTag = prefs.getBoolean(getString(R.string.PREF_SHOW_TAG), true);
        descartesView.setShowTag(showTag);
        SwitchMaterial showTagSwitch = findViewById(R.id.tag_show_on_map);
        showTagSwitch.setChecked(showTag);
        showTagSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                showTag = b;
                editor.putBoolean(getString(R.string.PREF_SHOW_TAG), b).apply();
                descartesView.setShowTag(showTag);
                SwitchMaterial showTagLabelSwitch = findViewById(R.id.tag_label_show_on_map);
                if (showTagLabelSwitch != null) {
                    showTagLabelSwitch.setEnabled(b);
                    if (!b) showTagLabelSwitch.setChecked(false);
                }
                descartesView.invalidate();
            }
        });

        descartesView.setShowTagLabel(showTagLabel);
        SwitchMaterial showTagLabelSwitch = findViewById(R.id.tag_label_show_on_map);
        showTagLabelSwitch.setChecked(showTagLabel);
        showTagLabelSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                showTagLabel = b;
                editor.putBoolean(getString(R.string.PREF_SHOW_TAG_LABEL), b).apply();
                descartesView.setShowTagLabel(showTagLabel);
                descartesView.invalidate();
            }
        });

        descartesView.setShowAnchor(showAnchor);
        SwitchMaterial showAnchorSwitch = findViewById(R.id.anchor_show_on_map);
        showAnchorSwitch.setChecked(showAnchor);
        showAnchorSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                showAnchor = b;
                editor.putBoolean(getString(R.string.PREF_SHOW_ANCHOR), b).apply();
                descartesView.setShowAnchor(showAnchor);
                SwitchMaterial showAnchorLabelSwitch = findViewById(R.id.anchor_label_show_on_map);
                if (showAnchorLabelSwitch != null) {
                    showAnchorLabelSwitch.setEnabled(b);
                    if (!b) showAnchorLabelSwitch.setChecked(false);
                }
                descartesView.invalidate();
            }
        });

        descartesView.setShowAnchorLabel(showAnchorLabel);
        SwitchMaterial showAnchorLabelSwitch = findViewById(R.id.anchor_label_show_on_map);
        showAnchorLabelSwitch.setChecked(showAnchorLabel);
        showAnchorLabelSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                showAnchorLabel = b;
                editor.putBoolean(getString(R.string.PREF_SHOW_ANCHOR_LABEL), b).apply();
                descartesView.setShowAnchorLabel(showAnchorLabel);
                descartesView.invalidate();
            }
        });

        descartesView.setShowAxis(showAxis);
        SwitchMaterial showAxisSwitch = findViewById(R.id.show_axis);
        showAxisSwitch.setChecked(showAxis);
        showAxisSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                showAxis = b;
                editor.putBoolean(getString(R.string.PREF_SHOW_AXIS), b).apply();
                descartesView.setShowAxis(showAxis);
                descartesView.invalidate();
            }
        });

        descartesView.setShowGrid(showGrid);
        SwitchMaterial showGridSwitch = findViewById(R.id.show_grid);
        showGridSwitch.setChecked(showGrid);
        showGridSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                showGrid = b;
                editor.putBoolean(getString(R.string.PREF_SHOW_GRID), b).apply();
                descartesView.setShowGrid(showGrid);
                descartesView.invalidate();
            }
        });

        switch (numberTrailer) {
            case -1:
                setTagTrailerDescription(getString(R.string.NO));
                break;
            case -2:
                setTagTrailerDescription(getString(R.string.SHOW_ALL_TRAIL_DESCRIPTION));
                break;
            default:
                setTagTrailerDescription(String.format(getString(R.string.NUMBER_TRAILER_DESCRIPTION), numberTrailer));
        }

        findViewById(R.id.tag_trailer).setOnClickListener(view -> {
            View tagTrailerCoordinateView = getLayoutInflater().inflate(R.layout.tag_trailer, null);

            SwitchMaterial showTrailer = tagTrailerCoordinateView.findViewById(R.id.show_trailer);
            SwitchMaterial showAllTrailers = tagTrailerCoordinateView.findViewById(R.id.show_all_trailers);
            EditText numberTrailerInput = tagTrailerCoordinateView.findViewById(R.id.number_trailer);

            switch (numberTrailer) {
                case -1: // NO TRAIL
                    showTrailer.setChecked(false);
                    showAllTrailers.setChecked(false);
                    showAllTrailers.setEnabled(false);
                    numberTrailerInput.setText(R.string.ZERO);
                    numberTrailerInput.setEnabled(false);
                    break;
                case -2: // ALL POINT
                    showTrailer.setChecked(true);
                    showAllTrailers.setChecked(true);
                    showAllTrailers.setEnabled(true);
                    numberTrailerInput.setText(R.string.INF);
                    numberTrailerInput.setEnabled(false);
                    break;
                default:
                    showTrailer.setChecked(true);
                    showAllTrailers.setChecked(false);
                    showAllTrailers.setEnabled(true);
                    numberTrailerInput.setText(String.valueOf(numberTrailer));
                    numberTrailerInput.setEnabled(true);
            }

            tagTrailerDialog = new BottomSheetDialog(this, R.style.DialogStyle);
            tagTrailerDialog.setContentView(tagTrailerCoordinateView);
            tagTrailerDialog.show();

            showTrailer.setOnCheckedChangeListener((compoundButton, b) -> {
                showAllTrailers.setEnabled(b);
                numberTrailerInput.setText(b?getString(R.string.ONE):getString(R.string.ZERO));
                if (!b) {
                    numberTrailerInput.setEnabled(false);
                    showAllTrailers.setChecked(false);
                } else {
                    numberTrailerInput.setEnabled(true);
                    numberTrailerInput.setSelection(numberTrailerInput.getText().length());
                }
            });
            showAllTrailers.setOnCheckedChangeListener((compoundButton, b) -> {
                if (b) {
                    numberTrailerInput.setEnabled(false);
                    numberTrailerInput.setText(R.string.INF);
                } else {
                    if (showTrailer.isChecked()) {
                        numberTrailerInput.setEnabled(true);
                        numberTrailerInput.setText(R.string.ONE);
                        numberTrailerInput.setSelection(numberTrailerInput.getText().length());
                    }
                }
            });
            numberTrailerInput.setOnKeyListener((view12, i, keyEvent) -> {
                if (showAllTrailers.isChecked() && numberTrailerInput.getText().length() > 0) {
                    showAllTrailers.setChecked(false);
                    numberTrailerInput.setSelection(numberTrailerInput.getText().length());
                }
                return false;
            });
            numberTrailerInput.setOnClickListener(view1 -> numberTrailerInput.setSelection(numberTrailerInput.getText().length()));

            Button saveTrailerSetting = tagTrailerCoordinateView.findViewById(R.id.save_trailer_setting);
            saveTrailerSetting.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!numberTrailerInput.isEnabled()
                            || (numberTrailerInput.getText() != null && numberTrailerInput.length() > 0
                                && (showAllTrailers.isChecked()
                                    || (!showAllTrailers.isChecked()
                                            && Integer.parseInt(numberTrailerInput.getText().toString()) >= 0)))) {
                        if (!showTrailer.isChecked()) {
                            setTagTrailerDescription(getString(R.string.NO));
                            descartesView.setNumberTrailer(-1);
                            editor.putInt(getString(R.string.PREF_NUMBER_TRAILER), -1).apply();
                            numberTrailer = -1;
                        } else {
                            if (showAllTrailers.isChecked()) {
                                setTagTrailerDescription(getString(R.string.SHOW_ALL_TRAIL_DESCRIPTION));
                                descartesView.setNumberTrailer(-2);
                                editor.putInt(getString(R.string.PREF_NUMBER_TRAILER), -2).apply();
                                numberTrailer = -2;
                            } else {
                                int num = Integer.parseInt(numberTrailerInput.getText().toString());
                                setTagTrailerDescription(String.format(getString(R.string.NUMBER_TRAILER_DESCRIPTION), num));
                                descartesView.setNumberTrailer(num);
                                editor.putInt(getString(R.string.PREF_NUMBER_TRAILER), num).apply();
                                numberTrailer = num;
                            }
                        }
                        tagTrailerDialog.dismiss();
                    } else {
                        Toast.makeText(mContext, "Số điểm lưu vết không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });

        descartesView.setListener(this);
        findViewById(R.id.delete_all_tags).setOnClickListener(this);
        findViewById(R.id.centerbutton).setOnClickListener(this);
        findViewById(R.id.btn_command_stop).setOnClickListener(this);
        GridLayout gridCommand = findViewById(R.id.grid_command);
        for (int i = 0; i < gridCommand.getChildCount(); i++) {
            gridCommand.getChildAt(i).setOnClickListener(this);
        }
        LinearLayout anchorSetting = findViewById(R.id.anchors_setting);
        for (int i = 0; i < anchorSetting.getChildCount(); i++) {
            anchorSetting.getChildAt(i).setOnClickListener(this);
        }

        descartesView.invalidate();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.centerbutton:
                descartesView.centerPlane();
                showPressedPoint = false;
                findViewById(R.id.target_icon).setVisibility(View.GONE);
                hideCardPoint();
                break;
            case R.id.delete_all_tags:
                new MaterialAlertDialogBuilder(this)
                        .setMessage("Bạn muốn xoá tất cả vị trí của tag?")
                        .setNeutralButton("Huỷ", (dialogInterface, i) -> {
                            dialogInterface.dismiss();
                        })
                        .setPositiveButton("Đồng ý", (dialogInterface, i) -> {
                            descartesView.deleteAllTags();
                            dialogInterface.dismiss();
                        })
                        .show();
                break;
            case R.id.btn_command_stop:
                setCommandView(Command.STOP);
                sendCommandFirebase(Command.STOP);
                break;
            case R.id.btn_command_line:
                setCommandView(Command.LINE);
                sendCommandFirebase(Command.LINE);
                break;
            case R.id.btn_command_rectangle:
                setCommandView(Command.RECTANGLE);
                sendCommandFirebase(Command.RECTANGLE);
                break;
            case R.id.btn_command_config:
                setCommandView(Command.CONFIG);
                sendCommandFirebase(Command.CONFIG);
                break;
            case R.id.btn_command_target:
                setCommandView(Command.TARGET);
                sendCommandFirebase(Command.TARGET);
                break;
            case R.id.btn_command_target90:
                setCommandView(Command.TARGET90);
                sendCommandFirebase(Command.TARGET90);
                break;
            case R.id.anchor_1_coordinate:
                setAnchorCoordinateDialog(1);
                break;
            case R.id.anchor_2_coordinate:
                setAnchorCoordinateDialog(2);
                break;
            case R.id.anchor_3_coordinate:
                setAnchorCoordinateDialog(3);
                break;
            case R.id.anchor_4_coordinate:
                setAnchorCoordinateDialog(4);
                break;
            default:
                break;
        }
    }

    public int pxToDp(Context context, int px) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int dp = Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return dp;
    }

    @Override
    public void onBackPressed() {
        if ((sliding.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED
                || sliding.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED)) {
            if (anchorDialog != null && anchorDialog.isShowing()) {
                anchorDialog.cancel();
            } else {
                sliding.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        } else {
            super.onBackPressed();
        }
    }

    @SuppressLint("UseCompatLoadingForColorStateLists")
    public void setCommandView(Command command) {
        if (findViewById(R.id.current_command_text) == null) return;
        TextView currentCommandText = findViewById(R.id.current_command_text);
        currentCommandText.setText(command.name());
        if (findViewById(R.id.grid_command) == null) return;
        GridLayout gridCommmand = findViewById(R.id.grid_command);
        for (int i = 0; i < gridCommmand.getChildCount(); i++) {
            Button btn = (Button)gridCommmand.getChildAt(i);
            Drawable drawable = btn.getBackground();
            if (command.name().equals(btn.getText().toString().toUpperCase())) {
                drawable.setTint(getResources().getColor(R.color.purple_500));
                btn.setBackground(drawable);
                btn.setTextColor(getResources().getColor(R.color.white));
            } else {
                drawable.setTint(getResources().getColor(R.color.white));
                btn.setBackground(drawable);
                btn.setTextColor(getResources().getColor(R.color.purple_500));
            }
        }
        Button btnStop = findViewById(R.id.btn_command_stop);
        Drawable btnStopDrawable = btnStop.getBackground();
        if (command.name().equals("STOP")) {
            btnStopDrawable.setTint(getResources().getColor(R.color.btn_stop));
            btnStop.setBackground(btnStopDrawable);
            btnStop.setTextColor(getResources().getColor(R.color.white));
        } else {
            btnStopDrawable.setTint(getResources().getColor(R.color.white));
            btnStop.setBackground(btnStopDrawable);
            btnStop.setTextColor(getResources().getColor(R.color.btn_stop));
        }
    }

    @SuppressLint("SetTextI18n")
    public void setAnchorCoordinateDialog(int selectedAnchor) {
        View anchorCoordinateView = getLayoutInflater().inflate(R.layout.anchor_coordinates, null);
        anchorDialog = new BottomSheetDialog(this, R.style.DialogStyle);
        anchorDialog.setContentView(anchorCoordinateView);
        anchorDialog.show();
        TextView anchorTitleView = anchorCoordinateView.findViewById(R.id.anchor_coordinate_title);
        if (anchorTitleView != null) {
            anchorTitleView.setText("Vị trí Anchor " + selectedAnchor);
        }
        anchorCoordinateView.findViewById(R.id.save_anchor_coordinate).setOnClickListener(view -> {
            TextInputEditText inputX = anchorCoordinateView.findViewById(R.id.anchor_coordinate_x);
            TextInputEditText inputY = anchorCoordinateView.findViewById(R.id.anchor_coordinate_y);
            Editable editableX = inputX.getText();
            Editable editableY = inputY.getText();
            if (editableX != null && editableY != null && editableX.length() > 0 && editableY.length() > 0) {
                double x = Double.parseDouble(editableX.toString());
                double y = Double.parseDouble(editableY.toString());
                Anchor anchor = new Anchor(x,y, "Anchor " + selectedAnchor);
                setAnchorFirebase(selectedAnchor, anchor);
                anchors.set(selectedAnchor - 1, anchor);
                setAnchorsPref();
                calcAnchorTitle();
                setAnchorValueView(selectedAnchor, anchor);
                onSelectPoint(anchor);
                descartesView.invalidate();
                anchorDialog.cancel();
            } else {
                Toast.makeText(mContext, "Toạ độ không hợp lệ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    public void setTargetCoordinateDialog() {
        View targetCoordinateView = getLayoutInflater().inflate(R.layout.anchor_coordinates, null);
        anchorDialog = new BottomSheetDialog(this, R.style.DialogStyle);
        anchorDialog.setContentView(targetCoordinateView);
        anchorDialog.show();
        TextView targetTitleView = targetCoordinateView.findViewById(R.id.anchor_coordinate_title);
        if (targetTitleView != null) {
            targetTitleView.setText("Vị trí mục tiêu");
        }
        targetCoordinateView.findViewById(R.id.save_anchor_coordinate).setOnClickListener(view -> {
            TextInputEditText inputX = targetCoordinateView.findViewById(R.id.anchor_coordinate_x);
            TextInputEditText inputY = targetCoordinateView.findViewById(R.id.anchor_coordinate_y);
            Editable editableX = inputX.getText();
            Editable editableY = inputY.getText();
            if (editableX != null && editableY != null && editableX.length() > 0 && editableY.length() > 0) {
                double x = Double.parseDouble(editableX.toString());
                double y = Double.parseDouble(editableY.toString());
                target = new Target(x,y);
                setTargetPref();
                onSelectPoint(target);
                setTargetFirebase(target);
                descartesView.setTarget(target);
                descartesView.invalidate();
                anchorDialog.cancel();
            } else {
                Toast.makeText(mContext, "Toạ độ không hợp lệ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    public void setAnchorValueView(int selectedAnchor, Anchor anchor) {
        TextView textView;
        switch (selectedAnchor) {
            case 1:
                textView = findViewById(R.id.anchor_1_coordinate_value);
                break;
            case 2:
                textView = findViewById(R.id.anchor_2_coordinate_value);
                break;
            case 3:
                textView = findViewById(R.id.anchor_3_coordinate_value);
                break;
            case 4:
                textView = findViewById(R.id.anchor_4_coordinate_value);
                break;
            default:
                return;
        }
        if (textView != null) {
            textView.setText("(" + anchor.getX() + "; " + anchor.getY() + ") cm");
        }
    }

    public void calcAnchorTitle() {
        for (int i = 0; i < anchors.size(); i++) {
            int tmpTop = 0, tmpLeft = 0;
            for (int j = 0; j < anchors.size(); j++) {
                if (anchors.get(j).getX() >= anchors.get(i).getX())
                    tmpLeft++;
                if (anchors.get(j).getY() >= anchors.get(i).getY())
                    tmpTop++;
            }
            if (tmpLeft > anchors.size() / 2)
                anchors.get(i).setHorizontalAlignment(-1);
            else
                anchors.get(i).setHorizontalAlignment(1);
            if (tmpTop <= anchors.size() / 2)
                anchors.get(i).setVerticalAlignment(-1);
            else
                anchors.get(i).setVerticalAlignment(1);
        }
    }

    public void setTagTrailerDescription(String text) {
        TextView tagTrailerDescription = findViewById(R.id.tag_trailer_description);
        if (tagTrailerDescription != null) {
            tagTrailerDescription.setText(text);
        }
    }

    Point lastTouchPoint;

    @SuppressLint("DefaultLocale")
    @Override
    public void onSelectPoint(Point point) {
        Log.d("SELECT", point.getType().name());
        lastTouchPoint = point;
        if (!showPressedPoint) {
            if (point.getType() == Point.PointType.ANCHOR
                    || point.getType() == Point.PointType.TARGET
                    || point.getType() == Point.PointType.TAG) {
                setCardPoint(point);
                showCardPoint();
            } else {
                hideCardPoint();
            }
        }
    }

    @SuppressLint("DefaultLocale")
    public void setCardPoint(Point point) {
        TextView cardTitle = findViewById(R.id.card_title);
        TextView pointCoordinate = findViewById(R.id.point_coordinate);
        MaterialButton btnAdd = findViewById(R.id.btn_add_point);
        MaterialButton btnEdit = findViewById(R.id.btn_edit_point);
        MaterialButton btnDelete = findViewById(R.id.btn_delete_point);
        cardTitle.setText(point.getTitle());
        pointCoordinate.setText(String.format("x = %.2f cm\ny = %.2f cm", point.getX(), point.getY()));
        if (point.getType() == Point.PointType.ANCHOR) {
            btnAdd.setVisibility(View.GONE);
            btnEdit.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.GONE);
            btnEdit.setOnClickListener(view -> {
                int anchorId = Character.getNumericValue(point.getTitle().charAt(point.getTitle().length() - 1));
                setAnchorCoordinateDialog(anchorId);
            });
        } else if (point.getType() == Point.PointType.TARGET) {
            btnAdd.setVisibility(View.GONE);
            btnEdit.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);
            btnEdit.setOnClickListener(view -> {
                setTargetCoordinateDialog();
            });
            btnDelete.setOnClickListener(view -> {
                descartesView.setTarget(null);
                descartesView.invalidate();
                hideCardPoint();
            });
        } else if (point.getType() == Point.PointType.TAG) {
            btnAdd.setVisibility(View.GONE);
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
        } else {
            btnAdd.setVisibility(View.VISIBLE);
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
            btnAdd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ImageView targetIcon = findViewById(R.id.target_icon);
                    targetIcon.setVisibility(View.GONE);
                    target = Target.fromPoint(point);
                    setTargetPref();
                    setCardPoint(target);
                    setTargetFirebase(target);
                    descartesView.setTarget(target);
                    descartesView.invalidate();
                }
            });
        }
    }

    public void hideCardPoint() {
        if (findViewById(R.id.card_point) != null)
            findViewById(R.id.card_point).setVisibility(View.GONE);
    }

    public void showCardPoint() {
        if (findViewById(R.id.card_point) != null)
            findViewById(R.id.card_point).setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        ImageView targetIcon = findViewById(R.id.target_icon);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)targetIcon.getLayoutParams();
        if (showPressedPoint && (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_MOVE) {
            if (!moving) {
                originTopMargin = lp.topMargin;
                originLeftMargin = lp.leftMargin;
                startMoveLeftMargin = (int)event.getX();
                startMoveTopMargin = (int)event.getY();
                moving = true;
//                Log.e("DEBUG", "set moving " + moving);
            }
            viewX = originLeftMargin + (int)event.getX() - startMoveLeftMargin;
            viewY = originTopMargin + (int)event.getY() - startMoveTopMargin;
            lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins((int) viewX, (int) viewY, 0, 0);
            targetIcon.setLayoutParams(lp);
//            Log.e("TOUCH", /*"originTopMargin: " + originTopMargin + ", originLeftMargin: " + originLeftMargin + */
//                        ", startMoveTopMargin: " + startMoveTopMargin + ", startMoveLeftMargin: " + startMoveLeftMargin +
//                             ", newMoveTopMargin: " + (int)event.getY() + ", newMoveLeftMargin: " + (int)event.getX() +
//                        ", viewY: " + viewY + ", viewX: " + viewX);
        }
        if (moving && (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            moving = false;
        }
        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_DOWN) {
            showPressedPoint = false;
            targetIcon.setVisibility(View.GONE);
        }
        return gestureDetector.onTouchEvent(event);
    }

    final GestureDetector gestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
        public void onLongPress(MotionEvent event) {
            if (lastTouchPoint.getType() != Point.PointType.ANCHOR) {
                showPressedPoint = true;
                float _1dp = getResources().getDisplayMetrics().density;
                viewX = event.getX();
                viewY = event.getY();
                Log.d("TOUCH", "You click long pressed at " + viewX + " " + viewY);
                ImageView targetIcon = findViewById(R.id.target_icon);
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins((int) viewX - (int) (8 * _1dp), (int) viewY - (int) (22 * _1dp), 0, 0);
                originTopMargin = (int) viewY - (int) (22 * _1dp);
                originLeftMargin = (int) viewX - (int) (8 * _1dp);
                targetIcon.setLayoutParams(lp);
                targetIcon.setVisibility(View.VISIBLE);
                setCardPoint(lastTouchPoint);
                showCardPoint();
            }
        }
        public boolean onSingleTapUp(MotionEvent e) {
            if (showPressedPoint) {
                showPressedPoint = false;
                ImageView targetIcon = findViewById(R.id.target_icon);
                targetIcon.setVisibility(View.GONE);
                hideCardPoint();
            }
            return true;
        }
    });

    public void firebase() {
        DatabaseReference command = database.getReference("Command");
        DatabaseReference tag = database.getReference("Location/Tag");
        DatabaseReference targetDb = database.getReference("MoveTarget");
        DatabaseReference anchor1Db = database.getReference("Location/Anchors/Anchor1");
        DatabaseReference anchor2Db = database.getReference("Location/Anchors/Anchor2");
        DatabaseReference anchor3Db = database.getReference("Location/Anchors/Anchor3");
        DatabaseReference anchor4Db = database.getReference("Location/Anchors/Anchor4");

        command.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @org.jetbrains.annotations.NotNull DataSnapshot snapshot) {
                String strCommand = snapshot.getValue(String.class);
                if (strCommand == null) return;
                Log.d("FIREBASE", "Receive command: " + strCommand);
                for (Command cmd: Command.values()) {
                    if (strCommand.equals(cmd.name())) {
                        setCommandView(cmd);
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull @org.jetbrains.annotations.NotNull DatabaseError error) {
                Log.e("FIREBASE", "Failed to read value.", error.toException());
            }
        });
        tag.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                FirebaseTag fbTag = snapshot.getValue(FirebaseTag.class);
                if (fbTag == null) return;
                Log.d("FIREBASE", "Receive tag: " + fbTag.getX() + " " + fbTag.getY());
                Tag tag = new Tag(Double.parseDouble(fbTag.getX()), Double.parseDouble(fbTag.getY()));
                descartesView.setTag(tag);
                descartesView.invalidate();
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {
                Log.e("FIREBASE", "Failed to read value.", error.toException());
            }
        });

        targetDb.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                FirebaseTarget fbTarget = snapshot.getValue(FirebaseTarget.class);
                if (fbTarget == null) return;
                Log.d("FIREBASE", "Receive target: " + fbTarget.getX() + " " + fbTarget.getY());
                target = new Target(Double.parseDouble(fbTarget.getX()), Double.parseDouble(fbTarget.getY()));
                setTargetPref();
                descartesView.setTarget(target);
                descartesView.invalidate();
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });

        anchor1Db.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                FirebaseAnchor fbAnchor = snapshot.getValue(FirebaseAnchor.class);
                if (fbAnchor == null) return;
                Log.d("FIREBASE", "Receive anchor 1: " + fbAnchor.getX() + " " + fbAnchor.getY());
                anchors.set(0, new Anchor(Double.parseDouble(fbAnchor.getX()), Double.parseDouble(fbAnchor.getY()), "Anchor 1"));
                calcAnchorTitle();
                setAnchorsPref();
                descartesView.setAnchors(anchors);
                descartesView.invalidate();
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });
    }

    public void setTargetFirebase(Target target) {
        if (target != null) {
            DatabaseReference targetDb = database.getReference("MoveTarget");
            FirebaseTarget fbTarget = new FirebaseTarget();
            fbTarget.setX(String.valueOf(target.getX()));
            fbTarget.setY(String.valueOf(target.getY()));
            targetDb.setValue(fbTarget);
        }
    }

    public void setAnchorFirebase(int id, Anchor anchor) {
        if (anchor != null) {
            DatabaseReference anchorDb = database.getReference("Location/Anchors/Anchor" + id);
            FirebaseAnchor fbAnchor = new FirebaseAnchor();
            fbAnchor.setX(String.valueOf(anchor.getX()));
            fbAnchor.setY(String.valueOf(anchor.getY()));
            anchorDb.setValue(fbAnchor);
        }
    }

    public void sendCommandFirebase(Command command) {
        DatabaseReference cmd = database.getReference("Command");
        cmd.setValue(command.name());
    }

    public void setTargetPref() {
        if (prefs == null) prefs = mContext.getSharedPreferences(getString(R.string.RTLS_CONFIG_PREFS), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(getString(R.string.PREF_TARGET_X), (float)target.getX());
        editor.putFloat(getString(R.string.PREF_TARGET_Y), (float)target.getY());
        editor.apply();
    }

    public void setAnchorsPref() {
        if (prefs == null) prefs = mContext.getSharedPreferences(getString(R.string.RTLS_CONFIG_PREFS), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        for (int i = 0; i < anchors.size(); i++) {
            editor.putFloat(String.format(getString(R.string.PREF_ANCHOR_ID_X), i+1), (float)anchors.get(i).getX());
            editor.putFloat(String.format(getString(R.string.PREF_ANCHOR_ID_Y), i+1), (float)anchors.get(i).getY());
        }
        editor.apply();
    }

}