/**
 * Dialog fragment displayed when adding a new item. Handles user input for item details such as name,
 * description, serial number, model, make, purchase date, price, and comments. Validates user input
 * for various fields, including character limits and format requirements. Allows users to add tags to
 * the item. Utilizes a listener interface to communicate with the hosting activity for item addition.
 * Ensures proper handling of user interactions, validation errors, and invokes the listener when a new
 * item is successfully added. Collaborates with the hosting activity to update the total cost of items.
 */


package com.example.cmput301project.fragments;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Double.parseDouble;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.cmput301project.activities.MainActivity;
import com.example.cmput301project.itemClasses.Item;
import com.example.cmput301project.R;
import com.example.cmput301project.itemClasses.Tag;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


public class AddItemFragment extends DialogFragment {

    private TextView title;
    private EditText itemName;
    private EditText itemDescription;
    private EditText itemSerial;
    private EditText itemModel;
    private EditText itemDay;
    private EditText itemMonth;
    private EditText itemYear;
    private EditText itemComments;
    private EditText itemPrice;
    private EditText itemMake;
    private Item editItem;
    private Boolean invalidInput;
    private OnFragmentInteractionListener listener;
    private EditText inputTagEditText;
    private ChipGroup chipGroupTags;
    private Button addTagButton;
    private Button scannerButton;
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 101;
    private String[] cameraPermissions;
    private String[] storagePermissions;
    private Uri imageURI = null;
    public static final String TAG = "MAIN_TAG";
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {});

    private BarcodeScannerOptions barcodeScannerOptions;
    private BarcodeScanner barcodeScanner;


    /**
     * @param context
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            listener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + "OnFragmentInteractionListener is not implemented");
        }
    }


    /**
     *
     */
    public interface OnFragmentInteractionListener {
        void onOKPressed(Item item);

        void updateTotalCost();
    }

    // Method to check if the tag is already added
    private boolean isTagAlreadyAdded(String tagText) {
        for (int i = 0; i < chipGroupTags.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupTags.getChildAt(i);
            if (chip.getText().toString().equalsIgnoreCase(tagText)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param savedInstanceState The last saved instance state of the Fragment,
     *                           or null if this is a freshly created Fragment.
     * @return
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        View view = LayoutInflater.from(getActivity()).inflate(R.layout.add_item_layout, null);
        itemName = view.findViewById(R.id.name_edit_text); //find views on fragment to set text later
        itemDescription = view.findViewById(R.id.description_edit_text);
        itemSerial = view.findViewById(R.id.serial_edit_text);
        itemModel = view.findViewById(R.id.model_edit_text);
        itemMake = view.findViewById(R.id.make_edit_text);
        itemDay = view.findViewById(R.id.item_day_edit_text);
        itemMonth = view.findViewById(R.id.item_month_edit_text);
        itemYear = view.findViewById(R.id.item_year_edit_text);
        itemPrice = view.findViewById(R.id.price_edit_text);
        itemComments = view.findViewById(R.id.comments_edit_text);
        title = view.findViewById(R.id.add_item_title);
        inputTagEditText = view.findViewById(R.id.input_tag_edit_text); // Initialize inputTagEditText
        chipGroupTags = view.findViewById(R.id.chip_group_tags); // Initialize chipGroupTags
        addTagButton = view.findViewById(R.id.add_tags_button); // Initialize the addTagButton
        scannerButton = view.findViewById(R.id.scan_barcode_button);
        cameraPermissions = new String[]{Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        barcodeScannerOptions = new BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build();
        barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        Dialog dialog = builder.setView(view)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("OK", null).create(); //create a dialog with buttons and title

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) { //need to overwrite the default behavior of buttons, which is to dismiss the dialog
                Button okButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);

                Button addButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE); // This is your existing OK button logic

                scannerButton.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        if (ContextCompat.checkSelfPermission(getActivity(),Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                            pickImageCamera();
                            detectResultFromImage();

                        }
                        else{
                            requestCameraPermission();
                        }
                    }
                });

                // Set the click listener for the add tag button
                addTagButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String tagText = inputTagEditText.getText().toString().trim();
                        if (!tagText.isEmpty()) {
                            if(isTagAlreadyAdded(tagText)) {
                                Toast.makeText(getContext(), "This tag has already been added", Toast.LENGTH_SHORT).show();
                            } else {
                                Chip chip = new Chip(getContext());
                                chip.setText(tagText);
                                chip.setCloseIconVisible(true);
                                chip.setOnCloseIconClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        chipGroupTags.removeView(chip);
                                    }
                                });
                                chipGroupTags.addView(chip);
                                inputTagEditText.setText(""); // Clear the EditText after adding the chip
                            }
                        }
                    }
                });

                okButton.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        // Add mode

                        // Extract input fields
                        String name = itemName.getText().toString().trim();
                        String description = itemDescription.getText().toString().trim();
                        String serialText = itemSerial.getText().toString().trim();
                        String model = itemModel.getText().toString().trim();
                        String make = itemMake.getText().toString().trim();
                        String day = itemDay.getText().toString().trim();
                        String month = itemMonth.getText().toString().trim();
                        String year = itemYear.getText().toString().trim();
                        String priceText = itemPrice.getText().toString().trim();
                        String comments = itemComments.getText().toString().trim();

                        // Check if any field is empty
                        boolean anyFieldsEmpty = name.isEmpty() || description.isEmpty() ||
                                model.isEmpty() || make.isEmpty() || day.isEmpty() || month.isEmpty() ||
                                year.isEmpty() || priceText.isEmpty() || comments.isEmpty();

                        // Set serial number to 0 if non provided
                        Integer serial;
                        if (!serialText.isEmpty()) {
                            serial = Integer.parseInt(serialText);
                        } else {
                            serial = 0;
                        }

                        // Validate all fields
                        boolean isValidFields = isValidName(name) && isValidDescription(description) && isValidModel(model) && isValidMake(make) &&
                                isValidPrice(priceText) && isValidComment(comments) && isValidDay(day) && isValidMonth(month) && isValidYear(year);

                        // Add new item if fields valid
                        if (isValidFields && !anyFieldsEmpty) {
                            String date = month + "/" + day + "/" + year;
                            DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
                            Date parsedDate;
                            try {
                                parsedDate = df.parse(date);
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
                            }
                            Double price = Double.parseDouble(priceText);

                            // Create a new Item
                            Item newItem = new Item(name, parsedDate, description, make, model, serial, price, comments);

                            // Add tags from the chipGroupTags to the newItem
                            for (int i = 0; i < chipGroupTags.getChildCount(); i++) {
                                Chip chip = (Chip) chipGroupTags.getChildAt(i);
                                Tag tag = new Tag(chip.getText().toString());
                                newItem.addTag(tag); // Use the new method in the Item class
                            }

                            // Call the listener with the new item
                            listener.onOKPressed(newItem);
                            listener.updateTotalCost();
                            dialog.dismiss();

                            // Error messages
                        } else {
                            if (!isValidName(name)) {
                                itemName.setError("Max 15 characters");
                            }
                            if (!isValidDescription(description)) {
                                itemDescription.setError("Max 50 characters");
                            }
                            if (!isValidModel(model)) {
                                itemModel.setError("Max 20 characters");
                            }
                            if (!isValidMake(make)) {
                                itemMake.setError("Max 20 characters");
                            }
                            if (!isValidPrice(priceText)) {
                                itemPrice.setError("Invalid price format");
                            }
                            if (!isValidComment(comments)) {
                                itemComments.setError("Max 25 characters");
                            }
                            if (!isValidDay(day)) {
                                itemDay.setError(("Invalid day"));
                            }
                            if (!isValidMonth(month)) {
                                itemMonth.setError(("Invalid month"));
                            }
                            if (!isValidYear(year)) {
                                itemYear.setError(("Invalid year"));
                            }
                            if (name.isEmpty()) {
                                itemName.setError("Item name required");
                            }
                            if (description.isEmpty()) {
                                itemDescription.setError("Item description required");
                            }
                            if (model.isEmpty()) {
                                itemModel.setError("Item model required");
                            }
                            if (make.isEmpty()) {
                                itemMake.setError("Item make required");
                            }
                            if (priceText.isEmpty()) {
                                itemPrice.setError("Item price required");
                            }
                            if (comments.isEmpty()) {
                                itemComments.setError("Item comments required");
                            }
                            if (day.isEmpty()) {
                                itemDay.setError("Date required");
                            }
                            if (month.isEmpty()) {
                                itemMonth.setError("Month required");
                            }
                            if (year.isEmpty()) {
                                itemYear.setError("Year required");
                            }
                        }



                    }
                });
            }
        });
        return dialog;
    }


    /**
     * @param name
     * @return
     */
    private boolean isValidName(String name) {
        if (name.length() > 15) {
            return false;
        }
        return true;
    }


    /**
     * @param description
     * @return
     */
    private boolean isValidDescription(String description) {
        if (description.length() > 50) {
            return false;
        }
        return true;
    }


    /**
     * @param model
     * @return
     */
    private boolean isValidModel(String model) {
        if (model.length() > 20) {
            return false;
        }
        return true;
    }


    /**
     * @param make
     * @return
     */
    private boolean isValidMake(String make) {
        if (make.length() > 20) {
            return false;
        }
        return true;
    }


    /**
     * @param price
     * @return
     */
    private boolean isValidPrice(String price) {
        String priceText = String.valueOf(price);
        return priceText.matches("^(0\\.\\d{1,2}|[1-9]\\d*\\.?\\d{0,2})$");
    }


    /**
     * @param comment
     * @return
     */
    private boolean isValidComment(String comment) {
        if (comment.length() > 25) {
            return false;
        }
        return true;
    }


    /**
     * @param day
     * @return
     */
    private boolean isValidDay(String day) {
        if (!day.isEmpty()) {
            int dayValue = Integer.parseInt(day);
            return dayValue >= 1 && dayValue <= 31;
        }
        return false;
    }


    /**
     * @param month
     * @return
     */
    private boolean isValidMonth(String month) {
        if (!month.isEmpty()) {
            int monthValue = Integer.parseInt(month);
            return monthValue >= 1 && monthValue <= 12;
        }
        return false;
    }


    /**
     * @param year
     * @return
     */
    private boolean isValidYear(String year) {
        if (!year.isEmpty() && year.matches("\\d{4}")) {
            return true;
        }
        return false;
    }

    private void pickImageCamera(){
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "Sample Title");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Sample Description");

        imageURI = getActivity().getApplicationContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageURI);
        cameraActivityResultLauncher.launch(intent);

    }

    private final ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();
                        Log.d(TAG,"onActivityResult: imageURI: "+ imageURI);

                    }
                    else{
                        //Error
                    }
                }
            }
    );
    private void requestCameraPermission(){
        requestPermissionLauncher.launch(Manifest.permission.CAMERA);
    }
    private void detectResultFromImage(){
        try{
            InputImage inputImage = InputImage.fromFilePath(getActivity(),imageURI);
            Task<List<Barcode>> barcodeResult = barcodeScanner.process(inputImage).addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                @Override
                public void onSuccess(List<Barcode> barcodes) {
                    extractBarCodeQRCodeInfo(barcodes);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    //error
                }
            });
        }
        catch (Exception e){
            Toast.makeText(getActivity(),"Failed due to "+e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    private void extractBarCodeQRCodeInfo(List<Barcode> barcodes){
        for(Barcode barcode : barcodes){
            Rect bounds = barcode.getBoundingBox();
            Point[] corners = barcode.getCornerPoints();

            String rawValue = barcode.getRawValue();
            Log.d(TAG,"extractBarCodeQRCodeInfo: rawValue: "+ rawValue);
            itemName.setError(rawValue);
        }
    }
//    private boolean checkStoragePermission(){
//        boolean result = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
//        return result;
//    }
//    private void requestStoragePermission(){
//        ActivityCompat.requestPermissions(getActivity(), storagePermissions,STORAGE_REQUEST_CODE);
//    }
//
//    private boolean checkCameraPermission(){
//        boolean resultCamera = ContextCompat.checkSelfPermission(getActivity(),Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
//        boolean resultStorage = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
//
//        return resultCamera && resultStorage;
//
//    }
//
//    private void requestCameraPermission(){
//        ActivityCompat.requestPermissions(getActivity(),cameraPermissions,CAMERA_REQUEST_CODE);
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
//
//    }
}
