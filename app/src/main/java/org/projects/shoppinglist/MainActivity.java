package org.projects.shoppinglist;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseListAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity implements DeleteDialogFragment.OnPositiveListener
{

    DatabaseReference ref; //Database reference
    FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance(); //Remote config instans
    FirebaseListAdapter<Product> adapter; //Firebaseadapter

    private FirebaseAuth mAuth;
    FirebaseUser user;

    ListView listView; //

    //ArrayAdapter<Product> adapter; //Tidligere adapter, da det var en Arrayliste, der var database
    //ArrayList<Product> bag = new ArrayList<Product>(); //Tidligere database

    static DeleteDialogFragment dialog; //Slette dialog
    static Context context;

    //Identifier til intents
    static final int REQUEST_SIGNIN = 10;
    static final int REQUEST_TAKE_PHOTO = 1;
    static final int REQUEST_SETTINGS = 2;

    String mCurrentPhotoPath; //Billedesti

    /* key til BroadCastReceiver */
    public static final String BROADCAST_KEY = "broadcastdata";
    LocalBroadcastManager broadcastManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Sætter layout
        setContentView(R.layout.activity_main);

        //Sætter actionbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Sætter context, hvilket er denne aktivitet
        this.context = this;


        //RemoteConfig
        //RemoteConfig defaults
            Map<String,Object> defaults = new HashMap<>();
            defaults.put("app_name", getResources().getString(R.string.app_name));
            firebaseRemoteConfig.setDefaults(defaults);


            FirebaseRemoteConfigSettings configSettings =
                        new FirebaseRemoteConfigSettings.Builder()
                                .setDeveloperModeEnabled(true) //set to false when releasing
                        .build();
            firebaseRemoteConfig.setConfigSettings(configSettings);

            //Opdatering baseret på RemoteConfig
            RemoteConfigTask();

        //firebase Crash rapport
            //For brug af firebase Crash rapport
            //Udkommenter nedenstående, da ikke ønsker det skal medtages hver gang,
            //at aktiviteten bruges.
                //FirebaseCrash.log("Creating the APP");
                //FirebaseCrash.report(new Exception("Logging"));


        //Login
            //Sætter variabler ift. login
                this.mAuth = FirebaseAuth.getInstance();
                this.user = mAuth.getCurrentUser();


            // Tjekker om brugeren er logget ind.
                isSignedIn();

                if(user != null) {
                    //Laver reference til firbase database
                    ref = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid()).child("products");

                }
                else {
                    ref = FirebaseDatabase.getInstance().getReference().child("None").child("products");
                }


        //Data fra firebase
            //Finder listview fra layout
            listView = (ListView) findViewById(R.id.list);


            //Laver FirebaseListAdapter adapter
            adapter = new FirebaseListAdapter<Product>(this, Product.class, android.R.layout.simple_list_item_checked, ref) {
                @Override
                protected void populateView(View v, Product model, int position) {
                    ((TextView)v.findViewById(android.R.id.text1)).setText(model.toString());
                }
            };

            //sætter adapter på listviewet
            listView.setAdapter(adapter);


            //Fortæller at bruger skal have mulighed for, at vælge flere items i listen.
            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);


        //Add knap og listener til
            //Finder add knap fra layout
            Button addButton = (Button) findViewById(R.id.addButton);

            //Lavet klik event til knap
            addButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addBtnKode();
                }
            });


    //Notifikationer
        if (savedInstanceState!=null) {
            Boolean saved = savedInstanceState.getBoolean("appDown");
            //USE saved bool???
        }  else
           {
                //Hvis brugeren ønsker notificationer fortælles der,
                //hvor mange dage til eller siden, der er shopping dag.
                //Bemærk dette kun sker, hvis ikke der er gemt stadie = appen er genoprettet.
                if(MyPreferenceFragment.wantNotifications(this)){
                    appNotifications();
                }
           }

        broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(broadCastCode, new IntentFilter(BROADCAST_KEY));

        //er der gemt noget, som skal bruges til, at genskabe tidligere stadie
		/*if (savedInstanceState!=null)
		{
            //ArrayList<Product> saved = savedInstanceState.getParcelableArrayList("SavedBag");

			if (saved!=null)
            {
                bag = saved; //Ikke nødvendigt, da data fra DB.
            }
		}
        else{

            //Tidligere tilføjelse af data, når der ikke er noget gemt i bag arraylisten
            bag.add(new Product("Bananas", 2));
            bag.add(new Product("Apples", 10));
            bag.add(new Product("Milk", 1));
        }*/



        //Adapter før, hvor der er brugt en arrayliste (bag) til database
        //adapter =  new ArrayAdapter<Product>(this, android.R.layout.simple_list_item_checked, bag);
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Håndter når app er ødelagt
        adapter.cleanup();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Tilføjer menupunkter til menuen, hvilket er de punkter der er i menu_main filen.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Håndter når der klikkes på et menupunkt

        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                Intent intent = new Intent(this,SettingsActivity.class);
                startActivityForResult(intent,REQUEST_SETTINGS);
                break;
            case R.id.profile:
                Intent intentLogin = new Intent(this,LoginActivity.class);
                startActivity(intentLogin);
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
            case R.id.signout:
                mAuth.signOut();
                user = mAuth.getCurrentUser();
                isSignedIn();
                break;
            case R.id.shareShoppingList:
                Product p;
                String itemsTekst = "";

                for(int i = 0; i<getMyAdapter().getCount(); i++) {
                    p = getItem(i);
                    if(p != null) {
                        itemsTekst += " - " + p.toString() + "\n";
                    }
                }

                if(itemsTekst.equals("")) { itemsTekst = "No items yet.";}

                //Deling af shoppinglist
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Shoppinglist");
                sendIntent.putExtra(Intent.EXTRA_TEXT, "My shoppinglist \n" + itemsTekst);
                sendIntent.setType("text/plain"); //fortæller, at det der sendes skal være plan tekst
                startActivity(sendIntent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode== REQUEST_SETTINGS)
        {
           Toast toast = Toast.makeText(this,"Your settings is saved. :)",Toast.LENGTH_LONG);
           toast.show();
        }
        else if(requestCode == REQUEST_TAKE_PHOTO &&
                resultCode == RESULT_OK) {

            if (mCurrentPhotoPath != null ) {
                //decoder fil til et bitmap
                Bitmap imageBitmap =
                        BitmapFactory.decodeFile (mCurrentPhotoPath);

                ImageDialog(imageBitmap);
            }
        }
        else if(requestCode == REQUEST_SIGNIN &&  resultCode == RESULT_OK) {
            isSignedIn();
            Toast toast = Toast.makeText(context, user.getEmail(), Toast.LENGTH_LONG);
            toast.show();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    //Kaldes før aktiviteten er destoryed
    @Override
    protected void onSaveInstanceState(Bundle outState) {
         super.onSaveInstanceState(outState);

		/* Kode til, at gemme nuværende stadie */
        //outState.putParcelableArrayList("SavedBag",bag); //Ikke nødvendigt nu, da data fra DB.

        outState.putBoolean("appDown", true);

    }

    /* GET an item  */
    public Product getItem(int index)
    {
        return (Product) getMyAdapter().getItem(index);
    }

    public void onClickBought(View view){
        ListView selected = (ListView)findViewById(R.id.list); //Laver reference til, ListViewet fra XML filen.
        final Map<String, Product> bagBackUp = new HashMap<>(); //Laver array til backup

        final View parent = findViewById(R.id.layout); //Finder parent layout
        int count = selected.getCount();  //Antallet af items i ListViewet

        //Der findes og gemmes alle de produktpositioner der er valgt af brugeren.
        SparseBooleanArray checkedItemPositions = selected.getCheckedItemPositions();

        for (int i = 0;i < count;i++){

            //Tjekker om der findes et item på positionen der er nået til.
            if(checkedItemPositions.get(i)) {

                //Sletter det produkt der er nået til, af de produkter brugere har markeret til slettning.
                getMyAdapter().getRef(i).setValue(null);

                //Tilføjer slettet til backup Map
                bagBackUp.put(adapter.getRef(i).getKey(), (Product)selected.getItemAtPosition(i));

                //Tilføjelse til tidligere backup arrayliste
                //slettet.add((Product)selected.getItemAtPosition(i));
            }
        }

        listView.clearChoices(); //Sørger for, at valgte glemmes
        getMyAdapter().notifyDataSetChanged(); //Giver besked på ændringer i data

        Snackbar snackbar = Snackbar
                .make(parent, "Changes saved", Snackbar.LENGTH_LONG)
                .setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        //Genskaber data
                        for(String key:bagBackUp.keySet()){
                            ref.child(key).setValue(bagBackUp.get(key));
                        }

                        getMyAdapter().notifyDataSetChanged(); //Giver besked på ændring

                        //Fortæller brugeren, at data er restored
                        Snackbar snackbar = Snackbar.make(parent, "Old bag restored!", Snackbar.LENGTH_SHORT);
                        snackbar.show();


                        //Kode der var brugt før til, at genskabe data
                            //getMyAdapter().clear(); //Fjerner alt fra bag
                            //bag.addAll(bagBackUp); //Tilføjer de elementer der var før sletning
                    }
                });

        snackbar.show(); //Gør snackbar synlig

        /**   Tidligere arbejde   */
        /**Context context = getApplicationContext();
         CharSequence text =  "You bought: " + (count-selected.getCount()) +" items." + Arrays.toString(slettet.toArray());
         int duration = Toast.LENGTH_LONG;

         Toast toast = Toast.makeText(context, text, duration);
         toast.show(); */

        /*  ListView selected = (ListView)findViewById(R.id.list);
        ArrayList<Product> slettet = new ArrayList();

        final ArrayList<Product> bagBackUp = new ArrayList<>(); //Laver array til backup
       // bagBackUp.addAll(bag); //Tilføjer bags elementer til backup array

        final View parent = findViewById(R.id.layout); //Finder parent layout
        int count = selected.getCount();  //number of my ListView items

        SparseBooleanArray checkedItemPositions = selected.getCheckedItemPositions();

        for (int i = 0;i < count;i++){
            if(checkedItemPositions.get(i)) {
                slettet.add((Product)selected.getItemAtPosition(i));
            }
        }

       // bag.removeAll(slettet);
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

        snackbar.show();*/
    }
    public void onClickClearCart(View view){
        dialog = new DeleteDialogFragment(); //Laver ny dialog objekt

        //Viser slette dialogen
        dialog.show(getFragmentManager(), "MyFragment");
    }
    @Override
    public void onPositiveClicked() {

        ref.removeValue(); //Fjerner alle items fra database
        listView.clearChoices();
        getMyAdapter().notifyDataSetChanged(); //Giver besked om, at data har ændret sig.


        //Laver toast, så brugeren ved, at alle items er slettet.
        Context context = getApplicationContext();
        CharSequence text =  "Shopping cart cleared";
        int duration = Toast.LENGTH_LONG;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();



        //systemNotification("No products", "What do you want to buy?");

        //make a new intent to send to the activity
        Intent in = new Intent(BROADCAST_KEY);
        LocalBroadcastManager.getInstance(this).sendBroadcast(in);


        // getMyAdapter().clear(); //Fjener alle items førhen
    }


    /** Get adapter */
    public FirebaseListAdapter getMyAdapter()
    {
        return adapter;
    }

    /** Beregner om, den angive string kan laves om til et hel tal. */
    boolean isInt(String s)
    {
        Boolean res = false;

        try {
              Integer.parseInt(s);
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
                //Laver den fil, som billedet skal gemmes i.
                photoFile = createImageFile();
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

        //Sti som skal bruges til, at gemme det billede der tages med kamera
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

                Toast toast = Toast.makeText(context, "Se billedet her: " + mCurrentPhotoPath, Toast.LENGTH_LONG);
                toast.show();

            }
        }).setNegativeButton("Take new", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                PhotoIntent(); //Gør brugeren kan tage et nyt billede
            }
        });

        AlertDialog dialog = builder.create(); //Bygger en dialog baseret på ovenstående settings.
        dialog.setTitle("Your photo"); //Sætter dialogens titel

        //disse to linjer laver et view, baseret på activity_image_result.xml filen.
        LayoutInflater inflater = getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.activity_image_result, null); //

        ImageView imageView  = (ImageView) dialogLayout.findViewById(R.id.imageView); //Finder ImageView i ovenstående layout
        imageView.setImageBitmap(imageBitmap); //Sætter billedet til, det der er givet fra kamera,
                                               //hvilket er det metoden tager som parameter.

        dialog.setView(dialogLayout); //Giver dialog det view, som er lavet før.
        dialog.show(); //Viser dialog
    }


    /***
     * Finder ud af, om brugeren er logget ind, hvis ikke gås til den aktivitet der håndter login.
    **/
    private void isSignedIn() {
        if (user != null) {
            //Skriver i loggen brugeren er logget ind.
            Log.d("User", "success");
        } else {
            //Starter Login aktivitet
            Intent intentLogin = new Intent(context,LoginActivity.class);
            startActivity(intentLogin);
        }
    }

    private void RemoteConfigTask()
    {
        Task<Void> myTask = firebaseRemoteConfig.fetch(1);
        myTask.addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful())
                {
                    firebaseRemoteConfig.activateFetched();
                    String name = firebaseRemoteConfig.getString("app_name");
                    getSupportActionBar().setTitle(name);
                    Log.d("Fkett: ", "New: " + name);
                } else
                    Log.d("ERROR", "Task not succesfull" + task.getException());
            }
        });
    }

    private void addBtnKode(){
        final View parent = findViewById(R.id.layout); //Finder parent layout
        String msg = ""; //Besked til bruger
        Boolean ok = false; //Om oprettelsen er ok


        // -- Validering af input -- //
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
        // ------- //


        if(!produktName.isEmpty() && isInt(amount) && produktName.length() > 0 && amount.length() > 0) {
            //Tilføjer nyt produkt
            ref.push().setValue(new Product(produktName, Integer.parseInt(amount)));

            //Fortæller der er sket ændringer i data
            getMyAdapter().notifyDataSetChanged();

            //Gør klar til, at brugeren kan tilføje et nyt produkt
            produktInput.getText().clear();
            amountInput.getText().clear();
            amoutList.setSelection(0);

            //Fortæller at produktet er oprettet
            ok = true;

            //Fjerner keyboard efter klik på knap
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(parent.getWindowToken(), 0);


            //Tilføjelse af nyt produkt før
            //bag.add(new Product(produktName, Integer.parseInt(amount)));
        }
        else { msg = "You have not typed both an amount and productname."; }

        if(!ok) {

            //Giver besked på, at der ikke er fuldt det nødvendige data til, at tilføje et nyt produkt
            Context context = getApplicationContext();
            CharSequence text = msg;
            int duration = Toast.LENGTH_LONG;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }
    }

    public void systemNotification(String title,String message)
    {
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(new Intent(this, MainActivity.class));
        PendingIntent pIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        //build the notification using the builder.
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_shopping)  //icon
                .setContentTitle(title)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setContentIntent(pIntent)
                .setContentText(message)
                .build();

        //use the built in notification service
        NotificationManager manager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        //the 42 is just a ID number you choose.
        manager.notify(40,notification);

    }

    private void appNotifications() {
        String message = "";

        //Finder gemt dato
        String saveddate =  MyPreferenceFragment.getDate(this);

        //Laver Kalender med ovenstående dato
        Calendar calSaved = Calendar.getInstance();
        calSaved.set(DatePreference.getYear(saveddate), DatePreference.getMonth(saveddate)-1, DatePreference.getDate(saveddate));

        //Før nuværende dato
        Calendar calDateNow = Calendar.getInstance();

        //Beregner forskel i millisekunder
        long diffMillis= Math.abs(calDateNow.getTimeInMillis()-calSaved.getTimeInMillis());

        //Laver ovenstående om til dage
        long differenceInDays = TimeUnit.DAYS.convert(diffMillis, TimeUnit.MILLISECONDS);

        //Laver passende besked
        if(differenceInDays == 0){
            message = "Remember it's shopping day today.";
        }
        else if(calDateNow.compareTo(calSaved) > 0){
            message = "Sopping day for "+ differenceInDays + " day(s) ago.";
        }
        else {
            message = "Sopping in "+ differenceInDays + " day(s).";
        }

        //Giver brugeren den lavede besked
        Toast toast2 = Toast.makeText(context, message + " " + MyPreferenceFragment.getName(context), Toast.LENGTH_LONG);
        toast2.show();
    }

    public BroadcastReceiver broadCastCode = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null ) {
                systemNotification("No products", "What do you want to buy?");
                Log.d("ReceiverKode", "success");
            }
        }
    };
}
