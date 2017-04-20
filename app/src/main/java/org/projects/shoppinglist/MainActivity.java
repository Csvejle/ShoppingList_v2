package org.projects.shoppinglist;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements DeleteDialogFragment.OnPositiveListener
{
    ArrayAdapter<Product> adapter;
    ListView listView;
    ArrayList<Product> bag = new ArrayList<Product>();

    static DeleteDialogFragment dialog;
    static Context context;

    static final int REQUEST_TAKE_PHOTO = 1;
    static final int REQUEST_SETTINGS = 2;
    String mCurrentPhotoPath;

    public ArrayAdapter getMyAdapter()
    {
        return adapter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;
        setContentView(R.layout.activity_main);

        if(MyPreferenceFragment.wantNotifications(this)){
           /* Toast toast = Toast.makeText(context, "Welcome " + MyPreferenceFragment.getName(this), Toast.LENGTH_LONG);
            toast.show();*/

            String message = "";

            String saveddate =  MyPreferenceFragment.getDate(this);

            Calendar calSaved = Calendar.getInstance();
            calSaved.set(DatePreference.getYear(saveddate), DatePreference.getMonth(saveddate)-1, DatePreference.getDate(saveddate));

            Calendar calDateNow = Calendar.getInstance();

            long diffMillis= Math.abs(calDateNow.getTimeInMillis()-calSaved.getTimeInMillis());

            long differenceInDays = TimeUnit.DAYS.convert(diffMillis, TimeUnit.MILLISECONDS);

            if(differenceInDays == 0){
                message = "Remember it's shopping day today.";
            }
            else if(calDateNow.compareTo(calSaved) > 0){
                message = "Sopping day for "+ differenceInDays + " day(s) ago.";
            }
            else {
                message = "Sopping in "+ differenceInDays + " day(s).";
            }

            Toast toast2 = Toast.makeText(context, message, Toast.LENGTH_LONG);
            toast2.show();

        }



        //Alternatively you can also get access to your saved data
        //in the onCreate method - you would need to do something
        //like this - here commented out:
        //I actually recommend doing the restore in the onCreate
        //method instead of in the onRestoreInstanceState method
		if (savedInstanceState!=null)
		{
            ArrayList<Product> saved = savedInstanceState.getParcelableArrayList("SavedBag");
			if (saved!=null) //did we save something
				bag = saved;

		}
        else{
            //add some stuff to the list so we have something
            // to show on app startup
            bag.add(new Product("Bananas", 2));
            bag.add(new Product("Apples", 10));
            bag.add(new Product("Milk", 1));
        }



        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //getting our listiew - you can check the ID in the xml to see that it
        //is indeed specified as "list"
        listView = (ListView) findViewById(R.id.list);

        //here we create a new adapter linking the bag and the
        //listview
        adapter =  new ArrayAdapter<Product>(this, android.R.layout.simple_list_item_checked, bag);

        //setting the adapter on the listview
        listView.setAdapter(adapter);


        //here we set the choice mode - meaning in this case we can
        //only select one item at a time.
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        Button addButton = (Button) findViewById(R.id.addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final View parent = findViewById(R.id.layout); //Finder parent layout

                String msg ="";
                Boolean ok = false;

                EditText produktInput = (EditText) findViewById(R.id.txtProduktName);
                String produktName = "";

                EditText amountInput = (EditText) findViewById(R.id.txtQuantity);
                String amount= "";

                Spinner amoutList = (Spinner) findViewById(R.id.amoutList);

                if(produktInput != null) {
                    produktName = produktInput.getText().toString();
                }
                else {
                    msg += "* Cannot find your produktName :(. *";
                }

                if(amountInput != null) {
                    amount = amountInput.getText().toString();
                }
                else {
                    msg += "* Cannot find your amount :(. *";
                }

                if(amount.isEmpty() || amount.length() == 0 || amount == null){

                    if(amoutList != null) {
                        amount = String.valueOf(amoutList.getSelectedItem()); //Finder den valgte quantity
                    }
                    else {
                        msg += "* Cannot find your amount :(. *";
                    }
                }

                if(!produktName.isEmpty() && isInt(amount) && produktName.length() > 0 && amount.length() > 0) {
                        bag.add(new Product(produktName, Integer.parseInt(amount)));
                        getMyAdapter().notifyDataSetChanged();
                        produktInput.getText().clear();
                        amountInput.getText().clear();
                        amoutList.setSelection(0);
                        ok = true;

                        //the following two lines hide the keyboard after clicking the button
                        //which is what you want!
                        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(parent.getWindowToken(), 0);
                }
                else { msg = "You have not typed both an amount and productname."; }

                if(!ok) {
                    Context context = getApplicationContext();
                    CharSequence text = msg;
                    int duration = Toast.LENGTH_LONG;

                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                }

            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                Intent intent = new Intent(this,SettingsActivity.class);
                startActivityForResult(intent,REQUEST_SETTINGS);
                break;
            case R.id.clearCartMenu:
                dialog = new DeleteDialogFragment();
                dialog.show(getFragmentManager(), "MyFragment");
                break;
            case R.id.aboutmenupkt:
                Intent intentAbout = new Intent(this,AboutActivity.class);
                startActivity(intentAbout);
                break;
            case R.id.takePhoto:
                PhotoIntent();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode== REQUEST_SETTINGS) //the code means we came back from settings
        {
            //I can can these methods like this, because they are static
           /*  boolean notification = MyPreferenceFragment.wantNotifications(this);
            String name = MyPreferenceFragment.getName(this);
            String message = "Welcome, "+name+", You want notifications? "+notification;*/
           Toast toast = Toast.makeText(this,"Your settings is saved. :)",Toast.LENGTH_LONG);
            toast.show();
        }
        else if(requestCode == REQUEST_TAKE_PHOTO &&
                resultCode == RESULT_OK) {

            if (mCurrentPhotoPath != null ) {
                //decoding the file into a bitmap
                Bitmap imageBitmap =
                        BitmapFactory.decodeFile (mCurrentPhotoPath);

                ImageDialog(imageBitmap);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    //This method is called before our activity is destoryed
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //ALWAYS CALL THE SUPER METHOD - To be nice!
        super.onSaveInstanceState(outState);

		/* Here we put code now to save the state */
        outState.putParcelableArrayList("SavedBag",bag);

    }


    public void onClickBought(View view){
        ListView selected = (ListView)findViewById(R.id.list);
        ArrayList<Product> slettet = new ArrayList();

        final ArrayList<Product> bagBackUp = new ArrayList<>(); //Laver array til backup
        bagBackUp.addAll(bag); //Tilføjer bags elementer til backup array

        final View parent = findViewById(R.id.layout); //Finder parent layout
        int count = selected.getCount();  //number of my ListView items

        SparseBooleanArray checkedItemPositions = selected.getCheckedItemPositions();

        for (int i = 0;i < count;i++){
            if(checkedItemPositions.get(i)) {
                slettet.add((Product)selected.getItemAtPosition(i));
            }
        }

        bag.removeAll(slettet);
        listView.clearChoices();
        getMyAdapter().notifyDataSetChanged();

        Snackbar snackbar = Snackbar
                .make(parent, "Changes saved", Snackbar.LENGTH_LONG)
                .setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //This code will ONLY be executed in case that
                        getMyAdapter().clear(); //Fjerner alt fra bag
                        bag.addAll(bagBackUp); //Tilføjer de elementer der var før sletning
                        getMyAdapter().notifyDataSetChanged(); //Giver besked på ændring


                        //Show the user we have restored the name - but here
                        //on this snackbar there is NO UNDO - so no SetAction method is called
                        //if you wanted, you could include a REDO on the second action button
                        //for instance.
                        Snackbar snackbar = Snackbar.make(parent, "Old bag restored!", Snackbar.LENGTH_SHORT);
                        snackbar.show();
                    }
                });

        snackbar.show();

        /**Context context = getApplicationContext();
         CharSequence text =  "You bought: " + (count-selected.getCount()) +" items." + Arrays.toString(slettet.toArray());
         int duration = Toast.LENGTH_LONG;

         Toast toast = Toast.makeText(context, text, duration);
         toast.show(); */
    }
    public void onClickClearCart(View view){
        dialog = new DeleteDialogFragment();

        //Here we show the dialog
        //The tag "MyFragement" is not important for us.
        dialog.show(getFragmentManager(), "MyFragment");

    }
    @Override
    public void onPositiveClicked() {
        getMyAdapter().clear();
        listView.clearChoices();
        getMyAdapter().notifyDataSetChanged();

        Context context = getApplicationContext();
        CharSequence text =  "Shopping cart cleared";
        int duration = Toast.LENGTH_LONG;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }


    boolean isInt(String s)
    {
        Boolean res = false;

        try { Integer.parseInt(s);
              res = true;
            }
        catch(NumberFormatException er)
        { res = false; }

        return  res;
    }

    private void dispatchTakePictureIntent() {
        //Laver intent til, at tage et billede
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        //Sikre der er en app der kan tage billeder
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {


            File photoFile = null;
            try {
                photoFile = createImageFile(); //Laver den fil, som billedet skal gemmes i.
            }
            catch (IOException ex)
            {
                // Error
                Toast toast = Toast.makeText(this,"Error during fil creation. - Sorry :(",Toast.LENGTH_LONG);
                toast.show();
            }

            //Tjekker om filen er oprettet ok
            if (photoFile != null) {

                //Finder Uri'en på, det billeder der tidligere er oprettet
                Uri photoURI = FileProvider.getUriForFile(this,
                        "org.projects.shoppinglist.fileprovider",
                        photoFile);

                //Tager data med intenten, som fortæller Uri'en til, den billede fil der er oprrettet tidligere.
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                //Starter kamera aktivitet
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private File createImageFile() throws IOException {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()); // Laver data, som skal være del af billedes filnavn.
        String imageFileName = "JPEG_" + timeStamp + "_"; //Laver et filnavn

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES); //Finder mappen, som appen må gemme ting i.
                                                                            // -> andre apps har ikke adgang til denne mappe.

        //Laver billede baseret på oventående beregninger.
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void PhotoIntent(){
        //Tjekker om, Android version er ok, da logikken bag at tage et billede afhænger af det.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            dispatchTakePictureIntent(); //
        }
        else
        {
            //Fortæller brugeren, at det ikke er ok Android version
            Toast toast = Toast.makeText(this,"Your Android version is too low.",Toast.LENGTH_LONG);
            toast.show();
        }
    }

    private void ImageDialog(Bitmap imageBitmap){

        //Opsætning af dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton("Fine for me", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Gør intet, da dialogen bare skal fjernes.
            }
        }).setNegativeButton("Take new", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                PhotoIntent(); //Går brugeren kan tage et nyt billede
            }
        });

        AlertDialog dialog = builder.create(); //Bygger en dialog baseret på ovenstående settings.
        dialog.setTitle("Your photo"); //Sætter dialogens titel

        //disse to linjer laver et view, baseret på activity_image_result xml filen.
        LayoutInflater inflater = getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.activity_image_result, null); //

        ImageView imageView  = (ImageView) dialogLayout.findViewById(R.id.imageView); //Finder ImageView i ovenstående layout
        imageView.setImageBitmap(imageBitmap); //Sætter billedet til, det der er givet fra kamera,
        //hvilket er det metoden tager som parameter.

        dialog.setView(dialogLayout); //Giver dialog det view, som er lavet får.
        dialog.show(); //Viser dialog
    }
}
